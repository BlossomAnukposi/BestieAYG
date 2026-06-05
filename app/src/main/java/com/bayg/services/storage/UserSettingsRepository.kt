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
        val roomUserId = resolveRoomUserId() ?: return@withContext null
        val userId = roomUserId.toString()
        val dao = db.userSettingsDao()
        dao.getByUserId(userId) ?: run {
            val defaults = UserSettings(userId = userId)
            val id = dao.insert(defaults)
            defaults.copy(id = id)
        }
    }

    suspend fun update(settings: UserSettings) = withContext(Dispatchers.IO) {
        db.userSettingsDao().update(settings)
    }

    private suspend fun resolveRoomUserId(): Long? {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return null
        return db.userDao().getByFirebaseUid(uid)?.id
    }
}
