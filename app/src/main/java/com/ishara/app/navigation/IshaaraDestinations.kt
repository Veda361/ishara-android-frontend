package com.ishara.app.navigation

/**
 * Type-safe destination routes for Ishaara navigation.
 * Structures the application into Auth, Student, and Driver sub-graphs.
 */
sealed class IshaaraDestination(val route: String) {
    // Auth Graph
    object Splash : IshaaraDestination("auth/splash")
    object Login : IshaaraDestination("auth/login")
    object Onboarding : IshaaraDestination("auth/onboarding")

    // Student / Passenger Graph
    object StudentHome : IshaaraDestination("student/home")
    object StudentDiscovery : IshaaraDestination("student/discovery")
    object StudentRideTracking : IshaaraDestination("student/ride/{rideId}") {
        fun createRoute(rideId: String) = "student/ride/$rideId"
    }
    object StudentSafety : IshaaraDestination("student/safety")
    object StudentHistory : IshaaraDestination("student/history")

    // Driver / Conductor Graph
    object DriverDashboard : IshaaraDestination("driver/dashboard")
    object DriverActiveTrip : IshaaraDestination("driver/trip/{tripId}") {
        fun createRoute(tripId: String) = "driver/trip/$tripId"
    }
    object DriverRideRequests : IshaaraDestination("driver/requests")
    object DriverEarnings : IshaaraDestination("driver/earnings")
    object DriverProfile : IshaaraDestination("driver/profile")
}
