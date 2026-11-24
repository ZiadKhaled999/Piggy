package com.oryno.piggy_ledger

import com.oryno.piggy_ledger.data.NotificationItem
import com.oryno.piggy_ledger.data.NotificationRemoteManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class NotificationRemoteManagerTest {

    @Test
    fun matchesRegion_supportsGlobalSingleAndMultipleCountries() {
        assertTrue(NotificationRemoteManager.matchesRegion("", "EG"))
        assertTrue(NotificationRemoteManager.matchesRegion("all", "US"))
        assertTrue(NotificationRemoteManager.matchesRegion("EG", "eg"))
        assertFalse(NotificationRemoteManager.matchesRegion("EG", "US"))

        // Multiple selected countries
        assertTrue(NotificationRemoteManager.matchesRegion("EG, SA, AE", "SA"))
        assertTrue(NotificationRemoteManager.matchesRegion("EG, SA, AE", "AE"))
        assertFalse(NotificationRemoteManager.matchesRegion("EG, SA, AE", "GB"))
        assertFalse(NotificationRemoteManager.matchesRegion("EG, SA, AE", ""))
    }

    @Test
    fun matchesUser_supportsEmailAndClerkUserId() {
        assertTrue(NotificationRemoteManager.matchesUser("", "ahmed@example.com", "user_123"))
        assertTrue(NotificationRemoteManager.matchesUser("Ahmed@Example.com", "ahmed@example.com", "user_123"))
        assertTrue(NotificationRemoteManager.matchesUser("user_123", "ahmed@example.com", "user_123"))
        assertTrue(NotificationRemoteManager.matchesUser("other@mail.com, user_123", "ahmed@example.com", "user_123"))

        assertFalse(NotificationRemoteManager.matchesUser("someone@mail.com", "ahmed@example.com", "user_123"))
        assertFalse(NotificationRemoteManager.matchesUser("user_999", "", ""))
    }

    @Test
    fun matchesNotificationTarget_appliesAllFiltersTogether() {
        val targeted = NotificationItem(
            id = "n1",
            title = "Offer",
            body = "Special offer",
            targetAudience = "free",
            targetRegion = "EG, SA",
            targetUser = "user_abc",
            status = "active",
            expiresAt = null
        )

        assertTrue(
            NotificationRemoteManager.matchesNotificationTarget(
                item = targeted,
                isPremium = false,
                region = "EG",
                userEmail = "test@example.com",
                userId = "user_abc",
                now = Date(1700000000000L)
            )
        )

        // Fails if pro user when targeting free
        assertFalse(
            NotificationRemoteManager.matchesNotificationTarget(
                item = targeted,
                isPremium = true,
                region = "EG",
                userEmail = "test@example.com",
                userId = "user_abc",
                now = Date(1700000000000L)
            )
        )

        // Fails if outside selected countries
        assertFalse(
            NotificationRemoteManager.matchesNotificationTarget(
                item = targeted,
                isPremium = false,
                region = "US",
                userEmail = "test@example.com",
                userId = "user_abc",
                now = Date(1700000000000L)
            )
        )
    }
}
