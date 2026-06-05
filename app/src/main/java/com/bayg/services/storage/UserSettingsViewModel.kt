package com.bayg.services.storage

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bayg.services.storage.entities.UserSettings
import kotlinx.coroutines.launch

class UserSettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = UserSettingsRepository(application)

    var settings by mutableStateOf<UserSettings?>(null)
        private set

    var isSaving by mutableStateOf(false)
        private set

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            settings = repository.getOrCreate()
        }
    }

    fun save(settings: UserSettings, onSaved: () -> Unit = {}) {
        if (isSaving) return
        viewModelScope.launch {
            isSaving = true
            repository.update(settings)
            this@UserSettingsViewModel.settings = settings
            isSaving = false
            onSaved()
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
