package com.bayg.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bayg.services.storage.AppDatabase
import com.bayg.services.storage.entities.BlockEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class StatsPeriod {
    WEEK, MONTH, ALL_TIME
}

/** One bar in the "Daily Usage vs Limit" chart. */
data class DayUsage(
    val label: String,      // "M", "T", "W"...
    val minutes: Int,
    val isToday: Boolean = false,
)

/** One row in the "Block Events" list, pre-formatted for display. */
data class BlockEventUi(
    val id: Long,
    val dateLabel: String,   // "Today" / "Sat" / "Jun 5"
    val timeLabel: String,   // "15:42"
    val title: String,       // "Daily limit exceeded (2h 13m)"
    val durationLabel: String, // "30 min"
    val severity: com.bayg.services.storage.entities.BlockEventSeverity,
)

data class StatsSummary(
    val totalLabel: String,   // "14h 22m"
    val dailyAvgLabel: String, // "2h 03m"
    val blockCountLabel: String, // "4x"
)

sealed class StatsUiState {
    data object Loading : StatsUiState()
    data class Success(
        val period: StatsPeriod,
        val summary: StatsSummary,
        val dailyLimitMinutes: Int,
        val dailyUsage: List<DayUsage>, // empty for ALL_TIME (no bar chart)
        val blockEvents: List<BlockEventUi>,
    ) : StatsUiState()
    data class Error(val message: String) : StatsUiState()
}

internal class StatsViewModel(
    private val context: Context,
    private val userId: String,
) : ViewModel() {

    private val db = AppDatabase.getInstance(context)

    private val _uiState = MutableStateFlow<StatsUiState>(StatsUiState.Loading)
    val uiState: StateFlow<StatsUiState> = _uiState

    private var roomUserId: Long? = null

    init {
        loadStats(StatsPeriod.WEEK)
    }

    fun loadStats(period: StatsPeriod) {
        viewModelScope.launch {
            _uiState.value = StatsUiState.Loading
            try {
                val roomUser = db.userDao().getByFirebaseUid(userId)
                if (roomUser == null) {
                    _uiState.value = StatsUiState.Error("Local user not found for uid: $userId")
                    return@launch
                }
                roomUserId = roomUser.id

                val settings = db.userSettingsDao().getByUserId(roomUser.id)
                val dailyLimitMinutes = settings?.dailyLimitMinutes ?: 45

                val (rangeStart, rangeEnd) = rangeFor(period)

                val dailyUsage = if (period == StatsPeriod.ALL_TIME) {
                    emptyList()
                } else {
                    loadDailyUsage(roomUser.id, rangeStart, rangeEnd, period)
                }

                val blockEvents = loadBlockEvents(rangeStart, rangeEnd)

                val totalMinutes = if (period == StatsPeriod.ALL_TIME) {
                    db.dailyUsageDao().getAll(roomUser.id).sumOf { it.usageMinutes }
                } else {
                    dailyUsage.sumOf { it.minutes }
                }

                val dayCount = if (period == StatsPeriod.ALL_TIME) {
                    db.dailyUsageDao().getAll(roomUser.id).size.takeIf { it > 0 } ?: 1
                } else {
                    dailyUsage.size.takeIf { it > 0 } ?: 1
                }

                val avgMinutes = totalMinutes / dayCount

                val summary = StatsSummary(
                    totalLabel = formatDuration(totalMinutes),
                    dailyAvgLabel = formatDuration(avgMinutes),
                    blockCountLabel = "${blockEvents.size}x",
                )

                _uiState.value = StatsUiState.Success(
                    period = period,
                    summary = summary,
                    dailyLimitMinutes = dailyLimitMinutes,
                    dailyUsage = dailyUsage,
                    blockEvents = blockEvents,
                )
            } catch (e: Exception) {
                _uiState.value = StatsUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    // ── Range helpers ────────────────────────────────────────────────────

    /** Returns start/end millis for the given period (end is "now"). */
    private fun rangeFor(period: StatsPeriod): Pair<Long, Long> {
        val now = Calendar.getInstance()
        val end = now.timeInMillis

        val start = Calendar.getInstance()
        when (period) {
            StatsPeriod.WEEK -> {
                // Start of this week (Monday)
                start.firstDayOfWeek = Calendar.MONDAY
                val dayOfWeek = start.get(Calendar.DAY_OF_WEEK)
                val daysFromMonday = (dayOfWeek - Calendar.MONDAY + 7) % 7
                start.add(Calendar.DAY_OF_MONTH, -daysFromMonday)
                clearToStartOfDay(start)
            }
            StatsPeriod.MONTH -> {
                start.set(Calendar.DAY_OF_MONTH, 1)
                clearToStartOfDay(start)
            }
            StatsPeriod.ALL_TIME -> {
                start.add(Calendar.YEAR, -10) // effectively "all"
                clearToStartOfDay(start)
            }
        }
        return start.timeInMillis to end
    }

    private fun clearToStartOfDay(cal: Calendar) {
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
    }

    private val dateKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /**
     * Loads daily usage for the chart.
     * For WEEK: always returns 7 entries (Mon..Sun), filling gaps with 0.
     * For MONTH: returns one entry per day of the current month so far.
     */
    private suspend fun loadDailyUsage(
        roomUserId: Long,
        rangeStart: Long,
        rangeEnd: Long,
        period: StatsPeriod,
    ): List<DayUsage> {
        val startKey = dateKeyFormat.format(Date(rangeStart))
        val endKey = dateKeyFormat.format(Date(rangeEnd))
        val rows = db.dailyUsageDao().getBetween(roomUserId, startKey, endKey)
        val byDate = rows.associateBy { it.date }

        val todayKey = dateKeyFormat.format(Date())
        val labelFormat = if (period == StatsPeriod.WEEK) {
            SimpleDateFormat("EEEEE", Locale.ENGLISH) // single-letter day, e.g. "M","T","W"
        } else {
            SimpleDateFormat("d", Locale.ENGLISH) // day-of-month number for Month view
        }

        val result = mutableListOf<DayUsage>()
        val cursor = Calendar.getInstance().apply { timeInMillis = rangeStart }
        val today = Calendar.getInstance()

        while (!cursor.after(today)) {
            val key = dateKeyFormat.format(cursor.time)
            val minutes = byDate[key]?.usageMinutes ?: 0
            result.add(
                DayUsage(
                    label = labelFormat.format(cursor.time),
                    minutes = minutes,
                    isToday = key == todayKey,
                )
            )
            cursor.add(Calendar.DAY_OF_MONTH, 1)
            if (period == StatsPeriod.WEEK && result.size >= 7) break
        }

        // For WEEK, pad to exactly 7 days (Mon..Sun) even if "today" is mid-week
        if (period == StatsPeriod.WEEK) {
            val cal = Calendar.getInstance().apply { timeInMillis = rangeStart }
            val full = mutableListOf<DayUsage>()
            val singleLetter = SimpleDateFormat("EEEEE", Locale.ENGLISH)
            for (i in 0 until 7) {
                val key = dateKeyFormat.format(cal.time)
                val minutes = byDate[key]?.usageMinutes ?: 0
                full.add(
                    DayUsage(
                        label = singleLetter.format(cal.time),
                        minutes = minutes,
                        isToday = key == todayKey,
                    )
                )
                cal.add(Calendar.DAY_OF_MONTH, 1)
            }
            return full
        }

        return result
    }

    private suspend fun loadBlockEvents(rangeStart: Long, rangeEnd: Long): List<BlockEventUi> {
        val events = db.blockEventDao().getBetween(userId, rangeStart, rangeEnd)
        return events.map { it.toUi() }
    }

    private fun BlockEvent.toUi(): BlockEventUi {
        val cal = Calendar.getInstance().apply { timeInMillis = triggeredAt }
        val today = Calendar.getInstance()
        val sameDay = cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)

        val dateLabel = if (sameDay) {
            "Today"
        } else {
            SimpleDateFormat("EEE", Locale.ENGLISH).format(Date(triggeredAt))
        }
        val timeLabel = SimpleDateFormat("HH:mm", Locale.ENGLISH).format(Date(triggeredAt))

        val title = if (!detail.isNullOrBlank()) "$label $detail" else label

        return BlockEventUi(
            id = id,
            dateLabel = dateLabel,
            timeLabel = timeLabel,
            title = title,
            durationLabel = "$blockDurationMinutes min",
            severity = severity,
        )
    }

    private fun formatDuration(totalMinutes: Int): String {
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours > 0 -> "${hours}h %02dm".format(minutes)
            else -> "${minutes}m"
        }
    }
}
