package com.bayg.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bayg.UsageTracker
import com.bayg.services.storage.AppDatabase
import com.bayg.services.storage.entities.UserSettings
import com.bayg.services.storage.sync.SyncWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class SettingsUiState {
    data object Loading : SettingsUiState()
    data class Success(val settings: UserSettings) : SettingsUiState()
    data class Error(val message: String) : SettingsUiState()
}

class ProfileSettingsViewModel(
    private val context: Context,
    private val userId: String
) : ViewModel() {

    private val db = AppDatabase.getInstance(context)
    private val userSettingsDao = db.userSettingsDao()

    private val _settingsState = MutableStateFlow<SettingsUiState>(SettingsUiState.Loading)
    val settingsState: StateFlow<SettingsUiState> = _settingsState

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            try {
                val roomUser = db.userDao().getByFirebaseUid(userId)
                if (roomUser == null) {
                    _settingsState.value = SettingsUiState.Error("Local user not found for uid: $userId")
                    return@launch
                }

                val roomUserId = roomUser.id

                // Now call DAO with the Room user id (Long)
                val settings = userSettingsDao.getByUserId(roomUserId)
                if (settings != null) {
                    // Even on a fresh read (not just insert), mirror the canonical
                    // Room value into SharedPreferences so the AccessibilityService
                    // pick up whatever the user has configured previously. This
                    // covers existing installs that pre-date the mirror-on-save fix.
                    UsageTracker.setDailyLimitMs(context, settings.dailyLimitMinutes * 60_000L)
                    _settingsState.value = SettingsUiState.Success(settings)
                } else {
                    // Create default settings referencing the Room user id (Long)
                    val defaultSettings = UserSettings(
                        userId = roomUserId,
                        dailyLimitMinutes = 45,
                        blockDurationMinutes = 30,
                        touchGrassModeEnabled = true,
                        locationEnabled = true,
                        notificationsEnabled = true
                    )
                    userSettingsDao.insert(defaultSettings)
                    // Mirror the default daily limit to SharedPreferences so the
                    // AccessibilityService and BlockedActivity (which read via
                    // UsageTracker.getDailyLimitMs) see the same canonical cap
                    // the Room default implies — covered here because this VM
                    // bypasses UserSettingsRepository and writes to the DAO directly.
                    UsageTracker.setDailyLimitMs(context, defaultSettings.dailyLimitMinutes * 60_000L)
                    _settingsState.value = SettingsUiState.Success(defaultSettings)
                }
            } catch (e: Exception) {
                _settingsState.value = SettingsUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun updateDailyLimit(minutes: Int) {
        viewModelScope.launch {
            try {
                val currentSettings = (settingsState.value as? SettingsUiState.Success)?.settings
                    ?: return@launch

                val updated = currentSettings.copy(dailyLimitMinutes = minutes)
                userSettingsDao.update(updated)
                // Mirror the change to SharedPreferences so the AccessibilityService
                // reads the new cap on its next poll. Same coverage gap as in
                // loadSettings — this VM uses the DAO directly, so the
                // repository-level mirror in UserSettingsRepository.update does
                // not fire for this code path.
                UsageTracker.setDailyLimitMs(context, minutes * 60_000L)
                _settingsState.value = SettingsUiState.Success(updated)

                // Trigger sync to Firestore
                SyncWorker.runOnce(context)
            } catch (e: Exception) {
                _settingsState.value = SettingsUiState.Error(e.message ?: "Update failed")
            }
        }
    }

    fun updateBlockDuration(minutes: Int) {
        viewModelScope.launch {
            try {
                val currentSettings = (settingsState.value as? SettingsUiState.Success)?.settings
                    ?: return@launch

                val updated = currentSettings.copy(blockDurationMinutes = minutes)
                userSettingsDao.update(updated)
                _settingsState.value = SettingsUiState.Success(updated)

                // Trigger sync to Firestore
                SyncWorker.runOnce(context)
            } catch (e: Exception) {
                _settingsState.value = SettingsUiState.Error(e.message ?: "Update failed")
            }
        }
    }

    fun updateTouchGrassMode(enabled: Boolean) {
        viewModelScope.launch {
            try {
                val currentSettings = (settingsState.value as? SettingsUiState.Success)?.settings
                    ?: return@launch

                val updated = currentSettings.copy(touchGrassModeEnabled = enabled)
                userSettingsDao.update(updated)
                _settingsState.value = SettingsUiState.Success(updated)

                SyncWorker.runOnce(context)
            } catch (e: Exception) {
                _settingsState.value = SettingsUiState.Error(e.message ?: "Update failed")
            }
        }
    }

    fun updateLocation(enabled: Boolean) {
        viewModelScope.launch {
            try {
                val currentSettings = (settingsState.value as? SettingsUiState.Success)?.settings
                    ?: return@launch

                val updated = currentSettings.copy(locationEnabled = enabled)
                userSettingsDao.update(updated)
                _settingsState.value = SettingsUiState.Success(updated)

                SyncWorker.runOnce(context)
            } catch (e: Exception) {
                _settingsState.value = SettingsUiState.Error(e.message ?: "Update failed")
            }
        }
    }

    fun updateNotifications(enabled: Boolean) {
        viewModelScope.launch {
            try {
                val currentSettings = (settingsState.value as? SettingsUiState.Success)?.settings
                    ?: return@launch

                val updated = currentSettings.copy(notificationsEnabled = enabled)
                userSettingsDao.update(updated)
                _settingsState.value = SettingsUiState.Success(updated)

                SyncWorker.runOnce(context)
            } catch (e: Exception) {
                _settingsState.value = SettingsUiState.Error(e.message ?: "Update failed")
            }
        }
    }
}