package com.ishara.app.navigation

object Graph {

    const val ROOT = "root"

    const val AUTH = "auth"

    const val PASSENGER = "passenger"

    const val DRIVER = "driver"
}

object Destination {

    // Root
    const val Splash = "splash"

    // Auth
    const val Login = "login"
    const val RoleSelection = "role_selection"

    // Passenger
    const val Home = "home"
    const val Discover = "discover"
    const val Trips = "trips"
    const val Profile = "profile"
    const val Tracking = "tracking"
    const val Safety = "safety"
    const val Notifications = "notifications"

    // Driver
    const val Dashboard = "dashboard"
    const val ActiveTrip = "active_trip"
    const val Requests = "requests"
    const val Earnings = "earnings"
    const val Vehicle = "vehicle"
    const val VoiceAssistant = "voice_assistant"
    const val DriverProfile = "driver_profile"
    const val DriverSettings = "driver_settings"
}