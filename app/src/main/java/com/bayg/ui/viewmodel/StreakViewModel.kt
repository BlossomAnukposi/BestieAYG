package com.bayg.services.storage

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import java.time.temporal.TemporalAdjusters

class StreakViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)

    var streakCount by mutableIntStateOf(0)
        private set

    var activeStreakDays by mutableStateOf(BooleanArray(7) { false })
        private set

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            loadStreak()
        }
    }

    companion object {
        private const val TAG = "StreakViewModel"
    }

    public fun todayIndex(): Int {
        val today = LocalDate.now(ZoneId.systemDefault())
        return today.dayOfWeek.value % 7
    }

    private fun computeStreak(timestamps: List<Long>, profileCreatedAt: Long): Int {
        val zone = ZoneId.systemDefault()
        val daysWithEvents: Set<LocalDate> = timestamps
            .map { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
            .toSet()

        val floor = Instant.ofEpochMilli(profileCreatedAt).atZone(zone).toLocalDate()

        var streak = 0
        var day = LocalDate.now(zone).minusDays(1) // yesterday

        while (!day.isBefore(floor)) {
            if (day in daysWithEvents) break
            streak++
            day = day.minusDays(1)
        }

        return streak
    }

    private fun calculateActiveStreakDays(blockEvents: List<BlockEvent>): BooleanArray {
        val activeDays = BooleanArray(7) { false }
        val zone = ZoneId.systemDefault()
        val daysWithEvents: Set<LocalDate> = blockEvents
            .map { Instant.ofEpochMilli(it.triggeredAt).atZone(zone).toLocalDate() }
            .toSet()

        val today = LocalDate.now(zone)
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))

        val activeDates = mutableSetOf<LocalDate>()
        var day = today.minusDays(1)
        while (!day.isBefore(weekStart) && day !in daysWithEvents) {
            activeDates += day
            day = day.minusDays(1)
        }

        for (dayOffset in 0..6) {
            val date = weekStart.plusDays(dayOffset.toLong())
            activeDays[dayOffset] = date in activeDates
        }

        return activeDays
    }

    private suspend fun loadStreak() {
        withContext(Dispatchers.IO) {
            try {
                val fbUser = FirebaseAuth.getInstance().currentUser ?: return@withContext

                val localUser = db.userDao().getByFirebaseUid(fbUser.uid)
                val profileCreatedAt = localUser?.createdAt ?: System.currentTimeMillis()

                val blockEvents = db.blockEventDao().getAllBlockEvents(fbUser.uid)
                val timestamps = db.blockEventDao().getAllTriggeredTimestamps(fbUser.uid)

                streakCount = computeStreak(timestamps, profileCreatedAt)
                activeStreakDays = calculateActiveStreakDays(blockEvents)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading streak", e)
            }
        }
    }
}
