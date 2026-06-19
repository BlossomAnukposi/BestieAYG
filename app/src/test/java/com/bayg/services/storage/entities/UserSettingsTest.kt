package com.bayg.services.storage.entities

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UserSettingsTest {

    @Test
    fun `default values match app setup expectations`() {
        val settings = UserSettings(userId = 1L)

        assertEquals(45, settings.dailyLimitMinutes)
        assertEquals(30, settings.blockDurationMinutes)
        assertTrue(settings.touchGrassModeEnabled)
        assertTrue(settings.locationEnabled)
        assertTrue(settings.notificationsEnabled)
    }

    @Test
    fun `copy preserves updated limits`() {
        val original = UserSettings(userId = 2L)
        val updated = original.copy(dailyLimitMinutes = 60, locationEnabled = false)

        assertEquals(60, updated.dailyLimitMinutes)
        assertEquals(false, updated.locationEnabled)
        assertEquals(2L, updated.userId)
    }
}
