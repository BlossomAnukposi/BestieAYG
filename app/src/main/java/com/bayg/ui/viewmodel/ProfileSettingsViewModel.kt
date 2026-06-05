package com.bayg.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
                val settings = userSettingsDao.getByUserId(userId)
                if (settings != null) {
                    _settingsState.value = SettingsUiState.Success(settings)
                } else {
                    // Create default settings if none exist
                    val defaultSettings = UserSettings(
                        userId = userId,
                        dailyLimitMinutes = 45,
                        blockDurationMinutes = 30,
                        touchGrassModeEnabled = true,
                        locationEnabled = true,
                        notificationsEnabled = true
                    )
                    userSettingsDao.insert(defaultSettings)
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