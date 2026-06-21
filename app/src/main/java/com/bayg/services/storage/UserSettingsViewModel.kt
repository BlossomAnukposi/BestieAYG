package com.bayg.services.storage

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bayg.services.storage.entities.BlockEvent
import com.bayg.services.storage.entities.UserSettings
import com.bayg.services.storage.sync.SyncWorker
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.DayOfWeek

class UserSettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = UserSettingsRepository(application)
    private val db = AppDatabase.getInstance(application)

    var settings by mutableStateOf<UserSettings?>(null)
        private set

    var isSaving by mutableStateOf(false)
        private set

    var displayName by mutableStateOf("")
        private set

    var email by mutableStateOf("")
        private set

    var streakCount by mutableStateOf(0)
        private set

    var activeStreakDays by mutableStateOf(BooleanArray(7) { false })
        private set

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            settings = repository.getOrCreate()
            loadUserProfile()
            loadStreak()
        }
    }

    private suspend fun loadStreak() {
        withContext(Dispatchers.IO) {
            try {
                val fbUser = FirebaseAuth.getInstance().currentUser ?: return@withContext
                val blockEvents = db.blockEventDao().getAllBlockEvents(fbUser.uid)

                streakCount = 0
                activeStreakDays = calculateActiveStreakDays(blockEvents)
            } catch (e: Exception) {
                // Fail silently; streak won't load but other data will still work
                android.util.Log.e("UserSettingsViewModel", "Error loading streak", e)
            }
        }
    }

    private fun calculateActiveStreakDays(blockEvents: List<BlockEvent>): BooleanArray {
        // Default to all false (no active days)
        val activeDays = BooleanArray(7) { false } // Index 0=Sun, 1=Mon, ..., 6=Sat

//        if (streak == null || streak.currentStreak <= 0 || streak.lastStreakDate == null) {
//            return activeDays
//        }
//
//        // Convert lastStreakDate to LocalDate
//        val instant = Instant.ofEpochMilli(streak.lastStreakDate!!)
//        val lastStreakLocalDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()
//
//        // Calculate the start date of the streak
//        val streakStartDate = lastStreakLocalDate.minusDays((streak.currentStreak - 1).toLong())

        // Get today's date
//        val today = LocalDate.now()
//
//        // For each day in the current week (Sunday to Saturday)
//        val weekStart = today.with(DayOfWeek.SUNDAY) // Start of current week (Sunday)
//
//        for (dayOffset in 0..6) {
//            val currentDate = weekStart.plusDays(dayOffset.toLong())
//            // Check if this date falls within the streak period
//            if (!currentDate.isBefore(streakStartDate) && !currentDate.isAfter(lastStreakLocalDate)) {
//                activeDays[dayOffset] = true
//            }
//        }

        return activeDays
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
            } catch (e: Exception) {
                // Fail silently; profile won't load but settings will still work
            }
        }
    }

    fun save(settings: UserSettings, onSaved: () -> Unit = {}) {
        if (isSaving) return
        viewModelScope.launch {
            isSaving = true
            try {
                repository.update(settings)
                this@UserSettingsViewModel.settings = settings

                // Sync to Firestore after update
                SyncWorker.runOnce(getApplication())
            } catch (e: Exception) {
                // Log error but still close dialog
                android.util.Log.e("UserSettingsViewModel", "Error saving settings", e)
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
