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
        val firebaseUid = FirebaseAuth.getInstance().currentUser?.uid ?: return@withContext null
        val roomUser = db.userDao().getByFirebaseUid(firebaseUid) ?: return@withContext null
        val roomUserId = roomUser.id
        val dao = db.userSettingsDao()
        val existing = dao.getByUserId(roomUserId)

        if (existing != null) {
            UsageTracker.setDailyLimitMs(appContext, existing.dailyLimitMinutes * 60_000L)
            return@withContext existing
        }

        val defaults = UserSettings(userId = roomUserId)
        val id = dao.insert(defaults)
        val created = defaults.copy(id = id)

        UsageTracker.setDailyLimitMs(appContext, created.dailyLimitMinutes * 60_000L)
        created
    }

    suspend fun update(settings: UserSettings) = withContext(Dispatchers.IO) {
        db.userSettingsDao().update(settings)

        UsageTracker.setDailyLimitMs(appContext, settings.dailyLimitMinutes * 60_000L)
    }
}