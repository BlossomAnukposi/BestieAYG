package com.bayg.services.storage

import android.content.Context
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
        dao.getByUserId(roomUserId) ?: run {
            // Create default settings if none exist
            val defaults = UserSettings(userId = roomUserId)
            val id = dao.insert(defaults)
            defaults.copy(id = id)
        }
    }

    suspend fun update(settings: UserSettings) = withContext(Dispatchers.IO) {
        db.userSettingsDao().update(settings)
    }
}