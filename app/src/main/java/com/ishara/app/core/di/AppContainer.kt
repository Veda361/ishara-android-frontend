package com.ishara.app.core.di

import android.content.Context
import com.ishara.app.core.auth.DefaultGoogleAuthClient
import com.ishara.app.core.auth.GoogleAuthClient
import com.ishara.app.core.common.DefaultDispatcherProvider
import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.core.location.LocationCoordinates
import com.ishara.app.core.location.LocationProvider
import com.ishara.app.core.network.DefaultIshaaraHttpClient
import com.ishara.app.core.network.IshaaraHttpClient
import com.ishara.app.core.network.NetworkConfig
import com.ishara.app.core.network.SessionInvalidationCoordinator
import com.ishara.app.core.realtime.RealtimeClient
import com.ishara.app.core.realtime.RealtimeConnectionState
import com.ishara.app.core.realtime.RealtimeEvent
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.core.storage.EncryptedSessionStore
import com.ishara.app.core.storage.SessionStore
import com.ishara.app.data.local.datasource.SessionLocalDataSource
import com.ishara.app.data.local.datasource.SessionLocalDataSourceImpl
import com.ishara.app.data.remote.datasource.AuthRemoteDataSource
import com.ishara.app.data.remote.datasource.AuthRemoteDataSourceImpl
import com.ishara.app.data.remote.datasource.DriverRemoteDataSource
import com.ishara.app.data.remote.datasource.DriverRemoteDataSourceImpl
import com.ishara.app.data.remote.datasource.TripRemoteDataSource
import com.ishara.app.data.remote.datasource.TripRemoteDataSourceImpl
import com.ishara.app.data.remote.datasource.UserRemoteDataSource
import com.ishara.app.data.remote.datasource.UserRemoteDataSourceImpl
import com.ishara.app.data.repository.AuthRepositoryImpl
import com.ishara.app.data.repository.DriverRepositoryImpl
import com.ishara.app.data.repository.OnboardingRepositoryImpl
import com.ishara.app.data.repository.TripRepositoryImpl
import com.ishara.app.data.repository.UserRepositoryImpl
import com.ishara.app.domain.repository.AuthRepository
import com.ishara.app.domain.repository.DriverRepository
import com.ishara.app.domain.repository.OnboardingRepository
import com.ishara.app.domain.repository.TripRepository
import com.ishara.app.domain.repository.UserRepository
import com.ishara.app.domain.usecase.DiscoverTripsUseCase
import com.ishara.app.domain.usecase.GetAuthSessionUseCase
import com.ishara.app.domain.usecase.GetCurrentUserProfileUseCase
import com.ishara.app.domain.usecase.GetDriverOperationalContextUseCase
import com.ishara.app.domain.usecase.GetOnboardingStateUseCase
import com.ishara.app.domain.usecase.ManageDriverTripLifecycleUseCase
import com.ishara.app.domain.usecase.ObserveAuthStateUseCase
import com.ishara.app.domain.usecase.ResolveApplicationDestinationUseCase
import com.ishara.app.domain.usecase.RestoreSessionUseCase
import com.ishara.app.domain.usecase.SetDriverAvailabilityUseCase
import com.ishara.app.domain.usecase.SignInWithGoogleUseCase
import com.ishara.app.domain.usecase.SignOutUseCase
import com.ishara.app.domain.usecase.SubmitOnboardingUseCase
import com.ishara.app.domain.usecase.UpdateDriverLocationUseCase
import com.ishara.app.navigation.NavigationManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Pure Dependency Injection Container for the Ishaara Android application.
 * Follows official Android recommended AppContainer pattern.
 */
interface AppContainer {
    val dispatchers: DispatcherProvider
    val networkConfig: NetworkConfig
    val sessionCoordinator: SessionInvalidationCoordinator
    val httpClient: IshaaraHttpClient
    val sessionStore: SessionStore
    val navigationManager: NavigationManager
    val locationProvider: LocationProvider
    val networkMonitor: com.ishara.app.core.network.NetworkMonitor
    val realtimeClient: RealtimeClient
    val googleAuthClient: GoogleAuthClient

    // Repositories
    val authRepository: AuthRepository
    val userRepository: UserRepository
    val onboardingRepository: OnboardingRepository
    val tripRepository: TripRepository
    val driverRepository: DriverRepository
    val locationRepository: com.ishara.app.domain.repository.LocationRepository
    val rideRepository: com.ishara.app.domain.repository.RideRepository

    // Auth Use Cases
    val getAuthSessionUseCase: GetAuthSessionUseCase
    val restoreSessionUseCase: RestoreSessionUseCase
    val observeAuthStateUseCase: ObserveAuthStateUseCase
    val signInWithGoogleUseCase: SignInWithGoogleUseCase
    val sendEmailOtpUseCase: com.ishara.app.domain.usecase.SendEmailOtpUseCase
    val signInWithEmailOtpUseCase: com.ishara.app.domain.usecase.SignInWithEmailOtpUseCase
    val signOutUseCase: SignOutUseCase

    // User & Onboarding Use Cases
    val getCurrentUserProfileUseCase: GetCurrentUserProfileUseCase
    val updateUserProfileUseCase: com.ishara.app.domain.usecase.UpdateUserProfileUseCase
    val getOnboardingStateUseCase: GetOnboardingStateUseCase
    val submitOnboardingUseCase: SubmitOnboardingUseCase
    val resolveApplicationDestinationUseCase: ResolveApplicationDestinationUseCase

    // Driver Operations Use Cases
    val getDriverOperationalContextUseCase: GetDriverOperationalContextUseCase
    val setDriverAvailabilityUseCase: SetDriverAvailabilityUseCase
    val manageDriverTripLifecycleUseCase: ManageDriverTripLifecycleUseCase

    // Phase A04: Driver Onboarding & Verification Use Cases
    val getDriverProfileUseCase: com.ishara.app.domain.usecase.GetDriverProfileUseCase
    val createDriverProfileUseCase: com.ishara.app.domain.usecase.CreateDriverProfileUseCase
    val updateDriverProfileUseCase: com.ishara.app.domain.usecase.UpdateDriverProfileUseCase
    val getDriverVerificationStatusUseCase: com.ishara.app.domain.usecase.GetDriverVerificationStatusUseCase
    val submitDriverVerificationUseCase: com.ishara.app.domain.usecase.SubmitDriverVerificationUseCase
    val observeDriverProfileUseCase: com.ishara.app.domain.usecase.ObserveDriverProfileUseCase
    val observeDriverOnboardingStateUseCase: com.ishara.app.domain.usecase.ObserveDriverOnboardingStateUseCase
    val observeDriverProfileStateUseCase: com.ishara.app.domain.usecase.ObserveDriverProfileStateUseCase
    val refreshDriverProfileUseCase: com.ishara.app.domain.usecase.RefreshDriverProfileUseCase
    val clearDriverStateUseCase: com.ishara.app.domain.usecase.ClearDriverStateUseCase

    // Phase A05: Agency Membership & Fleet Association Use Cases
    val agencyRemoteDataSource: com.ishara.app.data.remote.datasource.AgencyRemoteDataSource
    val agencyMembershipRepository: com.ishara.app.domain.repository.AgencyMembershipRepository
    val getCurrentAgencyMembershipUseCase: com.ishara.app.domain.usecase.GetCurrentAgencyMembershipUseCase
    val observeAgencyMembershipStateUseCase: com.ishara.app.domain.usecase.ObserveAgencyMembershipStateUseCase
    val observeCurrentAgencyMembershipUseCase: com.ishara.app.domain.usecase.ObserveCurrentAgencyMembershipUseCase
    val listAgenciesUseCase: com.ishara.app.domain.usecase.ListAgenciesUseCase
    val getAgencyDetailsUseCase: com.ishara.app.domain.usecase.GetAgencyDetailsUseCase
    val requestAgencyMembershipUseCase: com.ishara.app.domain.usecase.RequestAgencyMembershipUseCase
    val cancelAgencyMembershipUseCase: com.ishara.app.domain.usecase.CancelAgencyMembershipUseCase
    val refreshAgencyMembershipUseCase: com.ishara.app.domain.usecase.RefreshAgencyMembershipUseCase
    val clearAgencyMembershipStateUseCase: com.ishara.app.domain.usecase.ClearAgencyMembershipStateUseCase

    // Phase A06: Driver Operational Readiness
    val driverReadinessRemoteDataSource: com.ishara.app.data.remote.datasource.DriverReadinessRemoteDataSource
    val driverReadinessRepository: com.ishara.app.domain.repository.DriverReadinessRepository
    val getDriverReadinessUseCase: com.ishara.app.domain.usecase.GetDriverReadinessUseCase
    val observeDriverReadinessUseCase: com.ishara.app.domain.usecase.ObserveDriverReadinessUseCase
    val refreshDriverReadinessUseCase: com.ishara.app.domain.usecase.RefreshDriverReadinessUseCase
    val clearDriverReadinessStateUseCase: com.ishara.app.domain.usecase.ClearDriverReadinessStateUseCase

    // Transit & Location Use Cases
    val discoverTripsUseCase: DiscoverTripsUseCase
    val getPassengerTripDetailsUseCase: com.ishara.app.domain.usecase.GetPassengerTripDetailsUseCase
    val updateDriverLocationUseCase: UpdateDriverLocationUseCase
    val searchLocationsUseCase: com.ishara.app.domain.usecase.SearchLocationsUseCase
    val resolveCurrentLocationUseCase: com.ishara.app.domain.usecase.ResolveCurrentLocationUseCase
    val manageRecentDestinationsUseCase: com.ishara.app.domain.usecase.ManageRecentDestinationsUseCase
    val getActiveRideUseCase: com.ishara.app.domain.usecase.GetActiveRideUseCase
    val createRideRequestUseCase: com.ishara.app.domain.usecase.CreateRideRequestUseCase
    val getRideRequestUseCase: com.ishara.app.domain.usecase.GetRideRequestUseCase
    val cancelRideRequestUseCase: com.ishara.app.domain.usecase.CancelRideRequestUseCase
    val getUserRideRequestsUseCase: com.ishara.app.domain.usecase.GetUserRideRequestsUseCase
    val clearRideRequestStateUseCase: com.ishara.app.domain.usecase.ClearRideRequestStateUseCase
    val driverTrackingCoordinator: com.ishara.app.feature.driver.tracking.DriverTrackingCoordinator
    val startDriverTrackingUseCase: com.ishara.app.domain.usecase.StartDriverTrackingUseCase
    val stopDriverTrackingUseCase: com.ishara.app.domain.usecase.StopDriverTrackingUseCase
    val observeDriverTrackingStatusUseCase: com.ishara.app.domain.usecase.ObserveDriverTrackingStatusUseCase

    // Phase 10: Driver Ride Requests & Passenger Boarding
    val driverRideRequestRepository: com.ishara.app.domain.repository.DriverRideRequestRepository
    val getDriverRideRequestsUseCase: com.ishara.app.domain.usecase.GetDriverRideRequestsUseCase
    val acceptRideRequestUseCase: com.ishara.app.domain.usecase.AcceptRideRequestUseCase
    val rejectRideRequestUseCase: com.ishara.app.domain.usecase.RejectRideRequestUseCase
    val managePassengerBoardingUseCase: com.ishara.app.domain.usecase.ManagePassengerBoardingUseCase

    // Phase 11: Live Passenger Map / Ride Tracking
    val rideTrackingRepository: com.ishara.app.domain.repository.RideTrackingRepository
    val getRideTrackingUseCase: com.ishara.app.domain.usecase.GetRideTrackingUseCase
    val getDriverLocationUseCase: com.ishara.app.domain.usecase.GetDriverLocationUseCase
    val observeRideTrackingUseCase: com.ishara.app.domain.usecase.ObserveRideTrackingUseCase

    // Phase A12: Fare & Authoritative Pricing Foundation
    val fareRepository: com.ishara.app.domain.repository.FareRepository
    val getRideFareUseCase: com.ishara.app.domain.usecase.GetRideFareUseCase
    val clearFareStateUseCase: com.ishara.app.domain.usecase.ClearFareStateUseCase

    // Phase 12: Hands-Free Voice Trip Creation
    val voiceInputClient: com.ishara.app.core.voice.VoiceInputClient
    val voiceTripRepository: com.ishara.app.domain.repository.VoiceTripRepository
    val createVoiceTripDraftUseCase: com.ishara.app.domain.usecase.CreateVoiceTripDraftUseCase
    val cancelVoiceTripDraftUseCase: com.ishara.app.domain.usecase.CancelVoiceTripDraftUseCase

    // Phase 13: Digital Ticketing, Fare & Payment
    val paymentLauncher: com.ishara.app.core.payment.PaymentLauncher
    val paymentRepository: com.ishara.app.domain.repository.PaymentRepository
    val createPaymentOrderUseCase: com.ishara.app.domain.usecase.CreatePaymentOrderUseCase
    val getRidePaymentUseCase: com.ishara.app.domain.usecase.GetRidePaymentUseCase
    val verifyPaymentUseCase: com.ishara.app.domain.usecase.VerifyPaymentUseCase
    val clearPaymentStateUseCase: com.ishara.app.domain.usecase.ClearPaymentStateUseCase

    // Phase 14: Route Schedules, Bus Stops & Offline Transit Cache
    val transitRepository: com.ishara.app.domain.repository.TransitRepository
    val getTransitRoutesUseCase: com.ishara.app.domain.usecase.GetTransitRoutesUseCase
    val getRouteDetailsUseCase: com.ishara.app.domain.usecase.GetRouteDetailsUseCase
    val refreshTransitRoutesUseCase: com.ishara.app.domain.usecase.RefreshTransitRoutesUseCase

    // Phase 15: Safety, SOS & Emergency Response
    val safetyRemoteDataSource: com.ishara.app.data.remote.datasource.SafetyRemoteDataSource
    val safetyRepository: com.ishara.app.domain.repository.SafetyRepository
    val triggerSosUseCase: com.ishara.app.domain.usecase.TriggerSosUseCase
    val getActiveSosUseCase: com.ishara.app.domain.usecase.GetActiveSosUseCase
    val cancelSosUseCase: com.ishara.app.domain.usecase.CancelSosUseCase
    val getEmergencyContactsUseCase: com.ishara.app.domain.usecase.GetEmergencyContactsUseCase
    val createEmergencyContactUseCase: com.ishara.app.domain.usecase.CreateEmergencyContactUseCase
    val updateEmergencyContactUseCase: com.ishara.app.domain.usecase.UpdateEmergencyContactUseCase
    val deleteEmergencyContactUseCase: com.ishara.app.domain.usecase.DeleteEmergencyContactUseCase

    // Phase A14: Ratings, Reviews & Driver Summary
    val ratingRemoteDataSource: com.ishara.app.data.remote.datasource.RatingRemoteDataSource
    val ratingRepository: com.ishara.app.domain.repository.RatingRepository
    val checkRatingEligibilityUseCase: com.ishara.app.domain.usecase.CheckRatingEligibilityUseCase
    val submitRatingUseCase: com.ishara.app.domain.usecase.SubmitRatingUseCase
    val getRideRatingsUseCase: com.ishara.app.domain.usecase.GetRideRatingsUseCase
    val getDriverRatingSummaryUseCase: com.ishara.app.domain.usecase.GetDriverRatingSummaryUseCase

    // Phase 16: Driver Shift History, Earnings & Payout Analytics
    val driverEarningsRemoteDataSource: com.ishara.app.data.remote.datasource.DriverEarningsRemoteDataSource
    val driverEarningsRepository: com.ishara.app.domain.repository.DriverEarningsRepository
    val getDriverEarningsUseCase: com.ishara.app.domain.usecase.GetDriverEarningsUseCase
    val refreshDriverEarningsUseCase: com.ishara.app.domain.usecase.RefreshDriverEarningsUseCase
    val getDriverRideHistoryUseCase: com.ishara.app.domain.usecase.GetDriverRideHistoryUseCase

    // Phase A07: Vehicle Registration & Driver–Vehicle Assignment
    val vehicleRemoteDataSource: com.ishara.app.data.remote.datasource.VehicleRemoteDataSource
    val driverVehicleRepository: com.ishara.app.domain.repository.DriverVehicleRepository
    val getAssignedVehicleUseCase: com.ishara.app.domain.usecase.GetAssignedVehicleUseCase
    val observeAssignedVehicleUseCase: com.ishara.app.domain.usecase.ObserveAssignedVehicleUseCase
    val refreshAssignedVehicleUseCase: com.ishara.app.domain.usecase.RefreshAssignedVehicleUseCase
    val listMyVehiclesUseCase: com.ishara.app.domain.usecase.ListMyVehiclesUseCase
    val getVehicleDetailsUseCase: com.ishara.app.domain.usecase.GetVehicleDetailsUseCase
    val registerVehicleUseCase: com.ishara.app.domain.usecase.RegisterVehicleUseCase
    val assignSelfToVehicleUseCase: com.ishara.app.domain.usecase.AssignSelfToVehicleUseCase
    val unassignVehicleUseCase: com.ishara.app.domain.usecase.UnassignVehicleUseCase
    val getVehicleAssignmentHistoryUseCase: com.ishara.app.domain.usecase.GetVehicleAssignmentHistoryUseCase
    val clearVehicleStateUseCase: com.ishara.app.domain.usecase.ClearVehicleStateUseCase

    // Phase A08: Driver Trip & Dispatch Operational Foundation
    val tripRemoteDataSource: com.ishara.app.data.remote.datasource.TripRemoteDataSource
    val driverTripRepository: com.ishara.app.domain.repository.DriverTripRepository
    val getDriverTripsUseCase: com.ishara.app.domain.usecase.GetDriverTripsUseCase
    val getTripDetailsUseCase: com.ishara.app.domain.usecase.GetTripDetailsUseCase
    val createDriverTripUseCase: com.ishara.app.domain.usecase.CreateDriverTripUseCase
    val startDriverTripUseCase: com.ishara.app.domain.usecase.StartDriverTripUseCase
    val completeDriverTripUseCase: com.ishara.app.domain.usecase.CompleteDriverTripUseCase
    val cancelDriverTripUseCase: com.ishara.app.domain.usecase.CancelDriverTripUseCase
    val observeDriverTripsUseCase: com.ishara.app.domain.usecase.ObserveDriverTripsUseCase
    val observeActiveTripUseCase: com.ishara.app.domain.usecase.ObserveActiveTripUseCase
    val clearDriverTripStateUseCase: com.ishara.app.domain.usecase.ClearDriverTripStateUseCase

    // Phase A15: Notifications, Push Messaging & Device Tokens
    val deviceTokenStore: com.ishara.app.core.storage.DeviceTokenStore
    val deviceTokenRemoteDataSource: com.ishara.app.data.remote.datasource.DeviceTokenRemoteDataSource
    val deviceTokenRepository: com.ishara.app.domain.repository.DeviceTokenRepository
    val registerDeviceTokenUseCase: com.ishara.app.domain.usecase.RegisterDeviceTokenUseCase
    val removeDeviceTokenUseCase: com.ishara.app.domain.usecase.RemoveDeviceTokenUseCase

    val notificationRemoteDataSource: com.ishara.app.data.remote.datasource.NotificationRemoteDataSource
    val notificationRepository: com.ishara.app.domain.repository.NotificationRepository
    val getNotificationsUseCase: com.ishara.app.domain.usecase.GetNotificationsUseCase
    val getUnreadNotificationCountUseCase: com.ishara.app.domain.usecase.GetUnreadNotificationCountUseCase
    val markNotificationAsReadUseCase: com.ishara.app.domain.usecase.MarkNotificationAsReadUseCase
    val markAllNotificationsAsReadUseCase: com.ishara.app.domain.usecase.MarkAllNotificationsAsReadUseCase

    val notificationRouter: com.ishara.app.core.notifications.NotificationRouter
}

class DefaultAppContainer(private val appContext: Context) : AppContainer {

    override val dispatchers: DispatcherProvider by lazy {
        DefaultDispatcherProvider()
    }

    override val networkConfig: NetworkConfig by lazy {
        NetworkConfig()
    }

    override val sessionCoordinator: SessionInvalidationCoordinator by lazy {
        SessionInvalidationCoordinator()
    }

    override val httpClient: IshaaraHttpClient by lazy {
        DefaultIshaaraHttpClient(networkConfig, sessionCoordinator)
    }

    override val sessionStore: SessionStore by lazy {
        EncryptedSessionStore(appContext)
    }

    override val navigationManager: NavigationManager by lazy {
        NavigationManager()
    }

    override val locationProvider: LocationProvider by lazy {
        com.ishara.app.core.location.FusedLocationProvider(appContext)
    }

    override val networkMonitor: com.ishara.app.core.network.NetworkMonitor by lazy {
        com.ishara.app.core.network.NetworkConnectivityManager(appContext)
    }

    override val realtimeClient: RealtimeClient by lazy {
        com.ishara.app.core.realtime.OkHttpRealtimeClient(
            sessionCoordinator = sessionCoordinator,
            networkMonitor = networkMonitor,
            dispatchers = dispatchers
        )
    }

    override val googleAuthClient: GoogleAuthClient by lazy {
        val configuredClientId = com.ishara.app.BuildConfig.GOOGLE_WEB_CLIENT_ID.ifBlank {
            try {
                val resId = appContext.resources.getIdentifier("default_web_client_id", "string", appContext.packageName)
                if (resId != 0) appContext.getString(resId) else null
            } catch (_: Exception) {
                null
            }
        }?.takeIf { it.isNotBlank() && it != "UNCONFIGURED" }

        DefaultGoogleAuthClient(serverClientId = configuredClientId)
    }

    private val authRemoteDataSource: AuthRemoteDataSource by lazy {
        AuthRemoteDataSourceImpl(httpClient, networkConfig)
    }

    private val userRemoteDataSource: UserRemoteDataSource by lazy {
        UserRemoteDataSourceImpl(httpClient, networkConfig)
    }

    override val tripRemoteDataSource: TripRemoteDataSource by lazy {
        TripRemoteDataSourceImpl(httpClient, networkConfig)
    }

    private val driverRemoteDataSource: DriverRemoteDataSource by lazy {
        DriverRemoteDataSourceImpl(httpClient, networkConfig)
    }

    private val sessionLocalDataSource: SessionLocalDataSource by lazy {
        SessionLocalDataSourceImpl(sessionStore)
    }

    override val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(
            remoteDataSource = authRemoteDataSource,
            localDataSource = sessionLocalDataSource,
            sessionCoordinator = sessionCoordinator
        )
    }

    override val userRepository: UserRepository by lazy {
        UserRepositoryImpl(
            remoteDataSource = userRemoteDataSource,
            sessionStore = sessionStore
        )
    }

    override val resolveApplicationDestinationUseCase: ResolveApplicationDestinationUseCase by lazy {
        ResolveApplicationDestinationUseCase()
    }

    override val onboardingRepository: OnboardingRepository by lazy {
        OnboardingRepositoryImpl(
            remoteDataSource = userRemoteDataSource,
            userRepository = userRepository,
            sessionStore = sessionStore,
            destinationResolver = resolveApplicationDestinationUseCase
        )
    }

    private val locationRemoteDataSource: com.ishara.app.data.remote.datasource.LocationRemoteDataSource by lazy {
        com.ishara.app.data.remote.datasource.LocationRemoteDataSourceImpl(httpClient, networkConfig)
    }

    private val recentDestinationsLocalDataSource: com.ishara.app.data.local.datasource.RecentDestinationsLocalDataSource by lazy {
        com.ishara.app.data.local.datasource.InMemoryRecentDestinationsLocalDataSource()
    }

    override val rideRepository: com.ishara.app.domain.repository.RideRepository by lazy {
        com.ishara.app.data.repository.RideRepositoryImpl(httpClient, networkConfig, sessionLocalDataSource)
    }

    override val locationRepository: com.ishara.app.domain.repository.LocationRepository by lazy {
        com.ishara.app.data.repository.LocationRepositoryImpl(
            remoteDataSource = locationRemoteDataSource,
            localDataSource = recentDestinationsLocalDataSource,
            locationProvider = locationProvider,
            sessionLocalDataSource = sessionLocalDataSource
        )
    }

    private val discoveryRemoteDataSource: com.ishara.app.data.remote.datasource.DiscoveryRemoteDataSource by lazy {
        com.ishara.app.data.remote.datasource.DiscoveryRemoteDataSourceImpl(httpClient, networkConfig)
    }

    override val tripRepository: TripRepository by lazy {
        TripRepositoryImpl(tripRemoteDataSource, sessionLocalDataSource, discoveryRemoteDataSource)
    }

    override val driverRepository: DriverRepository by lazy {
        DriverRepositoryImpl(driverRemoteDataSource, sessionLocalDataSource)
    }

    override val getAuthSessionUseCase: GetAuthSessionUseCase by lazy {
        GetAuthSessionUseCase(authRepository)
    }

    override val restoreSessionUseCase: RestoreSessionUseCase by lazy {
        RestoreSessionUseCase(authRepository)
    }

    override val observeAuthStateUseCase: ObserveAuthStateUseCase by lazy {
        ObserveAuthStateUseCase(authRepository)
    }

    override val signInWithGoogleUseCase: SignInWithGoogleUseCase by lazy {
        SignInWithGoogleUseCase(authRepository)
    }

    override val sendEmailOtpUseCase: com.ishara.app.domain.usecase.SendEmailOtpUseCase by lazy {
        com.ishara.app.domain.usecase.SendEmailOtpUseCase(authRepository)
    }

    override val signInWithEmailOtpUseCase: com.ishara.app.domain.usecase.SignInWithEmailOtpUseCase by lazy {
        com.ishara.app.domain.usecase.SignInWithEmailOtpUseCase(authRepository)
    }

    override val signOutUseCase: SignOutUseCase by lazy {
        SignOutUseCase(authRepository)
    }

    override val getCurrentUserProfileUseCase: GetCurrentUserProfileUseCase by lazy {
        GetCurrentUserProfileUseCase(userRepository)
    }

    override val updateUserProfileUseCase: com.ishara.app.domain.usecase.UpdateUserProfileUseCase by lazy {
        com.ishara.app.domain.usecase.UpdateUserProfileUseCase(userRepository)
    }

    override val getOnboardingStateUseCase: GetOnboardingStateUseCase by lazy {
        GetOnboardingStateUseCase(onboardingRepository)
    }

    override val submitOnboardingUseCase: SubmitOnboardingUseCase by lazy {
        SubmitOnboardingUseCase(onboardingRepository)
    }

    override val discoverTripsUseCase: DiscoverTripsUseCase by lazy {
        DiscoverTripsUseCase(tripRepository)
    }

    override val getPassengerTripDetailsUseCase: com.ishara.app.domain.usecase.GetPassengerTripDetailsUseCase by lazy {
        com.ishara.app.domain.usecase.GetPassengerTripDetailsUseCase(tripRepository)
    }

    override val updateDriverLocationUseCase: UpdateDriverLocationUseCase by lazy {
        UpdateDriverLocationUseCase(driverRepository)
    }

    override val getDriverOperationalContextUseCase: GetDriverOperationalContextUseCase by lazy {
        GetDriverOperationalContextUseCase(driverRepository)
    }

    override val setDriverAvailabilityUseCase: SetDriverAvailabilityUseCase by lazy {
        SetDriverAvailabilityUseCase(driverRepository)
    }

    override val manageDriverTripLifecycleUseCase: ManageDriverTripLifecycleUseCase by lazy {
        ManageDriverTripLifecycleUseCase(driverRepository)
    }

    override val getDriverProfileUseCase: com.ishara.app.domain.usecase.GetDriverProfileUseCase by lazy {
        com.ishara.app.domain.usecase.GetDriverProfileUseCase(driverRepository)
    }

    override val createDriverProfileUseCase: com.ishara.app.domain.usecase.CreateDriverProfileUseCase by lazy {
        com.ishara.app.domain.usecase.CreateDriverProfileUseCase(driverRepository)
    }

    override val updateDriverProfileUseCase: com.ishara.app.domain.usecase.UpdateDriverProfileUseCase by lazy {
        com.ishara.app.domain.usecase.UpdateDriverProfileUseCase(driverRepository)
    }

    override val getDriverVerificationStatusUseCase: com.ishara.app.domain.usecase.GetDriverVerificationStatusUseCase by lazy {
        com.ishara.app.domain.usecase.GetDriverVerificationStatusUseCase(driverRepository)
    }

    override val submitDriverVerificationUseCase: com.ishara.app.domain.usecase.SubmitDriverVerificationUseCase by lazy {
        com.ishara.app.domain.usecase.SubmitDriverVerificationUseCase(driverRepository)
    }

    override val observeDriverProfileUseCase: com.ishara.app.domain.usecase.ObserveDriverProfileUseCase by lazy {
        com.ishara.app.domain.usecase.ObserveDriverProfileUseCase(driverRepository)
    }

    override val observeDriverOnboardingStateUseCase: com.ishara.app.domain.usecase.ObserveDriverOnboardingStateUseCase by lazy {
        com.ishara.app.domain.usecase.ObserveDriverOnboardingStateUseCase(driverRepository)
    }

    override val observeDriverProfileStateUseCase: com.ishara.app.domain.usecase.ObserveDriverProfileStateUseCase by lazy {
        com.ishara.app.domain.usecase.ObserveDriverProfileStateUseCase(driverRepository)
    }

    override val refreshDriverProfileUseCase: com.ishara.app.domain.usecase.RefreshDriverProfileUseCase by lazy {
        com.ishara.app.domain.usecase.RefreshDriverProfileUseCase(driverRepository)
    }

    override val clearDriverStateUseCase: com.ishara.app.domain.usecase.ClearDriverStateUseCase by lazy {
        com.ishara.app.domain.usecase.ClearDriverStateUseCase(driverRepository)
    }

    // Phase A05: Agency Membership & Fleet Association
    override val agencyRemoteDataSource: com.ishara.app.data.remote.datasource.AgencyRemoteDataSource by lazy {
        com.ishara.app.data.remote.datasource.AgencyRemoteDataSourceImpl(httpClient, networkConfig)
    }

    override val agencyMembershipRepository: com.ishara.app.domain.repository.AgencyMembershipRepository by lazy {
        com.ishara.app.data.repository.AgencyMembershipRepositoryImpl(
            remoteDataSource = agencyRemoteDataSource,
            sessionStore = sessionStore
        )
    }

    override val getCurrentAgencyMembershipUseCase: com.ishara.app.domain.usecase.GetCurrentAgencyMembershipUseCase by lazy {
        com.ishara.app.domain.usecase.GetCurrentAgencyMembershipUseCase(agencyMembershipRepository)
    }

    override val observeAgencyMembershipStateUseCase: com.ishara.app.domain.usecase.ObserveAgencyMembershipStateUseCase by lazy {
        com.ishara.app.domain.usecase.ObserveAgencyMembershipStateUseCase(agencyMembershipRepository)
    }

    override val observeCurrentAgencyMembershipUseCase: com.ishara.app.domain.usecase.ObserveCurrentAgencyMembershipUseCase by lazy {
        com.ishara.app.domain.usecase.ObserveCurrentAgencyMembershipUseCase(agencyMembershipRepository)
    }

    override val listAgenciesUseCase: com.ishara.app.domain.usecase.ListAgenciesUseCase by lazy {
        com.ishara.app.domain.usecase.ListAgenciesUseCase(agencyMembershipRepository)
    }

    override val getAgencyDetailsUseCase: com.ishara.app.domain.usecase.GetAgencyDetailsUseCase by lazy {
        com.ishara.app.domain.usecase.GetAgencyDetailsUseCase(agencyMembershipRepository)
    }

    override val requestAgencyMembershipUseCase: com.ishara.app.domain.usecase.RequestAgencyMembershipUseCase by lazy {
        com.ishara.app.domain.usecase.RequestAgencyMembershipUseCase(agencyMembershipRepository)
    }

    override val cancelAgencyMembershipUseCase: com.ishara.app.domain.usecase.CancelAgencyMembershipUseCase by lazy {
        com.ishara.app.domain.usecase.CancelAgencyMembershipUseCase(agencyMembershipRepository)
    }

    override val refreshAgencyMembershipUseCase: com.ishara.app.domain.usecase.RefreshAgencyMembershipUseCase by lazy {
        com.ishara.app.domain.usecase.RefreshAgencyMembershipUseCase(agencyMembershipRepository)
    }

    override val clearAgencyMembershipStateUseCase: com.ishara.app.domain.usecase.ClearAgencyMembershipStateUseCase by lazy {
        com.ishara.app.domain.usecase.ClearAgencyMembershipStateUseCase(agencyMembershipRepository)
    }

    // Phase A06: Driver Operational Readiness
    override val driverReadinessRemoteDataSource: com.ishara.app.data.remote.datasource.DriverReadinessRemoteDataSource by lazy {
        com.ishara.app.data.remote.datasource.DriverReadinessRemoteDataSourceImpl(httpClient, networkConfig)
    }

    override val driverReadinessRepository: com.ishara.app.domain.repository.DriverReadinessRepository by lazy {
        com.ishara.app.data.repository.DriverReadinessRepositoryImpl(
            remoteDataSource = driverReadinessRemoteDataSource,
            localDataSource = sessionLocalDataSource
        )
    }

    override val getDriverReadinessUseCase: com.ishara.app.domain.usecase.GetDriverReadinessUseCase by lazy {
        com.ishara.app.domain.usecase.GetDriverReadinessUseCase(driverReadinessRepository)
    }

    override val observeDriverReadinessUseCase: com.ishara.app.domain.usecase.ObserveDriverReadinessUseCase by lazy {
        com.ishara.app.domain.usecase.ObserveDriverReadinessUseCase(driverReadinessRepository)
    }

    override val refreshDriverReadinessUseCase: com.ishara.app.domain.usecase.RefreshDriverReadinessUseCase by lazy {
        com.ishara.app.domain.usecase.RefreshDriverReadinessUseCase(driverReadinessRepository)
    }

    override val clearDriverReadinessStateUseCase: com.ishara.app.domain.usecase.ClearDriverReadinessStateUseCase by lazy {
        com.ishara.app.domain.usecase.ClearDriverReadinessStateUseCase(driverReadinessRepository)
    }

    // Phase A07: Vehicle Registration & Driver–Vehicle Assignment
    override val vehicleRemoteDataSource: com.ishara.app.data.remote.datasource.VehicleRemoteDataSource by lazy {
        com.ishara.app.data.remote.datasource.VehicleRemoteDataSourceImpl(httpClient, networkConfig)
    }

    override val driverVehicleRepository: com.ishara.app.domain.repository.DriverVehicleRepository by lazy {
        com.ishara.app.data.repository.DriverVehicleRepositoryImpl(
            remoteDataSource = vehicleRemoteDataSource,
            localDataSource = sessionLocalDataSource
        )
    }

    override val getAssignedVehicleUseCase: com.ishara.app.domain.usecase.GetAssignedVehicleUseCase by lazy {
        com.ishara.app.domain.usecase.GetAssignedVehicleUseCase(driverVehicleRepository)
    }

    override val observeAssignedVehicleUseCase: com.ishara.app.domain.usecase.ObserveAssignedVehicleUseCase by lazy {
        com.ishara.app.domain.usecase.ObserveAssignedVehicleUseCase(driverVehicleRepository)
    }

    override val refreshAssignedVehicleUseCase: com.ishara.app.domain.usecase.RefreshAssignedVehicleUseCase by lazy {
        com.ishara.app.domain.usecase.RefreshAssignedVehicleUseCase(driverVehicleRepository, driverReadinessRepository)
    }

    override val listMyVehiclesUseCase: com.ishara.app.domain.usecase.ListMyVehiclesUseCase by lazy {
        com.ishara.app.domain.usecase.ListMyVehiclesUseCase(driverVehicleRepository)
    }

    override val getVehicleDetailsUseCase: com.ishara.app.domain.usecase.GetVehicleDetailsUseCase by lazy {
        com.ishara.app.domain.usecase.GetVehicleDetailsUseCase(driverVehicleRepository)
    }

    override val registerVehicleUseCase: com.ishara.app.domain.usecase.RegisterVehicleUseCase by lazy {
        com.ishara.app.domain.usecase.RegisterVehicleUseCase(driverVehicleRepository)
    }

    override val assignSelfToVehicleUseCase: com.ishara.app.domain.usecase.AssignSelfToVehicleUseCase by lazy {
        com.ishara.app.domain.usecase.AssignSelfToVehicleUseCase(driverVehicleRepository, driverReadinessRepository)
    }

    override val unassignVehicleUseCase: com.ishara.app.domain.usecase.UnassignVehicleUseCase by lazy {
        com.ishara.app.domain.usecase.UnassignVehicleUseCase(driverVehicleRepository, driverReadinessRepository)
    }

    override val getVehicleAssignmentHistoryUseCase: com.ishara.app.domain.usecase.GetVehicleAssignmentHistoryUseCase by lazy {
        com.ishara.app.domain.usecase.GetVehicleAssignmentHistoryUseCase(driverVehicleRepository)
    }

    override val clearVehicleStateUseCase: com.ishara.app.domain.usecase.ClearVehicleStateUseCase by lazy {
        com.ishara.app.domain.usecase.ClearVehicleStateUseCase(driverVehicleRepository)
    }

    // Phase A08: Driver Trip & Dispatch Operational Foundation
    override val driverTripRepository: com.ishara.app.domain.repository.DriverTripRepository by lazy {
        com.ishara.app.data.repository.DriverTripRepositoryImpl(
            remoteDataSource = tripRemoteDataSource,
            localDataSource = sessionLocalDataSource
        )
    }

    override val getDriverTripsUseCase: com.ishara.app.domain.usecase.GetDriverTripsUseCase by lazy {
        com.ishara.app.domain.usecase.GetDriverTripsUseCase(driverTripRepository)
    }

    override val getTripDetailsUseCase: com.ishara.app.domain.usecase.GetTripDetailsUseCase by lazy {
        com.ishara.app.domain.usecase.GetTripDetailsUseCase(driverTripRepository)
    }

    override val createDriverTripUseCase: com.ishara.app.domain.usecase.CreateDriverTripUseCase by lazy {
        com.ishara.app.domain.usecase.CreateDriverTripUseCase(driverTripRepository)
    }

    override val startDriverTripUseCase: com.ishara.app.domain.usecase.StartDriverTripUseCase by lazy {
        com.ishara.app.domain.usecase.StartDriverTripUseCase(driverTripRepository)
    }

    override val completeDriverTripUseCase: com.ishara.app.domain.usecase.CompleteDriverTripUseCase by lazy {
        com.ishara.app.domain.usecase.CompleteDriverTripUseCase(driverTripRepository)
    }

    override val cancelDriverTripUseCase: com.ishara.app.domain.usecase.CancelDriverTripUseCase by lazy {
        com.ishara.app.domain.usecase.CancelDriverTripUseCase(driverTripRepository)
    }

    override val observeDriverTripsUseCase: com.ishara.app.domain.usecase.ObserveDriverTripsUseCase by lazy {
        com.ishara.app.domain.usecase.ObserveDriverTripsUseCase(driverTripRepository)
    }

    override val observeActiveTripUseCase: com.ishara.app.domain.usecase.ObserveActiveTripUseCase by lazy {
        com.ishara.app.domain.usecase.ObserveActiveTripUseCase(driverTripRepository)
    }

    override val clearDriverTripStateUseCase: com.ishara.app.domain.usecase.ClearDriverTripStateUseCase by lazy {
        com.ishara.app.domain.usecase.ClearDriverTripStateUseCase(driverTripRepository)
    }



    override val searchLocationsUseCase: com.ishara.app.domain.usecase.SearchLocationsUseCase by lazy {
        com.ishara.app.domain.usecase.SearchLocationsUseCase(locationRepository)
    }

    override val resolveCurrentLocationUseCase: com.ishara.app.domain.usecase.ResolveCurrentLocationUseCase by lazy {
        com.ishara.app.domain.usecase.ResolveCurrentLocationUseCase(locationRepository)
    }

    override val manageRecentDestinationsUseCase: com.ishara.app.domain.usecase.ManageRecentDestinationsUseCase by lazy {
        com.ishara.app.domain.usecase.ManageRecentDestinationsUseCase(locationRepository)
    }

    override val getActiveRideUseCase: com.ishara.app.domain.usecase.GetActiveRideUseCase by lazy {
        com.ishara.app.domain.usecase.GetActiveRideUseCase(rideRepository)
    }

    override val createRideRequestUseCase: com.ishara.app.domain.usecase.CreateRideRequestUseCase by lazy {
        com.ishara.app.domain.usecase.CreateRideRequestUseCase(rideRepository)
    }

    override val getRideRequestUseCase: com.ishara.app.domain.usecase.GetRideRequestUseCase by lazy {
        com.ishara.app.domain.usecase.GetRideRequestUseCase(rideRepository)
    }

    override val cancelRideRequestUseCase: com.ishara.app.domain.usecase.CancelRideRequestUseCase by lazy {
        com.ishara.app.domain.usecase.CancelRideRequestUseCase(rideRepository)
    }

    override val getUserRideRequestsUseCase: com.ishara.app.domain.usecase.GetUserRideRequestsUseCase by lazy {
        com.ishara.app.domain.usecase.GetUserRideRequestsUseCase(rideRepository)
    }

    override val clearRideRequestStateUseCase: com.ishara.app.domain.usecase.ClearRideRequestStateUseCase by lazy {
        com.ishara.app.domain.usecase.ClearRideRequestStateUseCase()
    }

    override val driverTrackingCoordinator: com.ishara.app.feature.driver.tracking.DriverTrackingCoordinator by lazy {
        com.ishara.app.feature.driver.tracking.DriverTrackingCoordinator(
            locationProvider = locationProvider,
            updateDriverLocationUseCase = updateDriverLocationUseCase,
            dispatchers = dispatchers
        )
    }

    override val startDriverTrackingUseCase: com.ishara.app.domain.usecase.StartDriverTrackingUseCase by lazy {
        com.ishara.app.domain.usecase.StartDriverTrackingUseCase(driverTrackingCoordinator)
    }

    override val stopDriverTrackingUseCase: com.ishara.app.domain.usecase.StopDriverTrackingUseCase by lazy {
        com.ishara.app.domain.usecase.StopDriverTrackingUseCase(driverTrackingCoordinator)
    }

    override val observeDriverTrackingStatusUseCase: com.ishara.app.domain.usecase.ObserveDriverTrackingStatusUseCase by lazy {
        com.ishara.app.domain.usecase.ObserveDriverTrackingStatusUseCase(driverTrackingCoordinator)
    }

    private val driverRideRequestRemoteDataSource: com.ishara.app.data.remote.datasource.DriverRideRequestRemoteDataSource by lazy {
        com.ishara.app.data.remote.datasource.DriverRideRequestRemoteDataSourceImpl(httpClient, networkConfig)
    }

    override val driverRideRequestRepository: com.ishara.app.domain.repository.DriverRideRequestRepository by lazy {
        com.ishara.app.data.repository.DriverRideRequestRepositoryImpl(
            remoteDataSource = driverRideRequestRemoteDataSource,
            sessionLocalDataSource = sessionLocalDataSource,
            networkConfig = networkConfig,
            realtimeClient = realtimeClient,
            dispatchers = dispatchers
        )
    }

    override val getDriverRideRequestsUseCase: com.ishara.app.domain.usecase.GetDriverRideRequestsUseCase by lazy {
        com.ishara.app.domain.usecase.GetDriverRideRequestsUseCase(driverRideRequestRepository)
    }

    override val acceptRideRequestUseCase: com.ishara.app.domain.usecase.AcceptRideRequestUseCase by lazy {
        com.ishara.app.domain.usecase.AcceptRideRequestUseCase(driverRideRequestRepository)
    }

    override val rejectRideRequestUseCase: com.ishara.app.domain.usecase.RejectRideRequestUseCase by lazy {
        com.ishara.app.domain.usecase.RejectRideRequestUseCase(driverRideRequestRepository)
    }

    override val managePassengerBoardingUseCase: com.ishara.app.domain.usecase.ManagePassengerBoardingUseCase by lazy {
        com.ishara.app.domain.usecase.ManagePassengerBoardingUseCase(driverRideRequestRepository)
    }

    // Phase 11: Live Passenger Map / Ride Tracking
    private val rideTrackingRemoteDataSource: com.ishara.app.data.remote.datasource.RideTrackingRemoteDataSource by lazy {
        com.ishara.app.data.remote.datasource.DefaultRideTrackingRemoteDataSource(httpClient, networkConfig)
    }

    override val rideTrackingRepository: com.ishara.app.domain.repository.RideTrackingRepository by lazy {
        com.ishara.app.data.repository.RideTrackingRepositoryImpl(
            remoteDataSource = rideTrackingRemoteDataSource,
            sessionLocalDataSource = sessionLocalDataSource,
            networkConfig = networkConfig,
            realtimeClient = realtimeClient,
            dispatchers = dispatchers
        )
    }

    override val getRideTrackingUseCase: com.ishara.app.domain.usecase.GetRideTrackingUseCase by lazy {
        com.ishara.app.domain.usecase.GetRideTrackingUseCase(rideTrackingRepository)
    }

    override val getDriverLocationUseCase: com.ishara.app.domain.usecase.GetDriverLocationUseCase by lazy {
        com.ishara.app.domain.usecase.GetDriverLocationUseCase(rideTrackingRepository)
    }

    override val observeRideTrackingUseCase: com.ishara.app.domain.usecase.ObserveRideTrackingUseCase by lazy {
        com.ishara.app.domain.usecase.ObserveRideTrackingUseCase(rideTrackingRepository)
    }

    // Phase A12: Fare & Authoritative Pricing Foundation
    private val fareRemoteDataSource: com.ishara.app.data.remote.datasource.FareRemoteDataSource by lazy {
        com.ishara.app.data.remote.datasource.DefaultFareRemoteDataSource(httpClient, networkConfig)
    }

    override val fareRepository: com.ishara.app.domain.repository.FareRepository by lazy {
        com.ishara.app.data.repository.FareRepositoryImpl(
            remoteDataSource = fareRemoteDataSource,
            sessionLocalDataSource = sessionLocalDataSource,
            dispatchers = dispatchers
        )
    }

    override val getRideFareUseCase: com.ishara.app.domain.usecase.GetRideFareUseCase by lazy {
        com.ishara.app.domain.usecase.GetRideFareUseCase(fareRepository)
    }

    override val clearFareStateUseCase: com.ishara.app.domain.usecase.ClearFareStateUseCase by lazy {
        com.ishara.app.domain.usecase.ClearFareStateUseCase(fareRepository)
    }

    // Phase 12: Hands-Free Voice Trip Creation
    override val voiceInputClient: com.ishara.app.core.voice.VoiceInputClient by lazy {
        com.ishara.app.core.voice.AndroidSpeechRecognizerClient(appContext)
    }

    private val voiceTripDraftRemoteDataSource: com.ishara.app.data.remote.datasource.VoiceTripDraftRemoteDataSource by lazy {
        com.ishara.app.data.remote.datasource.VoiceTripDraftRemoteDataSourceImpl(httpClient, networkConfig)
    }

    override val voiceTripRepository: com.ishara.app.domain.repository.VoiceTripRepository by lazy {
        com.ishara.app.data.repository.VoiceTripRepositoryImpl(
            remoteDataSource = voiceTripDraftRemoteDataSource,
            sessionStore = sessionStore,
            dispatchers = dispatchers
        )
    }

    override val createVoiceTripDraftUseCase: com.ishara.app.domain.usecase.CreateVoiceTripDraftUseCase by lazy {
        com.ishara.app.domain.usecase.CreateVoiceTripDraftUseCase(voiceTripRepository)
    }

    override val cancelVoiceTripDraftUseCase: com.ishara.app.domain.usecase.CancelVoiceTripDraftUseCase by lazy {
        com.ishara.app.domain.usecase.CancelVoiceTripDraftUseCase(voiceTripRepository)
    }

    // Phase 13: Digital Ticketing, Fare & Payment
    override val paymentLauncher: com.ishara.app.core.payment.PaymentLauncher by lazy {
        com.ishara.app.core.payment.DefaultPaymentLauncher(isTestMode = true)
    }

    private val paymentRemoteDataSource: com.ishara.app.data.remote.datasource.PaymentRemoteDataSource by lazy {
        com.ishara.app.data.remote.datasource.PaymentRemoteDataSourceImpl(httpClient, networkConfig)
    }

    override val paymentRepository: com.ishara.app.domain.repository.PaymentRepository by lazy {
        com.ishara.app.data.repository.PaymentRepositoryImpl(
            remoteDataSource = paymentRemoteDataSource,
            sessionStore = sessionStore,
            dispatchers = dispatchers
        )
    }

    override val createPaymentOrderUseCase: com.ishara.app.domain.usecase.CreatePaymentOrderUseCase by lazy {
        com.ishara.app.domain.usecase.CreatePaymentOrderUseCase(paymentRepository)
    }

    override val getRidePaymentUseCase: com.ishara.app.domain.usecase.GetRidePaymentUseCase by lazy {
        com.ishara.app.domain.usecase.GetRidePaymentUseCase(paymentRepository)
    }

    override val verifyPaymentUseCase: com.ishara.app.domain.usecase.VerifyPaymentUseCase by lazy {
        com.ishara.app.domain.usecase.VerifyPaymentUseCase(paymentRepository)
    }

    override val clearPaymentStateUseCase: com.ishara.app.domain.usecase.ClearPaymentStateUseCase by lazy {
        com.ishara.app.domain.usecase.ClearPaymentStateUseCase(paymentRepository)
    }

    // Phase 14: Route Schedules, Bus Stops & Offline Transit Cache
    private val transitDatabaseHelper: com.ishara.app.data.local.db.TransitDatabaseHelper by lazy {
        com.ishara.app.data.local.db.TransitDatabaseHelper(appContext)
    }

    private val transitLocalDataSource: com.ishara.app.data.local.datasource.TransitLocalDataSource by lazy {
        com.ishara.app.data.local.datasource.SqliteTransitLocalDataSource(transitDatabaseHelper)
    }

    private val transitRemoteDataSource: com.ishara.app.data.remote.datasource.TransitRemoteDataSource by lazy {
        com.ishara.app.data.remote.datasource.TransitRemoteDataSourceImpl(httpClient, networkConfig)
    }

    override val transitRepository: com.ishara.app.domain.repository.TransitRepository by lazy {
        com.ishara.app.data.repository.TransitRepositoryImpl(
            remoteDataSource = transitRemoteDataSource,
            localDataSource = transitLocalDataSource,
            sessionStore = sessionStore,
            dispatchers = dispatchers
        )
    }

    override val getTransitRoutesUseCase: com.ishara.app.domain.usecase.GetTransitRoutesUseCase by lazy {
        com.ishara.app.domain.usecase.GetTransitRoutesUseCase(transitRepository)
    }

    override val getRouteDetailsUseCase: com.ishara.app.domain.usecase.GetRouteDetailsUseCase by lazy {
        com.ishara.app.domain.usecase.GetRouteDetailsUseCase(transitRepository)
    }

    override val refreshTransitRoutesUseCase: com.ishara.app.domain.usecase.RefreshTransitRoutesUseCase by lazy {
        com.ishara.app.domain.usecase.RefreshTransitRoutesUseCase(transitRepository)
    }

    // Phase 15: Safety, SOS & Emergency Response
    override val safetyRemoteDataSource: com.ishara.app.data.remote.datasource.SafetyRemoteDataSource by lazy {
        com.ishara.app.data.remote.datasource.SafetyRemoteDataSourceImpl(httpClient, networkConfig)
    }

    override val safetyRepository: com.ishara.app.domain.repository.SafetyRepository by lazy {
        com.ishara.app.data.repository.SafetyRepositoryImpl(
            remoteDataSource = safetyRemoteDataSource,
            sessionStore = sessionStore,
            dispatchers = dispatchers
        )
    }

    override val triggerSosUseCase: com.ishara.app.domain.usecase.TriggerSosUseCase by lazy {
        com.ishara.app.domain.usecase.TriggerSosUseCase(safetyRepository)
    }

    override val getActiveSosUseCase: com.ishara.app.domain.usecase.GetActiveSosUseCase by lazy {
        com.ishara.app.domain.usecase.GetActiveSosUseCase(safetyRepository)
    }

    override val cancelSosUseCase: com.ishara.app.domain.usecase.CancelSosUseCase by lazy {
        com.ishara.app.domain.usecase.CancelSosUseCase(safetyRepository)
    }

    override val getEmergencyContactsUseCase: com.ishara.app.domain.usecase.GetEmergencyContactsUseCase by lazy {
        com.ishara.app.domain.usecase.GetEmergencyContactsUseCase(safetyRepository)
    }

    override val createEmergencyContactUseCase: com.ishara.app.domain.usecase.CreateEmergencyContactUseCase by lazy {
        com.ishara.app.domain.usecase.CreateEmergencyContactUseCase(safetyRepository)
    }

    override val updateEmergencyContactUseCase: com.ishara.app.domain.usecase.UpdateEmergencyContactUseCase by lazy {
        com.ishara.app.domain.usecase.UpdateEmergencyContactUseCase(safetyRepository)
    }

    override val deleteEmergencyContactUseCase: com.ishara.app.domain.usecase.DeleteEmergencyContactUseCase by lazy {
        com.ishara.app.domain.usecase.DeleteEmergencyContactUseCase(safetyRepository)
    }

    // Phase A14: Ratings, Reviews & Driver Summary
    override val ratingRemoteDataSource: com.ishara.app.data.remote.datasource.RatingRemoteDataSource by lazy {
        com.ishara.app.data.remote.datasource.RatingRemoteDataSourceImpl(
            httpClient = httpClient,
            networkConfig = networkConfig
        )
    }

    override val ratingRepository: com.ishara.app.domain.repository.RatingRepository by lazy {
        com.ishara.app.data.repository.RatingRepositoryImpl(
            remoteDataSource = ratingRemoteDataSource,
            sessionStore = sessionStore,
            dispatchers = dispatchers
        )
    }

    override val checkRatingEligibilityUseCase: com.ishara.app.domain.usecase.CheckRatingEligibilityUseCase by lazy {
        com.ishara.app.domain.usecase.CheckRatingEligibilityUseCase(ratingRepository)
    }

    override val submitRatingUseCase: com.ishara.app.domain.usecase.SubmitRatingUseCase by lazy {
        com.ishara.app.domain.usecase.SubmitRatingUseCase(ratingRepository)
    }

    override val getRideRatingsUseCase: com.ishara.app.domain.usecase.GetRideRatingsUseCase by lazy {
        com.ishara.app.domain.usecase.GetRideRatingsUseCase(ratingRepository)
    }

    override val getDriverRatingSummaryUseCase: com.ishara.app.domain.usecase.GetDriverRatingSummaryUseCase by lazy {
        com.ishara.app.domain.usecase.GetDriverRatingSummaryUseCase(ratingRepository)
    }

    // Phase 16: Driver Shift History, Earnings & Payout Analytics
    override val driverEarningsRemoteDataSource: com.ishara.app.data.remote.datasource.DriverEarningsRemoteDataSource by lazy {
        com.ishara.app.data.remote.datasource.DriverEarningsRemoteDataSourceImpl(
            httpClient = httpClient,
            networkConfig = networkConfig
        )
    }

    override val driverEarningsRepository: com.ishara.app.domain.repository.DriverEarningsRepository by lazy {
        com.ishara.app.data.repository.DriverEarningsRepositoryImpl(
            remoteDataSource = driverEarningsRemoteDataSource,
            sessionStore = sessionStore,
            dispatchers = dispatchers
        )
    }

    override val getDriverEarningsUseCase: com.ishara.app.domain.usecase.GetDriverEarningsUseCase by lazy {
        com.ishara.app.domain.usecase.GetDriverEarningsUseCase(driverEarningsRepository)
    }

    override val refreshDriverEarningsUseCase: com.ishara.app.domain.usecase.RefreshDriverEarningsUseCase by lazy {
        com.ishara.app.domain.usecase.RefreshDriverEarningsUseCase(driverEarningsRepository)
    }

    override val getDriverRideHistoryUseCase: com.ishara.app.domain.usecase.GetDriverRideHistoryUseCase by lazy {
        com.ishara.app.domain.usecase.GetDriverRideHistoryUseCase(driverEarningsRepository)
    }

    // Phase A15: Notifications, Push Messaging & Device Tokens
    override val deviceTokenStore: com.ishara.app.core.storage.DeviceTokenStore by lazy {
        com.ishara.app.core.storage.InMemoryDeviceTokenStore()
    }

    override val deviceTokenRemoteDataSource: com.ishara.app.data.remote.datasource.DeviceTokenRemoteDataSource by lazy {
        com.ishara.app.data.remote.datasource.DeviceTokenRemoteDataSourceImpl(httpClient, networkConfig)
    }

    override val deviceTokenRepository: com.ishara.app.domain.repository.DeviceTokenRepository by lazy {
        com.ishara.app.data.repository.DeviceTokenRepositoryImpl(
            remoteDataSource = deviceTokenRemoteDataSource,
            tokenStore = deviceTokenStore,
            sessionStore = sessionStore,
            dispatchers = dispatchers
        )
    }

    override val registerDeviceTokenUseCase: com.ishara.app.domain.usecase.RegisterDeviceTokenUseCase by lazy {
        com.ishara.app.domain.usecase.RegisterDeviceTokenUseCase(deviceTokenRepository)
    }

    override val removeDeviceTokenUseCase: com.ishara.app.domain.usecase.RemoveDeviceTokenUseCase by lazy {
        com.ishara.app.domain.usecase.RemoveDeviceTokenUseCase(deviceTokenRepository)
    }

    override val notificationRemoteDataSource: com.ishara.app.data.remote.datasource.NotificationRemoteDataSource by lazy {
        com.ishara.app.data.remote.datasource.NotificationRemoteDataSourceImpl(httpClient, networkConfig)
    }

    override val notificationRepository: com.ishara.app.domain.repository.NotificationRepository by lazy {
        com.ishara.app.data.repository.NotificationRepositoryImpl(
            remoteDataSource = notificationRemoteDataSource,
            sessionStore = sessionStore,
            dispatchers = dispatchers
        )
    }

    override val getNotificationsUseCase: com.ishara.app.domain.usecase.GetNotificationsUseCase by lazy {
        com.ishara.app.domain.usecase.GetNotificationsUseCase(notificationRepository)
    }

    override val getUnreadNotificationCountUseCase: com.ishara.app.domain.usecase.GetUnreadNotificationCountUseCase by lazy {
        com.ishara.app.domain.usecase.GetUnreadNotificationCountUseCase(notificationRepository)
    }

    override val markNotificationAsReadUseCase: com.ishara.app.domain.usecase.MarkNotificationAsReadUseCase by lazy {
        com.ishara.app.domain.usecase.MarkNotificationAsReadUseCase(notificationRepository)
    }

    override val markAllNotificationsAsReadUseCase: com.ishara.app.domain.usecase.MarkAllNotificationsAsReadUseCase by lazy {
        com.ishara.app.domain.usecase.MarkAllNotificationsAsReadUseCase(notificationRepository)
    }

    override val notificationRouter: com.ishara.app.core.notifications.NotificationRouter by lazy {
        com.ishara.app.core.notifications.NotificationRouter(navigationManager)
    }
}

/**
 * Default fallback LocationProvider.
 */
private class DefaultLocationProvider : LocationProvider {
    override suspend fun getCurrentLocation(): IshaaraResult<LocationCoordinates> {
        return IshaaraResult.success(
            LocationCoordinates(latitude = 18.5204, longitude = 73.8567)
        )
    }

    override fun observeLocationUpdates(intervalMillis: Long): Flow<LocationCoordinates> = emptyFlow()

    override fun isLocationAvailable(): Boolean = true
}

/**
 * Default fallback RealtimeClient.
 */
private class DefaultRealtimeClient : RealtimeClient {
    private val stateFlow = MutableStateFlow<RealtimeConnectionState>(RealtimeConnectionState.Disconnected)
    private val eventsFlow = MutableSharedFlow<RealtimeEvent>(extraBufferCapacity = 16)

    override suspend fun connect(endpoint: String, token: String): IshaaraResult<Unit> {
        stateFlow.value = RealtimeConnectionState.Connected
        return IshaaraResult.success(Unit)
    }

    override suspend fun disconnect() {
        stateFlow.value = RealtimeConnectionState.Disconnected
    }

    override suspend fun send(message: String): IshaaraResult<Unit> {
        return IshaaraResult.success(Unit)
    }

    override fun observeConnectionState(): Flow<RealtimeConnectionState> = stateFlow.asStateFlow()

    override fun observeEvents(): Flow<RealtimeEvent> = eventsFlow.asSharedFlow()
}
