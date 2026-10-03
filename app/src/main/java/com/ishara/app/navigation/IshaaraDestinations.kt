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
    object StudentSearch : IshaaraDestination("student/search")
    object StudentDiscovery : IshaaraDestination("student/discovery")
    object StudentVoiceTrip : IshaaraDestination("student/voice")
    object StudentRideRequest : IshaaraDestination("student/ride/request")
    object StudentRideRequestStatus : IshaaraDestination("student/ride/request/status/{requestId}") {
        fun createRoute(requestId: String) = "student/ride/request/status/$requestId"
    }
    object StudentRideTracking : IshaaraDestination("student/ride/{rideId}") {
        fun createRoute(rideId: String) = "student/ride/$rideId"
    }
    object StudentFareSummary : IshaaraDestination("student/ride/{rideId}/fare") {
        fun createRoute(rideId: String) = "student/ride/$rideId/fare"
    }
    object StudentPayment : IshaaraDestination("student/ride/{rideId}/payment") {
        fun createRoute(rideId: String) = "student/ride/$rideId/payment"
    }
    object StudentTransitRoutes : IshaaraDestination("student/transit/routes")
    object StudentRouteDetails : IshaaraDestination("student/transit/routes/{routeId}") {
        fun createRoute(routeId: String) = "student/transit/routes/$routeId"
    }
    object StudentSafety : IshaaraDestination("student/safety/{rideId}") {
        fun createRoute(rideId: String) = "student/safety/$rideId"
    }
    object StudentHistory : IshaaraDestination("student/history")
    object StudentNotifications : IshaaraDestination("student/notifications")
    object StudentProfile : IshaaraDestination("student/profile")

    // Driver / Conductor Graph
    object DriverOnboarding : IshaaraDestination("driver/onboarding")
    object DriverVerification : IshaaraDestination("driver/verification")
    object DriverAgency : IshaaraDestination("driver/agency")
    object DriverReadiness : IshaaraDestination("driver/readiness")
    object DriverDashboard : IshaaraDestination("driver/dashboard")
    object DriverActiveTrip : IshaaraDestination("driver/trip/{tripId}") {
        fun createRoute(tripId: String) = "driver/trip/$tripId"
    }
    object DriverRideRequests : IshaaraDestination("driver/requests")
    object DriverEarnings : IshaaraDestination("driver/earnings")
    object DriverVehicle : IshaaraDestination("driver/vehicle")
    object DriverTrips : IshaaraDestination("driver/trips")
    object DriverTripDetails : IshaaraDestination("driver/trips/{tripId}") {
        fun createRoute(tripId: String) = "driver/trips/$tripId"
    }
    object DriverNotifications : IshaaraDestination("driver/notifications")
    object DriverProfile : IshaaraDestination("driver/profile")
}
