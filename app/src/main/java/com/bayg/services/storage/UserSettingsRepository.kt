package com.bayg.services.storage

import android.content.Context
import com.bayg.UsageTracker
import com.bayg.services.storage.entities.UserSettings
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserSettingsRepository(
    context: Context,
    private val db: AppDatabase = AppDatabase.getInstance(context),
) {
    private val appContext = context.applicationContext

    suspend fun getOrCreate(): UserSettings? = withContext(Dispatchers.IO) {
        // Get Firebase UID from logged-in user
        val firebaseUid = FirebaseAuth.getInstance().currentUser?.uid ?: return@withContext null

        // Get Room User ID using Firebase UID
        val roomUser = db.userDao().getByFirebaseUid(firebaseUid) ?: return@withContext null
        val roomUserId = roomUser.id

        // Query settings by Room User ID
        val dao = db.userSettingsDao()
        val existing = dao.getByUserId(roomUserId)
        if (existing != null) {
            // Always mirror the canonical Room value into SharedPreferences
            // so the AccessibilityService and BlockedActivity see the same cap.
            // This covers existing installs where the Room row was inserted
            // before we started mirroring on every save — opening any settings
            // screen once after the fix lands is enough to bootstrap.
            UsageTracker.setDailyLimitMs(appContext, existing.dailyLimitMinutes * 60_000L)
            return@withContext existing
        }
        // Create default settings if none exist
        val defaults = UserSettings(userId = roomUserId)
        val id = dao.insert(defaults)
        val created = defaults.copy(id = id)
        // Mirror the freshly-inserted Room default so both stores agree
        // before the service's first poll.
        UsageTracker.setDailyLimitMs(appContext, created.dailyLimitMinutes * 60_000L)
        created
    }

    suspend fun update(settings: UserSettings) = withContext(Dispatchers.IO) {
        db.userSettingsDao().update(settings)
        // Mirror the daily limit to SharedPreferences so the in-service
        // poll (which can't open a Room cursor from its accessibility-
        // binding lifecycle) sees the same cap on its next tick instead
        // of holding a stale value from a previous install or run.
        UsageTracker.setDailyLimitMs(appContext, settings.dailyLimitMinutes * 60_000L)
    }
}