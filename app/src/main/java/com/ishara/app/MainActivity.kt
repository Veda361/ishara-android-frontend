package com.ishara.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ishara.app.core.di.AppContainer
import com.ishara.app.core.designsystem.component.IshaaraBadge
import com.ishara.app.core.designsystem.component.IshaaraButton
import com.ishara.app.core.designsystem.component.IshaaraButtonVariant
import com.ishara.app.core.designsystem.component.IshaaraPrimaryAction
import com.ishara.app.core.designsystem.component.IshaaraStatusChip
import com.ishara.app.core.designsystem.component.IshaaraTransitStatus
import com.ishara.app.core.designsystem.theme.IshaaraTheme
import com.ishara.app.domain.model.ApplicationDestination
import com.ishara.app.domain.model.AuthState
import com.ishara.app.domain.model.OnboardingState
import com.ishara.app.domain.model.UserRole
import com.ishara.app.feature.auth.AuthViewModel
import com.ishara.app.feature.auth.ui.LoginScreen
import com.ishara.app.feature.auth.ui.SplashScreen
import com.ishara.app.feature.onboarding.OnboardingViewModel
import com.ishara.app.feature.onboarding.ui.RoleSelectionScreen
import com.ishara.app.feature.onboarding.ui.SafeRecoveryScreen

/**
 * Root Activity for Ishaara.
 * Implements centralized, state-driven navigation gating based on AuthState and OnboardingState.
 * Prevents navigation flicker, protects authenticated screens, and prevents back-stack leakage after logout.
 */
class MainActivity : ComponentActivity() {

    private lateinit var authViewModel: AuthViewModel
    private lateinit var onboardingViewModel: OnboardingViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as IshaaraApplication).appContainer
        com.ishara.app.core.notifications.NotificationChannelConfig.createNotificationChannels(this)

        authViewModel = AuthViewModel(
            getAuthSessionUseCase = appContainer.getAuthSessionUseCase,
            signInWithGoogleUseCase = appContainer.signInWithGoogleUseCase,
            signOutUseCase = appContainer.signOutUseCase,
            navigationManager = appContainer.navigationManager,
            restoreSessionUseCase = appContainer.restoreSessionUseCase,
            observeAuthStateUseCase = appContainer.observeAuthStateUseCase,
            googleAuthClient = appContainer.googleAuthClient,
            sendEmailOtpUseCase = appContainer.sendEmailOtpUseCase,
            signInWithEmailOtpUseCase = appContainer.signInWithEmailOtpUseCase
        )

        onboardingViewModel = OnboardingViewModel(
            getOnboardingStateUseCase = appContainer.getOnboardingStateUseCase,
            submitOnboardingUseCase = appContainer.submitOnboardingUseCase,
            navigationManager = appContainer.navigationManager
        )

        setContent {
            IshaaraTheme {
                val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()

                when (val state = authUiState.authState) {
                    is AuthState.Unknown -> {
                        SplashScreen()
                    }
                    is AuthState.Authenticated -> {
                        val activeUser = state.user
                        if (activeUser != null && activeUser.isOnboarded && activeUser.role != null) {
                            val userProfile = com.ishara.app.domain.model.UserProfile(
                                id = activeUser.id,
                                name = activeUser.name,
                                email = activeUser.email,
                                phoneNumber = activeUser.phoneNumber,
                                role = activeUser.role,
                                profileImageUrl = activeUser.profileImageUrl,
                                isOnboarded = true,
                                onboardingCompleted = true
                            )
                            when (activeUser.role) {
                                UserRole.USER, UserRole.ADMIN -> {
                                    StudentContainerScreen(
                                        appContainer = appContainer,
                                        userProfile = userProfile,
                                        onSignOut = {
                                            appContainer.clearDriverTripStateUseCase()
                                            authViewModel.signOut()
                                        }
                                    )
                                }
                                UserRole.DRIVER_CONDUCTOR -> {
                                    DriverContainerScreen(
                                        appContainer = appContainer,
                                        userProfile = userProfile,
                                        onSignOut = { authViewModel.signOut() }
                                    )
                                }
                            }
                        } else if (state.session.role != null && activeUser == null) {
                            // Offline or cached session with authoritative role
                            val offlineProfile = com.ishara.app.domain.model.UserProfile(
                                id = state.session.userId,
                                name = "User",
                                role = state.session.role,
                                isOnboarded = true,
                                onboardingCompleted = true
                            )
                            when (state.session.role) {
                                UserRole.USER, UserRole.ADMIN -> {
                                    StudentContainerScreen(
                                        appContainer = appContainer,
                                        userProfile = offlineProfile,
                                        onSignOut = {
                                            appContainer.clearDriverTripStateUseCase()
                                            authViewModel.signOut()
                                        }
                                    )
                                }
                                UserRole.DRIVER_CONDUCTOR -> {
                                    DriverContainerScreen(
                                        appContainer = appContainer,
                                        userProfile = offlineProfile,
                                        onSignOut = { authViewModel.signOut() }
                                    )
                                }
                            }
                        } else {
                            val onboardingUiState by onboardingViewModel.uiState.collectAsStateWithLifecycle()

                            when (val obState = onboardingUiState.onboardingState) {
                                is OnboardingState.Completed -> {
                                    when (val destination = obState.destination) {
                                        is ApplicationDestination.StudentHome -> {
                                            if (obState.userProfile.role == UserRole.DRIVER_CONDUCTOR) {
                                                DriverContainerScreen(
                                                    appContainer = appContainer,
                                                    userProfile = obState.userProfile,
                                                    onSignOut = { authViewModel.signOut() }
                                                )
                                            } else {
                                                StudentContainerScreen(
                                                    appContainer = appContainer,
                                                    userProfile = obState.userProfile,
                                                    onSignOut = { authViewModel.signOut() }
                                                )
                                            }
                                        }
                                        is ApplicationDestination.DriverDashboard -> {
                                            if (obState.userProfile.role != UserRole.DRIVER_CONDUCTOR) {
                                                SafeRecoveryScreen(
                                                    message = "Access denied: Driver permissions required.",
                                                    onRetryClick = { onboardingViewModel.retry() },
                                                    onSignOutClick = { authViewModel.signOut() }
                                                )
                                            } else {
                                                DriverContainerScreen(
                                                    appContainer = appContainer,
                                                    userProfile = obState.userProfile,
                                                    onSignOut = { authViewModel.signOut() }
                                                )
                                            }
                                        }
                                        is ApplicationDestination.SafeRecovery -> {
                                            SafeRecoveryScreen(
                                                message = destination.message,
                                                onRetryClick = { onboardingViewModel.retry() },
                                                onSignOutClick = { authViewModel.signOut() }
                                            )
                                        }
                                        else -> {
                                            RoleSelectionScreen(viewModel = onboardingViewModel)
                                        }
                                    }
                                }
                                else -> {
                                    // Onboarding required, in-progress, or error
                                    RoleSelectionScreen(viewModel = onboardingViewModel)
                                }
                            }
                        }
                    }
                    is AuthState.Unauthenticated,
                    is AuthState.SessionExpired,
                    is AuthState.Error -> {
                        LoginScreen(viewModel = authViewModel)
                    }
                }
            }
        }
    }
}

private sealed interface DriverScreenState {
    object Loading : DriverScreenState
    object OperatingTypeSelection : DriverScreenState
    object Onboarding : DriverScreenState
    object Verification : DriverScreenState
    object Agency : DriverScreenState
    object Readiness : DriverScreenState
    object Vehicle : DriverScreenState
    object Home : DriverScreenState
    object Trips : DriverScreenState
    data class TripDetail(val tripId: String) : DriverScreenState
    object Requests : DriverScreenState
    object Earnings : DriverScreenState
    data class Safety(val rideId: String) : DriverScreenState
    object Notifications : DriverScreenState
}

@Composable
private fun DriverContainerScreen(
    appContainer: AppContainer,
    userProfile: com.ishara.app.domain.model.UserProfile,
    onSignOut: () -> Unit
) {
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    var currentScreen by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf<DriverScreenState>(DriverScreenState.Loading)
    }

    val driverOnboardingViewModel = androidx.compose.runtime.remember(userProfile.id) {
        com.ishara.app.feature.driver.onboarding.DriverOnboardingViewModel(
            createDriverProfileUseCase = appContainer.createDriverProfileUseCase,
            dispatchers = appContainer.dispatchers
        )
    }

    val driverVerificationViewModel = androidx.compose.runtime.remember(userProfile.id) {
        com.ishara.app.feature.driver.verification.DriverVerificationViewModel(
            getDriverVerificationStatusUseCase = appContainer.getDriverVerificationStatusUseCase,
            submitDriverVerificationUseCase = appContainer.submitDriverVerificationUseCase,
            observeDriverProfileUseCase = appContainer.observeDriverProfileUseCase,
            observeDriverOnboardingStateUseCase = appContainer.observeDriverOnboardingStateUseCase,
            refreshDriverProfileUseCase = appContainer.refreshDriverProfileUseCase,
            dispatchers = appContainer.dispatchers
        )
    }

    val onboardingState by appContainer.observeDriverOnboardingStateUseCase().collectAsStateWithLifecycle()
    val driverProfileState by appContainer.observeDriverProfileStateUseCase().collectAsStateWithLifecycle()

    androidx.compose.runtime.LaunchedEffect(userProfile.id) {
        appContainer.getDriverProfileUseCase()
    }

    androidx.compose.runtime.LaunchedEffect(onboardingState, driverProfileState) {
        when {
            driverProfileState is com.ishara.app.domain.model.DriverProfileState.Loading -> {
                if (currentScreen != DriverScreenState.Onboarding && currentScreen != DriverScreenState.OperatingTypeSelection) {
                    currentScreen = DriverScreenState.Loading
                }
            }
            driverProfileState is com.ishara.app.domain.model.DriverProfileState.NeedsOperatingTypeSelection ||
            onboardingState is com.ishara.app.domain.model.DriverOnboardingState.NeedsOnboarding ||
            onboardingState is com.ishara.app.domain.model.DriverOnboardingState.NeedsOperatingTypeSelection -> {
                if (currentScreen != DriverScreenState.Onboarding) {
                    currentScreen = DriverScreenState.OperatingTypeSelection
                }
            }
            onboardingState is com.ishara.app.domain.model.DriverOnboardingState.PendingVerification ||
            onboardingState is com.ishara.app.domain.model.DriverOnboardingState.Rejected ||
            onboardingState is com.ishara.app.domain.model.DriverOnboardingState.Suspended ||
            onboardingState is com.ishara.app.domain.model.DriverOnboardingState.Verified -> {
                if (currentScreen == DriverScreenState.Loading ||
                    currentScreen == DriverScreenState.Onboarding ||
                    currentScreen == DriverScreenState.OperatingTypeSelection) {
                    currentScreen = DriverScreenState.Home
                }
            }
            driverProfileState is com.ishara.app.domain.model.DriverProfileState.Error ||
            onboardingState is com.ishara.app.domain.model.DriverOnboardingState.Error -> {
                // Keep currentScreen as Loading so the error card and retry button can be presented
                if (currentScreen != DriverScreenState.Onboarding && currentScreen != DriverScreenState.OperatingTypeSelection) {
                    currentScreen = DriverScreenState.Loading
                }
            }
            else -> {}
        }
    }

    val handleSignOut: () -> Unit = {
        appContainer.clearDriverStateUseCase()
        appContainer.clearAgencyMembershipStateUseCase()
        appContainer.clearDriverReadinessStateUseCase()
        appContainer.clearVehicleStateUseCase()
        appContainer.clearDriverTripStateUseCase()
        onSignOut()
    }

    androidx.compose.runtime.LaunchedEffect(appContainer.navigationManager) {
        appContainer.navigationManager.commands.collect { command ->
            when (command) {
                is com.ishara.app.navigation.NavigationCommand.NavigateTo -> {
                    if (command.route == "driver/notifications" || command.route == com.ishara.app.navigation.IshaaraDestination.DriverNotifications.route) {
                        currentScreen = DriverScreenState.Notifications
                    } else if (command.route == "driver/earnings" || command.route == com.ishara.app.navigation.IshaaraDestination.DriverEarnings.route) {
                        currentScreen = DriverScreenState.Earnings
                    }
                }
                else -> Unit
            }
        }
    }

    val canGoBackToHome = currentScreen != DriverScreenState.Home &&
            currentScreen != DriverScreenState.Loading &&
            currentScreen != DriverScreenState.OperatingTypeSelection &&
            currentScreen != DriverScreenState.Onboarding

    androidx.activity.compose.BackHandler(enabled = canGoBackToHome) {
        if (currentScreen is DriverScreenState.TripDetail) {
            currentScreen = DriverScreenState.Trips
        } else {
            currentScreen = DriverScreenState.Home
        }
    }

    when (val screen = currentScreen) {
        is DriverScreenState.Loading -> {
            val error = (onboardingState as? com.ishara.app.domain.model.DriverOnboardingState.Error)?.error
            androidx.compose.foundation.layout.Column(
                modifier = androidx.compose.ui.Modifier
                    .fillMaxSize()
                    .background(com.ishara.app.core.designsystem.theme.IshaaraTheme.colors.background)
                    .padding(24.dp),
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
            ) {
                if (error != null) {
                    androidx.compose.material3.Text(
                        text = "Unable to load driver profile",
                        style = com.ishara.app.core.designsystem.theme.IshaaraTheme.typography.titleMedium,
                        color = com.ishara.app.core.designsystem.theme.IshaaraTheme.colors.danger,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
                    androidx.compose.material3.Text(
                        text = error.message.ifEmpty { "Please check your network connection and try again." },
                        style = com.ishara.app.core.designsystem.theme.IshaaraTheme.typography.bodySmall,
                        color = com.ishara.app.core.designsystem.theme.IshaaraTheme.colors.foregroundMuted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(24.dp))
                    com.ishara.app.core.designsystem.component.IshaaraButton(
                        text = "Retry",
                        onClick = {
                            coroutineScope.launch {
                                appContainer.getDriverProfileUseCase()
                            }
                        },
                        variant = com.ishara.app.core.designsystem.component.IshaaraButtonVariant.Primary,
                        modifier = androidx.compose.ui.Modifier.fillMaxWidth()
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(12.dp))
                    com.ishara.app.core.designsystem.component.IshaaraButton(
                        text = "Sign Out",
                        onClick = handleSignOut,
                        variant = com.ishara.app.core.designsystem.component.IshaaraButtonVariant.Outlined,
                        modifier = androidx.compose.ui.Modifier.fillMaxWidth()
                    )
                } else {
                    androidx.compose.material3.CircularProgressIndicator(
                        color = com.ishara.app.core.designsystem.theme.IshaaraTheme.colors.primary,
                        modifier = androidx.compose.ui.Modifier.size(40.dp)
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))
                    androidx.compose.material3.Text(
                        text = "Loading driver profile...",
                        style = com.ishara.app.core.designsystem.theme.IshaaraTheme.typography.bodyMedium,
                        color = com.ishara.app.core.designsystem.theme.IshaaraTheme.colors.foregroundMuted
                    )
                }
            }
        }
        is DriverScreenState.OperatingTypeSelection -> {
            com.ishara.app.feature.driver.onboarding.ui.DriverOperatingTypeScreen(
                viewModel = driverOnboardingViewModel,
                onSignOut = handleSignOut,
                onContinue = {
                    currentScreen = DriverScreenState.Onboarding
                }
            )
        }
        is DriverScreenState.Onboarding -> {
            com.ishara.app.feature.driver.onboarding.ui.DriverOnboardingScreen(
                viewModel = driverOnboardingViewModel,
                onSignOut = handleSignOut,
                onNavigateBack = {
                    currentScreen = DriverScreenState.OperatingTypeSelection
                },
                onOnboardingComplete = {
                    currentScreen = DriverScreenState.Verification
                }
            )
        }
        is DriverScreenState.Verification -> {
            com.ishara.app.feature.driver.verification.ui.DriverVerificationScreen(
                viewModel = driverVerificationViewModel,
                onSignOut = handleSignOut,
                onNavigateToHome = {
                    currentScreen = DriverScreenState.Home
                },
                onNavigateToOnboarding = {
                    currentScreen = DriverScreenState.Onboarding
                },
                onNavigateToAgency = {
                    currentScreen = DriverScreenState.Agency
                },
                onNavigateBack = {
                    currentScreen = DriverScreenState.Home
                }
            )
        }
        is DriverScreenState.Home -> {
            val driverViewModel = androidx.compose.runtime.remember(userProfile.id) {
                com.ishara.app.feature.driver.DriverViewModel(
                    driverName = userProfile.name,
                    getDriverOperationalContextUseCase = appContainer.getDriverOperationalContextUseCase,
                    setDriverAvailabilityUseCase = appContainer.setDriverAvailabilityUseCase,
                    manageDriverTripLifecycleUseCase = appContainer.manageDriverTripLifecycleUseCase,
                    navigationManager = appContainer.navigationManager,
                    startDriverTrackingUseCase = appContainer.startDriverTrackingUseCase,
                    stopDriverTrackingUseCase = appContainer.stopDriverTrackingUseCase,
                    observeDriverTrackingStatusUseCase = appContainer.observeDriverTrackingStatusUseCase,
                    getDriverRatingSummaryUseCase = appContainer.getDriverRatingSummaryUseCase,
                    observeAgencyMembershipStateUseCase = appContainer.observeAgencyMembershipStateUseCase,
                    observeDriverProfileUseCase = appContainer.observeDriverProfileUseCase,
                    refreshDriverProfileUseCase = appContainer.refreshDriverProfileUseCase,
                    refreshAgencyMembershipUseCase = appContainer.refreshAgencyMembershipUseCase,
                    dispatchers = appContainer.dispatchers
                )
            }
            com.ishara.app.feature.driver.ui.DriverHomeScreen(
                viewModel = driverViewModel,
                onSignOut = handleSignOut,
                onNavigateToRequests = { currentScreen = DriverScreenState.Requests },
                onNavigateToEarnings = { currentScreen = DriverScreenState.Earnings },
                onNavigateToVerification = { currentScreen = DriverScreenState.Verification },
                onNavigateToAgency = { currentScreen = DriverScreenState.Agency },
                onNavigateToReadiness = { currentScreen = DriverScreenState.Readiness },
                onNavigateToVehicle = { currentScreen = DriverScreenState.Vehicle },
                onNavigateToTrips = { currentScreen = DriverScreenState.Trips },
                onNavigateToTripDetail = { tripId ->
                    currentScreen = DriverScreenState.TripDetail(tripId)
                }
            )
        }
        is DriverScreenState.Trips -> {
            val driverTripListViewModel = androidx.compose.runtime.remember(userProfile.id) {
                com.ishara.app.feature.driver.trip.DriverTripListViewModel(
                    getDriverTripsUseCase = appContainer.getDriverTripsUseCase,
                    startDriverTripUseCase = appContainer.startDriverTripUseCase,
                    completeDriverTripUseCase = appContainer.completeDriverTripUseCase,
                    cancelDriverTripUseCase = appContainer.cancelDriverTripUseCase,
                    createDriverTripUseCase = appContainer.createDriverTripUseCase,
                    observeDriverTripsUseCase = appContainer.observeDriverTripsUseCase,
                    observeActiveTripUseCase = appContainer.observeActiveTripUseCase,
                    getAssignedVehicleUseCase = appContainer.getAssignedVehicleUseCase,
                    dispatchers = appContainer.dispatchers
                )
            }
            com.ishara.app.feature.driver.trip.ui.DriverTripListScreen(
                viewModel = driverTripListViewModel,
                onNavigateBack = { currentScreen = DriverScreenState.Home },
                onNavigateToTripDetail = { tripId ->
                    currentScreen = DriverScreenState.TripDetail(tripId)
                }
            )
        }
        is DriverScreenState.TripDetail -> {
            val driverTripDetailViewModel = androidx.compose.runtime.remember(screen.tripId) {
                com.ishara.app.feature.driver.trip.DriverTripDetailViewModel(
                    tripId = screen.tripId,
                    getTripDetailsUseCase = appContainer.getTripDetailsUseCase,
                    startDriverTripUseCase = appContainer.startDriverTripUseCase,
                    completeDriverTripUseCase = appContainer.completeDriverTripUseCase,
                    cancelDriverTripUseCase = appContainer.cancelDriverTripUseCase,
                    getVehicleDetailsUseCase = appContainer.getVehicleDetailsUseCase,
                    dispatchers = appContainer.dispatchers
                )
            }
            com.ishara.app.feature.driver.trip.ui.DriverTripDetailScreen(
                viewModel = driverTripDetailViewModel,
                onNavigateBack = { currentScreen = DriverScreenState.Trips }
            )
        }
        is DriverScreenState.Agency -> {
            val driverAgencyViewModel = androidx.compose.runtime.remember(userProfile.id) {
                com.ishara.app.feature.driver.agency.DriverAgencyMembershipViewModel(
                    getCurrentAgencyMembershipUseCase = appContainer.getCurrentAgencyMembershipUseCase,
                    observeAgencyMembershipStateUseCase = appContainer.observeAgencyMembershipStateUseCase,
                    observeCurrentAgencyMembershipUseCase = appContainer.observeCurrentAgencyMembershipUseCase,
                    listAgenciesUseCase = appContainer.listAgenciesUseCase,
                    requestAgencyMembershipUseCase = appContainer.requestAgencyMembershipUseCase,
                    cancelAgencyMembershipUseCase = appContainer.cancelAgencyMembershipUseCase,
                    refreshAgencyMembershipUseCase = appContainer.refreshAgencyMembershipUseCase,
                    dispatchers = appContainer.dispatchers
                )
            }
            com.ishara.app.feature.driver.agency.ui.DriverAgencyMembershipScreen(
                viewModel = driverAgencyViewModel,
                onNavigateBack = {
                    currentScreen = DriverScreenState.Home
                }
            )
        }
        is DriverScreenState.Readiness -> {
            val driverReadinessViewModel = androidx.compose.runtime.remember(userProfile.id) {
                com.ishara.app.feature.driver.readiness.DriverOperationalReadinessViewModel(
                    getDriverReadinessUseCase = appContainer.getDriverReadinessUseCase,
                    refreshDriverReadinessUseCase = appContainer.refreshDriverReadinessUseCase,
                    observeDriverReadinessUseCase = appContainer.observeDriverReadinessUseCase,
                    dispatchers = appContainer.dispatchers
                )
            }
            com.ishara.app.feature.driver.readiness.ui.DriverOperationalReadinessScreen(
                viewModel = driverReadinessViewModel,
                onNavigateBack = { currentScreen = DriverScreenState.Home },
                onNavigateToVerification = { currentScreen = DriverScreenState.Verification },
                onNavigateToAgency = { currentScreen = DriverScreenState.Agency },
                onNavigateToVehicle = { currentScreen = DriverScreenState.Vehicle }
            )
        }
        is DriverScreenState.Vehicle -> {
            val driverVehicleViewModel = androidx.compose.runtime.remember(userProfile.id) {
                com.ishara.app.feature.driver.vehicle.DriverVehicleViewModel(
                    getAssignedVehicleUseCase = appContainer.getAssignedVehicleUseCase,
                    refreshAssignedVehicleUseCase = appContainer.refreshAssignedVehicleUseCase,
                    observeAssignedVehicleUseCase = appContainer.observeAssignedVehicleUseCase,
                    listMyVehiclesUseCase = appContainer.listMyVehiclesUseCase,
                    registerVehicleUseCase = appContainer.registerVehicleUseCase,
                    assignSelfToVehicleUseCase = appContainer.assignSelfToVehicleUseCase,
                    unassignVehicleUseCase = appContainer.unassignVehicleUseCase,
                    getVehicleAssignmentHistoryUseCase = appContainer.getVehicleAssignmentHistoryUseCase,
                    dispatchers = appContainer.dispatchers
                )
            }
            com.ishara.app.feature.driver.vehicle.ui.DriverVehicleScreen(
                viewModel = driverVehicleViewModel,
                onNavigateBack = { currentScreen = DriverScreenState.Home }
            )
        }
        is DriverScreenState.Requests -> {
            val driverRideRequestsViewModel = androidx.compose.runtime.remember(userProfile.id) {
                com.ishara.app.feature.driver.requests.DriverRideRequestsViewModel(
                    getDriverRideRequestsUseCase = appContainer.getDriverRideRequestsUseCase,
                    acceptRideRequestUseCase = appContainer.acceptRideRequestUseCase,
                    rejectRideRequestUseCase = appContainer.rejectRideRequestUseCase,
                    managePassengerBoardingUseCase = appContainer.managePassengerBoardingUseCase,
                    repository = appContainer.driverRideRequestRepository,
                    navigationManager = appContainer.navigationManager,
                    dispatchers = appContainer.dispatchers
                )
            }
            com.ishara.app.feature.driver.requests.DriverRideRequestsScreen(
                viewModel = driverRideRequestsViewModel,
                onNavigateBack = { currentScreen = DriverScreenState.Home }
            )
        }
        is DriverScreenState.Earnings -> {
            val driverEarningsViewModel = androidx.compose.runtime.remember(userProfile.id) {
                com.ishara.app.feature.driver.earnings.DriverEarningsViewModel(
                    getDriverEarningsUseCase = appContainer.getDriverEarningsUseCase,
                    refreshDriverEarningsUseCase = appContainer.refreshDriverEarningsUseCase,
                    dispatchers = appContainer.dispatchers
                )
            }
            com.ishara.app.feature.driver.earnings.DriverEarningsScreen(
                viewModel = driverEarningsViewModel,
                onNavigateBack = { currentScreen = DriverScreenState.Home }
            )
        }
        is DriverScreenState.Safety -> {
            val safetyViewModel = androidx.compose.runtime.remember(screen.rideId) {
                com.ishara.app.feature.safety.SafetyViewModel(
                    rideId = screen.rideId,
                    triggerSosUseCase = appContainer.triggerSosUseCase,
                    getActiveSosUseCase = appContainer.getActiveSosUseCase,
                    cancelSosUseCase = appContainer.cancelSosUseCase,
                    getEmergencyContactsUseCase = appContainer.getEmergencyContactsUseCase,
                    createEmergencyContactUseCase = appContainer.createEmergencyContactUseCase,
                    deleteEmergencyContactUseCase = appContainer.deleteEmergencyContactUseCase,
                    navigationManager = appContainer.navigationManager,
                    dispatchers = appContainer.dispatchers
                )
            }
            com.ishara.app.feature.safety.SafetyScreen(
                viewModel = safetyViewModel,
                onNavigateBack = { currentScreen = DriverScreenState.Home }
            )
        }
        is DriverScreenState.Notifications -> {
            val driverNotificationViewModel = androidx.compose.runtime.remember(userProfile.id) {
                com.ishara.app.feature.notifications.NotificationViewModel(
                    getNotificationsUseCase = appContainer.getNotificationsUseCase,
                    getUnreadNotificationCountUseCase = appContainer.getUnreadNotificationCountUseCase,
                    markNotificationAsReadUseCase = appContainer.markNotificationAsReadUseCase,
                    markAllNotificationsAsReadUseCase = appContainer.markAllNotificationsAsReadUseCase,
                    notificationRouter = appContainer.notificationRouter,
                    navigationManager = appContainer.navigationManager,
                    dispatchers = appContainer.dispatchers
                )
            }
            com.ishara.app.feature.notifications.ui.NotificationInboxScreen(
                viewModel = driverNotificationViewModel,
                userRole = com.ishara.app.domain.model.UserRole.DRIVER_CONDUCTOR,
                onNavigateBack = { currentScreen = DriverScreenState.Home }
            )
        }
    }
}

private sealed interface StudentScreenState {
    object Home : StudentScreenState
    object Search : StudentScreenState
    object VoiceTrip : StudentScreenState
    object Profile : StudentScreenState
    data class Discovery(val query: com.ishara.app.domain.model.DiscoveryQuery) : StudentScreenState
    data class RideRequest(
        val trip: com.ishara.app.domain.model.DiscoveredTrip,
        val pickupAddress: String,
        val destinationAddress: String,
        val pickupLatitude: Double,
        val pickupLongitude: Double,
        val destinationLatitude: Double,
        val destinationLongitude: Double
    ) : StudentScreenState
    data class RideRequestStatus(val requestId: String) : StudentScreenState
    data class Payment(
        val rideId: String,
        val pickupAddress: String? = null,
        val destinationAddress: String? = null
    ) : StudentScreenState
    data class RideTracking(val rideId: String) : StudentScreenState
    data class FareSummary(val rideId: String) : StudentScreenState
    object TransitRoutes : StudentScreenState
    data class RouteDetails(val routeId: String) : StudentScreenState
    data class Safety(val rideId: String) : StudentScreenState
    data class Rating(val rideId: String) : StudentScreenState
    object Notifications : StudentScreenState
}

@Composable
private fun StudentContainerScreen(
    appContainer: AppContainer,
    userProfile: com.ishara.app.domain.model.UserProfile,
    onSignOut: () -> Unit
) {
    var currentScreen by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf<StudentScreenState>(StudentScreenState.Home)
    }

    val studentHomeViewModel = androidx.compose.runtime.remember(userProfile.id) {
        com.ishara.app.feature.student.home.StudentHomeViewModel(
            userName = userProfile.name,
            resolveCurrentLocationUseCase = appContainer.resolveCurrentLocationUseCase,
            manageRecentDestinationsUseCase = appContainer.manageRecentDestinationsUseCase,
            getActiveRideUseCase = appContainer.getActiveRideUseCase,
            navigationManager = appContainer.navigationManager,
            dispatchers = appContainer.dispatchers
        )
    }

    val locationSearchViewModel = androidx.compose.runtime.remember(userProfile.id) {
        com.ishara.app.feature.student.search.LocationSearchViewModel(
            searchLocationsUseCase = appContainer.searchLocationsUseCase,
            manageRecentDestinationsUseCase = appContainer.manageRecentDestinationsUseCase,
            navigationManager = appContainer.navigationManager,
            dispatchers = appContainer.dispatchers
        )
    }

    val voiceTripViewModel = androidx.compose.runtime.remember(userProfile.id) {
        com.ishara.app.feature.student.voice.VoiceTripViewModel(
            voiceInputClient = appContainer.voiceInputClient,
            createVoiceTripDraftUseCase = appContainer.createVoiceTripDraftUseCase,
            cancelVoiceTripDraftUseCase = appContainer.cancelVoiceTripDraftUseCase,
            searchLocationsUseCase = appContainer.searchLocationsUseCase,
            navigationManager = appContainer.navigationManager,
            dispatchers = appContainer.dispatchers
        )
    }

    val profileViewModel = androidx.compose.runtime.remember(userProfile.id) {
        com.ishara.app.feature.student.profile.ProfileViewModel(
            initialProfile = userProfile,
            getCurrentUserProfileUseCase = appContainer.getCurrentUserProfileUseCase,
            updateUserProfileUseCase = appContainer.updateUserProfileUseCase,
            userRepository = appContainer.userRepository,
            navigationManager = appContainer.navigationManager,
            dispatchers = appContainer.dispatchers
        )
    }

    val studentNotificationViewModel = androidx.compose.runtime.remember(userProfile.id) {
        com.ishara.app.feature.notifications.NotificationViewModel(
            getNotificationsUseCase = appContainer.getNotificationsUseCase,
            getUnreadNotificationCountUseCase = appContainer.getUnreadNotificationCountUseCase,
            markNotificationAsReadUseCase = appContainer.markNotificationAsReadUseCase,
            markAllNotificationsAsReadUseCase = appContainer.markAllNotificationsAsReadUseCase,
            notificationRouter = appContainer.notificationRouter,
            navigationManager = appContainer.navigationManager,
            dispatchers = appContainer.dispatchers
        )
    }

    androidx.activity.compose.BackHandler(enabled = currentScreen != StudentScreenState.Home) {
        if (currentScreen is StudentScreenState.RouteDetails) {
            currentScreen = StudentScreenState.TransitRoutes
        } else if (currentScreen is StudentScreenState.Safety) {
            currentScreen = StudentScreenState.RideTracking((currentScreen as StudentScreenState.Safety).rideId)
        } else if (currentScreen is StudentScreenState.FareSummary) {
            currentScreen = StudentScreenState.RideTracking((currentScreen as StudentScreenState.FareSummary).rideId)
        } else {
            currentScreen = StudentScreenState.Home
        }
    }

    when (val screen = currentScreen) {
        is StudentScreenState.Home -> {
            com.ishara.app.feature.student.home.StudentHomeScreen(
                viewModel = studentHomeViewModel,
                onProfileClick = { currentScreen = StudentScreenState.Profile }
            )
        }
        is StudentScreenState.Profile -> {
            com.ishara.app.feature.student.profile.StudentProfileScreen(
                viewModel = profileViewModel,
                onNavigateBack = { currentScreen = StudentScreenState.Home },
                onSignOut = onSignOut
            )
        }
        is StudentScreenState.VoiceTrip -> {
            com.ishara.app.feature.student.voice.VoiceTripScreen(
                viewModel = voiceTripViewModel,
                onConfirmed = { discoveryQuery ->
                    val destination = com.ishara.app.domain.model.StudentDestination(
                        name = discoveryQuery.destinationName ?: "Destination",
                        formattedAddress = discoveryQuery.destinationAddress ?: "",
                        coordinates = com.ishara.app.core.location.LocationCoordinates(
                            latitude = discoveryQuery.destinationLatitude,
                            longitude = discoveryQuery.destinationLongitude
                        )
                    )
                    studentHomeViewModel.onDestinationSelected(destination)
                    currentScreen = StudentScreenState.Discovery(discoveryQuery)
                },
                onNavigateBack = { currentScreen = StudentScreenState.Home }
            )
        }
        is StudentScreenState.Search -> {
            com.ishara.app.feature.student.search.LocationSearchScreen(
                viewModel = locationSearchViewModel,
                onDestinationSelected = { destination ->
                    studentHomeViewModel.onDestinationSelected(destination)
                    currentScreen = StudentScreenState.Home
                }
            )
        }
        is StudentScreenState.Discovery -> {
            val discoveryViewModel = androidx.compose.runtime.remember(screen.query) {
                com.ishara.app.feature.student.discovery.DiscoveryViewModel(
                    initialQuery = screen.query,
                    discoverTripsUseCase = appContainer.discoverTripsUseCase,
                    getPassengerTripDetailsUseCase = appContainer.getPassengerTripDetailsUseCase,
                    navigationManager = appContainer.navigationManager,
                    dispatchers = appContainer.dispatchers
                )
            }
            com.ishara.app.feature.student.discovery.DiscoveryScreen(
                viewModel = discoveryViewModel,
                onNavigateToRideRequest = { trip ->
                    currentScreen = StudentScreenState.RideRequest(
                        trip = trip,
                        pickupAddress = trip.originAddress,
                        destinationAddress = trip.destinationAddress,
                        pickupLatitude = trip.originCoordinate.latitude,
                        pickupLongitude = trip.originCoordinate.longitude,
                        destinationLatitude = trip.destinationCoordinate.latitude,
                        destinationLongitude = trip.destinationCoordinate.longitude
                    )
                }
            )
        }
        is StudentScreenState.RideRequest -> {
            val rideRequestViewModel = androidx.compose.runtime.remember(screen.trip.tripId) {
                com.ishara.app.feature.student.riderequest.RideRequestViewModel(
                    trip = screen.trip,
                    pickupAddress = screen.pickupAddress,
                    destinationAddress = screen.destinationAddress,
                    pickupLatitude = screen.pickupLatitude,
                    pickupLongitude = screen.pickupLongitude,
                    destinationLatitude = screen.destinationLatitude,
                    destinationLongitude = screen.destinationLongitude,
                    createRideRequestUseCase = appContainer.createRideRequestUseCase,
                    navigationManager = appContainer.navigationManager,
                    dispatchers = appContainer.dispatchers
                )
            }
            com.ishara.app.feature.student.riderequest.RideRequestReviewScreen(
                viewModel = rideRequestViewModel
            )
        }
        is StudentScreenState.RideRequestStatus -> {
            val statusViewModel = androidx.compose.runtime.remember(screen.requestId) {
                com.ishara.app.feature.student.riderequest.RideRequestStatusViewModel(
                    requestId = screen.requestId,
                    getRideRequestUseCase = appContainer.getRideRequestUseCase,
                    cancelRideRequestUseCase = appContainer.cancelRideRequestUseCase,
                    navigationManager = appContainer.navigationManager,
                    dispatchers = appContainer.dispatchers
                )
            }
            com.ishara.app.feature.student.riderequest.RideRequestStatusScreen(
                viewModel = statusViewModel
            )
        }
        is StudentScreenState.Payment -> {
            val paymentViewModel = androidx.compose.runtime.remember(screen.rideId) {
                com.ishara.app.feature.student.payment.PaymentViewModel(
                    rideId = screen.rideId,
                    pickupAddress = screen.pickupAddress,
                    destinationAddress = screen.destinationAddress,
                    getRidePaymentUseCase = appContainer.getRidePaymentUseCase,
                    createPaymentOrderUseCase = appContainer.createPaymentOrderUseCase,
                    verifyPaymentUseCase = appContainer.verifyPaymentUseCase,
                    paymentLauncher = appContainer.paymentLauncher,
                    navigationManager = appContainer.navigationManager,
                    dispatchers = appContainer.dispatchers
                )
            }
            com.ishara.app.feature.student.payment.PaymentScreen(
                viewModel = paymentViewModel,
                onNavigateBack = { currentScreen = StudentScreenState.Home }
            )
        }
        is StudentScreenState.RideTracking -> {
            val trackingViewModel = androidx.compose.runtime.remember(screen.rideId) {
                com.ishara.app.feature.student.tracking.RideTrackingViewModel(
                    rideId = screen.rideId,
                    getRideTrackingUseCase = appContainer.getRideTrackingUseCase,
                    getDriverLocationUseCase = appContainer.getDriverLocationUseCase,
                    observeRideTrackingUseCase = appContainer.observeRideTrackingUseCase,
                    navigationManager = appContainer.navigationManager,
                    locationRepository = appContainer.locationRepository,
                    dispatchers = appContainer.dispatchers
                )
            }
            com.ishara.app.feature.student.tracking.RideTrackingScreen(
                viewModel = trackingViewModel,
                onNavigateBack = { currentScreen = StudentScreenState.Home },
                onRateTripClick = { currentScreen = StudentScreenState.Rating(screen.rideId) }
            )
        }
        is StudentScreenState.FareSummary -> {
            val fareViewModel = androidx.compose.runtime.remember(screen.rideId) {
                com.ishara.app.feature.student.fare.FareSummaryViewModel(
                    rideId = screen.rideId,
                    getRideFareUseCase = appContainer.getRideFareUseCase,
                    navigationManager = appContainer.navigationManager,
                    dispatchers = appContainer.dispatchers
                )
            }
            com.ishara.app.feature.student.fare.FareSummaryScreen(
                viewModel = fareViewModel,
                onNavigateBack = { currentScreen = StudentScreenState.RideTracking(screen.rideId) }
            )
        }
        is StudentScreenState.TransitRoutes -> {
            val transitViewModel = androidx.compose.runtime.remember {
                com.ishara.app.feature.student.transit.TransitRoutesViewModel(
                    getTransitRoutesUseCase = appContainer.getTransitRoutesUseCase,
                    navigationManager = appContainer.navigationManager,
                    dispatchers = appContainer.dispatchers
                )
            }
            com.ishara.app.feature.student.transit.TransitRoutesScreen(
                viewModel = transitViewModel,
                onNavigateBack = { currentScreen = StudentScreenState.Home }
            )
        }
        is StudentScreenState.RouteDetails -> {
            val routeDetailsViewModel = androidx.compose.runtime.remember(screen.routeId) {
                com.ishara.app.feature.student.transit.RouteDetailsViewModel(
                    routeId = screen.routeId,
                    getRouteDetailsUseCase = appContainer.getRouteDetailsUseCase,
                    navigationManager = appContainer.navigationManager,
                    dispatchers = appContainer.dispatchers
                )
            }
            com.ishara.app.feature.student.transit.RouteDetailsScreen(
                viewModel = routeDetailsViewModel,
                onNavigateBack = { currentScreen = StudentScreenState.TransitRoutes }
            )
        }
        is StudentScreenState.Safety -> {
            val safetyViewModel = androidx.compose.runtime.remember(screen.rideId) {
                com.ishara.app.feature.safety.SafetyViewModel(
                    rideId = screen.rideId,
                    triggerSosUseCase = appContainer.triggerSosUseCase,
                    getActiveSosUseCase = appContainer.getActiveSosUseCase,
                    cancelSosUseCase = appContainer.cancelSosUseCase,
                    getEmergencyContactsUseCase = appContainer.getEmergencyContactsUseCase,
                    createEmergencyContactUseCase = appContainer.createEmergencyContactUseCase,
                    deleteEmergencyContactUseCase = appContainer.deleteEmergencyContactUseCase,
                    navigationManager = appContainer.navigationManager,
                    dispatchers = appContainer.dispatchers
                )
            }
            com.ishara.app.feature.safety.SafetyScreen(
                viewModel = safetyViewModel,
                onNavigateBack = { currentScreen = StudentScreenState.RideTracking(screen.rideId) }
            )
        }
        is StudentScreenState.Rating -> {
            val ratingViewModel = androidx.compose.runtime.remember(screen.rideId) {
                com.ishara.app.feature.rating.RatingViewModel(
                    rideId = screen.rideId,
                    checkRatingEligibilityUseCase = appContainer.checkRatingEligibilityUseCase,
                    submitRatingUseCase = appContainer.submitRatingUseCase,
                    dispatchers = appContainer.dispatchers
                )
            }
            com.ishara.app.feature.rating.ui.RatingScreen(
                viewModel = ratingViewModel,
                onNavigateBack = { currentScreen = StudentScreenState.Home }
            )
        }
        is StudentScreenState.Notifications -> {
            com.ishara.app.feature.notifications.ui.NotificationInboxScreen(
                viewModel = studentNotificationViewModel,
                userRole = com.ishara.app.domain.model.UserRole.USER,
                onNavigateBack = { currentScreen = StudentScreenState.Home }
            )
        }
    }

    // Observe decoupled navigation commands to open search, discovery, tracking, or home
    androidx.compose.runtime.LaunchedEffect(appContainer.navigationManager) {
        appContainer.navigationManager.commands.collect { command: com.ishara.app.navigation.NavigationCommand ->
            when (command) {
                is com.ishara.app.navigation.NavigationCommand.NavigateTo -> {
                    if (command.route == com.ishara.app.navigation.IshaaraDestination.StudentSearch.route) {
                        currentScreen = StudentScreenState.Search
                    } else if (command.route == com.ishara.app.navigation.IshaaraDestination.StudentVoiceTrip.route) {
                        currentScreen = StudentScreenState.VoiceTrip
                    } else if (command.route == com.ishara.app.navigation.IshaaraDestination.StudentTransitRoutes.route) {
                        currentScreen = StudentScreenState.TransitRoutes
                    } else if (command.route.startsWith("student/transit/routes/")) {
                        val routeId = command.route.substringAfter("student/transit/routes/")
                        currentScreen = StudentScreenState.RouteDetails(routeId)
                    } else if (command.route.startsWith("student/safety/")) {
                        val rideId = command.route.substringAfter("student/safety/")
                        currentScreen = StudentScreenState.Safety(rideId)
                    } else if (command.route == com.ishara.app.navigation.IshaaraDestination.StudentHome.route) {
                        currentScreen = StudentScreenState.Home
                    } else if (command.route == com.ishara.app.navigation.IshaaraDestination.StudentProfile.route) {
                        currentScreen = StudentScreenState.Profile
                    } else if (command.route == "student/notifications" || command.route == com.ishara.app.navigation.IshaaraDestination.StudentNotifications.route) {
                        currentScreen = StudentScreenState.Notifications
                    } else if (command.route.startsWith("student/ride/") && command.route.endsWith("/fare")) {
                        val rideId = command.route.removePrefix("student/ride/").removeSuffix("/fare")
                        currentScreen = StudentScreenState.FareSummary(rideId)
                    } else if (command.route.startsWith("student/ride/") && command.route.endsWith("/payment")) {
                        val rideId = command.route.removePrefix("student/ride/").removeSuffix("/payment")
                        currentScreen = StudentScreenState.Payment(rideId)
                    } else if (command.route.startsWith("student/ride/request/status/")) {
                        val requestId = command.route.substringAfter("student/ride/request/status/")
                        currentScreen = StudentScreenState.RideRequestStatus(requestId)
                    } else if (command.route.startsWith("student/ride/") && !command.route.endsWith("request")) {
                        val rideId = command.route.substringAfter("student/ride/")
                        currentScreen = StudentScreenState.RideTracking(rideId)
                    } else if (command.route == com.ishara.app.navigation.IshaaraDestination.StudentDiscovery.route) {
                        val homeState = studentHomeViewModel.uiState.value
                        val destination = homeState.selectedDestination
                        val locationDisplay = (homeState.locationState as? com.ishara.app.feature.student.home.LocationUiState.Available)?.location
                        if (destination != null) {
                            val query = com.ishara.app.domain.model.DiscoveryQuery(
                                originLatitude = locationDisplay?.coordinates?.latitude ?: 25.4484,
                                originLongitude = locationDisplay?.coordinates?.longitude ?: 78.5685,
                                originName = locationDisplay?.title ?: "Current Location",
                                originAddress = locationDisplay?.subtitle ?: "Current Location",
                                destinationLatitude = destination.coordinates.latitude,
                                destinationLongitude = destination.coordinates.longitude,
                                destinationName = destination.name,
                                destinationAddress = destination.formattedAddress,
                                maxPickupDistanceMeters = com.ishara.app.domain.model.DiscoveryQuery.DEFAULT_PICKUP_RADIUS_METERS,
                                maxDestinationDeviationMeters = com.ishara.app.domain.model.DiscoveryQuery.DEFAULT_DESTINATION_RADIUS_METERS
                            )
                            currentScreen = StudentScreenState.Discovery(query)
                        }
                    }
                }
                is com.ishara.app.navigation.NavigationCommand.NavigateUp -> {
                    currentScreen = StudentScreenState.Home
                }
                else -> Unit
            }
        }
    }
}

/**
 * Authenticated landing container demonstrating role-aware UI and clean session logout.
 */
@Composable
private fun AuthenticatedRootScreen(
    role: UserRole,
    userName: String,
    userId: String,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    val isDriver = role == UserRole.DRIVER_CONDUCTOR

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(spacing.md),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(spacing.lg))

                IshaaraStatusChip(
                    status = if (isDriver) IshaaraTransitStatus.ONLINE else IshaaraTransitStatus.AVAILABLE
                )

                Spacer(modifier = Modifier.height(spacing.md))

                Text(
                    text = if (isDriver) "Driver Workspace" else "Student Transit",
                    style = typography.headlineLarge,
                    color = colors.foreground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(spacing.xs))

                Text(
                    text = "Signed in as $userName",
                    style = typography.bodyMedium,
                    color = colors.foregroundMuted,
                    textAlign = TextAlign.Center
                )
            }

            // Role Context Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface, shape = IshaaraTheme.shapes.md)
                    .padding(spacing.md),
                horizontalAlignment = Alignment.Start
            ) {
                IshaaraBadge(
                    text = if (isDriver) "ROLE: DRIVER / CONDUCTOR" else "ROLE: STUDENT / PASSENGER"
                )

                Spacer(modifier = Modifier.height(spacing.sm))

                Text(
                    text = if (isDriver) "Ready to start trip or manage passenger boarding." else "Ready to find and request transit rides.",
                    style = typography.bodyLarge,
                    color = colors.foreground
                )

                Spacer(modifier = Modifier.height(spacing.md))

                IshaaraPrimaryAction(
                    text = if (isDriver) "Go online" else "Search destination",
                    onClick = { /* Ready for Phase 05 role workflows */ }
                )
            }

            // Bottom Actions: Secure Sign Out
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = spacing.md),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IshaaraButton(
                    text = "Sign out",
                    onClick = onSignOut,
                    variant = IshaaraButtonVariant.Outlined,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(spacing.xs))

                Text(
                    text = "Session active with Bearer token authentication.",
                    style = typography.labelSmall,
                    color = colors.foregroundSubtle,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}