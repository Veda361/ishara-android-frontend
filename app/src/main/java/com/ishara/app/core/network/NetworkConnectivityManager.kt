package com.ishara.app.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.ishara.app.core.common.IshaaraLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Distinguishes true internet availability from basic interface link state.
 *
 * States:
 * - [NoNetwork]: No network interface connected.
 * - [NetworkAvailable]: Interface connected (Wi-Fi or Cellular), but internet not yet validated.
 * - [InternetValidated]: Full internet access validated by OS.
 * - [NetworkLost]: Network interface dropped.
 */
sealed interface NetworkStatus {
    object NoNetwork : NetworkStatus
    object NetworkAvailable : NetworkStatus
    object InternetValidated : NetworkStatus
    object NetworkLost : NetworkStatus
}

interface NetworkMonitor {
    val networkStatus: StateFlow<NetworkStatus>
    val isInternetValidated: Boolean
}

class NetworkConnectivityManager(
    context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : NetworkMonitor {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val _networkStatus = MutableStateFlow<NetworkStatus>(NetworkStatus.NoNetwork)
    override val networkStatus: StateFlow<NetworkStatus> = _networkStatus.asStateFlow()

    override val isInternetValidated: Boolean
        get() = _networkStatus.value is NetworkStatus.InternetValidated

    private val isRegistered = AtomicBoolean(false)

    init {
        checkInitialState()
        registerNetworkCallback()
    }

    private fun checkInitialState() {
        val cm = connectivityManager ?: return
        val activeNetwork = cm.activeNetwork
        if (activeNetwork == null) {
            _networkStatus.value = NetworkStatus.NoNetwork
            IshaaraLogger.d(TAG, "state=NO_NETWORK")
            return
        }

        val caps = cm.getNetworkCapabilities(activeNetwork)
        if (caps == null) {
            _networkStatus.value = NetworkStatus.NoNetwork
            IshaaraLogger.d(TAG, "state=NO_NETWORK")
            return
        }

        val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val isValidated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

        val status = when {
            isValidated -> NetworkStatus.InternetValidated
            hasInternet -> NetworkStatus.NetworkAvailable
            else -> NetworkStatus.NoNetwork
        }
        _networkStatus.value = status
        IshaaraLogger.d(TAG, "state=$status transport=${getTransportName(caps)}")
    }

    private fun registerNetworkCallback() {
        val cm = connectivityManager ?: return
        if (isRegistered.getAndSet(true)) return

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        try {
            cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    val caps = cm.getNetworkCapabilities(network)
                    val isValidated = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
                    val status = if (isValidated) NetworkStatus.InternetValidated else NetworkStatus.NetworkAvailable
                    _networkStatus.value = status
                    IshaaraLogger.d(TAG, "event=ON_AVAILABLE state=$status transport=${getTransportName(caps)}")
                }

                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    val isValidated = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    val status = if (isValidated) NetworkStatus.InternetValidated else NetworkStatus.NetworkAvailable
                    _networkStatus.value = status
                    IshaaraLogger.d(TAG, "event=CAPABILITIES_CHANGED state=$status validated=$isValidated transport=${getTransportName(networkCapabilities)}")
                }

                override fun onLost(network: Network) {
                    _networkStatus.value = NetworkStatus.NetworkLost
                    IshaaraLogger.d(TAG, "event=NETWORK_LOST state=NETWORK_LOST")
                }

                override fun onUnavailable() {
                    _networkStatus.value = NetworkStatus.NoNetwork
                    IshaaraLogger.d(TAG, "event=NETWORK_UNAVAILABLE state=NO_NETWORK")
                }
            })
        } catch (e: Exception) {
            IshaaraLogger.w(TAG, "Failed to register network callback: ${e.message}")
        }
    }

    private fun getTransportName(caps: NetworkCapabilities?): String {
        if (caps == null) return "UNKNOWN"
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELLULAR"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> "BLUETOOTH"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
            else -> "OTHER"
        }
    }

    companion object {
        private const val TAG = "ISHAARA_NETWORK"
    }
}
