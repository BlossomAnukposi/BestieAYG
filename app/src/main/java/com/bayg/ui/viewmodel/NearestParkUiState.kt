package com.bayg.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bayg.Park
import com.bayg.services.storage.AppDatabase
import com.bayg.services.storage.UserSettingsRepository
import com.bayg.services.storage.entities.UserSettings
import com.bayg.services.storage.sync.SyncWorker
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface NearestParkUiState {
    data object Loading : NearestParkUiState
    data class Success(val park: Park, val totalCount: Int) : NearestParkUiState
    data class Error(val message: String) : NearestParkUiState
}

class UserSettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = UserSettingsRepository(application)
    private val db = AppDatabase.Companion.getInstance(application)

    var settings by mutableStateOf<UserSettings?>(null)
        private set

    var isSaving by mutableStateOf(false)
        private set

    var displayName by mutableStateOf("")
        private set

    var email by mutableStateOf("")
        private set

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            settings = repository.getOrCreate()
            loadUserProfile()
        }
    }

    private suspend fun loadUserProfile() {
        withContext(Dispatchers.IO) {
            try {
                val fbUser = FirebaseAuth.getInstance().currentUser ?: return@withContext
                val roomUser = db.userDao().getByFirebaseUid(fbUser.uid) ?: return@withContext

                val fullName = "${roomUser.firstName} ${roomUser.lastName}".trim()
                    .takeIf { it.isNotEmpty() } ?: roomUser.firstName
                    .takeIf { it.isNotEmpty() } ?: "User"

                displayName = fullName
                email = roomUser.email
            } catch (e: Exception) { /* ignore */ }
        }
    }

    fun save(settings: UserSettings, onSaved: () -> Unit = {}) {
        if (isSaving) return
        viewModelScope.launch {
            isSaving = true
            try {
                repository.update(settings)
                this@UserSettingsViewModel.settings = settings

                SyncWorker.Companion.runOnce(getApplication())
            } catch (e: Exception) {
                Log.e("UserSettingsViewModel", "Error saving settings", e)
            } finally {
                isSaving = false
                onSaved()
            }
        }
    }

    fun updateToggle(
        touchGrass: Boolean? = null,
        location: Boolean? = null,
        notifications: Boolean? = null,
    ) {
        val current = settings ?: return
        save(
            current.copy(
                touchGrassModeEnabled = touchGrass ?: current.touchGrassModeEnabled,
                locationEnabled = location ?: current.locationEnabled,
                notificationsEnabled = notifications ?: current.notificationsEnabled,
            ),
        )
    }

    fun saveLimits(dailyLimitMinutes: Int, blockDurationMinutes: Int, onSaved: () -> Unit = {}) {
        val current = settings ?: return
        save(
            current.copy(
                dailyLimitMinutes = dailyLimitMinutes,
                blockDurationMinutes = blockDurationMinutes,
            ),
            onSaved = onSaved,
        )
    }
}