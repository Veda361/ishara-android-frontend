package com.ishara.app

import com.ishara.app.core.notifications.NotificationRouter
import com.ishara.app.domain.model.UserRole
import com.ishara.app.navigation.NavigationCommand
import com.ishara.app.navigation.NavigationManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for NotificationRouter.
 * Verifies secure deep link resolution, role isolation, unauthenticated blocking, and unknown event safety.
 */
class NotificationRouterTest {

    @Test
    fun `routeNotification blocks navigation if user is unauthenticated`() {
        val navManager = NavigationManager()
        val router = NotificationRouter(navManager)

        val success = router.routeNotification(
            type = "RIDE_COMPLETED",
            data = mapOf("rideId" to "ride_123"),
            authenticatedRole = null
        )

        assertFalse(success)
    }

    @Test
    fun `resolvePassengerRoute routes ride events correctly`() {
        val navManager = NavigationManager()
        val router = NotificationRouter(navManager)

        // RIDE_COMPLETED -> rating screen
        assertEquals(
            "student/ride/r1/rating",
            router.resolveRoute("RIDE_COMPLETED", mapOf("rideId" to "r1"), UserRole.USER)
        )

        // RIDE_STARTED -> tracking screen
        assertEquals(
            "student/ride/r1",
            router.resolveRoute("RIDE_STARTED", mapOf("rideId" to "r1"), UserRole.USER)
        )

        // RIDE_DRIVER_ARRIVING -> tracking screen
        assertEquals(
            "student/ride/r1",
            router.resolveRoute("RIDE_DRIVER_ARRIVING", mapOf("rideId" to "r1"), UserRole.USER)
        )

        // RIDE_REQUEST_REJECTED -> request status
        assertEquals(
            "student/ride/request/status/req1",
            router.resolveRoute("RIDE_REQUEST_REJECTED", mapOf("requestId" to "req1"), UserRole.USER)
        )

        // PAYMENT_CAPTURED -> payment screen
        assertEquals(
            "student/ride/r1/payment",
            router.resolveRoute("PAYMENT_CAPTURED", mapOf("rideId" to "r1"), UserRole.USER)
        )

        // REFUND_PROCESSED -> notifications inbox
        assertEquals(
            "student/notifications",
            router.resolveRoute("REFUND_PROCESSED", emptyMap(), UserRole.USER)
        )
    }

    @Test
    fun `resolveDriverRoute routes driver events correctly`() {
        val navManager = NavigationManager()
        val router = NotificationRouter(navManager)

        // RIDE_REQUEST_CREATED -> requests screen
        assertEquals(
            "driver/requests",
            router.resolveRoute("RIDE_REQUEST_CREATED", mapOf("requestId" to "req1"), UserRole.DRIVER_CONDUCTOR)
        )

        // RIDE_STARTED -> active trip
        assertEquals(
            "driver/active_trip",
            router.resolveRoute("RIDE_STARTED", mapOf("rideId" to "r1"), UserRole.DRIVER_CONDUCTOR)
        )

        // SETTLEMENT_PROCESSED -> driver home
        assertEquals(
            "driver/home",
            router.resolveRoute("SETTLEMENT_PROCESSED", mapOf("settlementId" to "s1"), UserRole.DRIVER_CONDUCTOR)
        )

        // PAYMENT_CAPTURED -> driver home
        assertEquals(
            "driver/home",
            router.resolveRoute("PAYMENT_CAPTURED", mapOf("rideId" to "r1"), UserRole.DRIVER_CONDUCTOR)
        )
    }

    @Test
    fun `role isolation blocks passenger from driver-only events`() {
        val navManager = NavigationManager()
        val router = NotificationRouter(navManager)

        val driverOnlyEvents = listOf(
            "RIDE_REQUEST_CREATED",
            "RIDE_REQUEST_CANCELLED",
            "SETTLEMENT_PROCESSED"
        )

        for (event in driverOnlyEvents) {
            val route = router.resolveRoute(event, mapOf("requestId" to "req1"), UserRole.USER)
            assertNull("Passenger should not be able to resolve driver event $event", route)

            val routed = router.routeNotification(event, mapOf("requestId" to "req1"), UserRole.USER)
            assertFalse("routeNotification should return false for driver event $event dispatched to USER", routed)
        }
    }

    @Test
    fun `role isolation blocks driver from passenger-only events`() {
        val navManager = NavigationManager()
        val router = NotificationRouter(navManager)

        val passengerOnlyEvents = listOf(
            "RIDE_REQUEST_REJECTED",
            "RIDE_REQUEST_EXPIRED",
            "REFUND_PROCESSED"
        )

        for (event in passengerOnlyEvents) {
            val route = router.resolveRoute(event, mapOf("requestId" to "req1"), UserRole.DRIVER_CONDUCTOR)
            assertNull("Driver should not be able to resolve passenger event $event", route)

            val routed = router.routeNotification(event, mapOf("requestId" to "req1"), UserRole.DRIVER_CONDUCTOR)
            assertFalse("routeNotification should return false for passenger event $event dispatched to DRIVER", routed)
        }
    }

    @Test
    fun `unknown notification type does not crash and routes safely to inbox`() {
        val navManager = NavigationManager()
        val router = NotificationRouter(navManager)

        val userRoute = router.resolveRoute("FUTURE_NEW_EVENT", emptyMap(), UserRole.USER)
        assertEquals("student/notifications", userRoute)

        val driverRoute = router.resolveRoute("FUTURE_NEW_EVENT", emptyMap(), UserRole.DRIVER_CONDUCTOR)
        assertEquals("driver/notifications", driverRoute)
    }

    @Test
    fun `routeNotification dispatches command to NavigationManager`() = runBlocking {
        val navManager = NavigationManager()
        val router = NotificationRouter(navManager)

        val success = router.routeNotification(
            type = "RIDE_COMPLETED",
            data = mapOf("rideId" to "ride_444"),
            authenticatedRole = UserRole.USER
        )

        assertTrue(success)
        val command = navManager.commands.first()
        assertTrue(command is NavigationCommand.NavigateTo)
        assertEquals("student/ride/ride_444/rating", (command as NavigationCommand.NavigateTo).route)
    }
}
