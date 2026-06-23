package com.bayg.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bayg.managers.AppUsageManager
import com.bayg.services.storage.AppDatabase
import com.bayg.services.storage.entities.BlockEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
        val dailyUsage: List<DayUsage>,
        val blockEvents: List<BlockEventUi>,
    ) : StatsUiState()
    data class Error(val message: String) : StatsUiState()
}

internal class StatsViewModel(
    private val context: Context,
    private val userId: String,
) : ViewModel() {
    private val db = AppDatabase.getInstance(context)
    private val appUsageManager = AppUsageManager(context)
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
                val dailyUsage = loadDailyUsage(rangeStart, rangeEnd, period)
                val blockEvents = loadBlockEvents(rangeStart, rangeEnd)
                val totalMinutes = dailyUsage.sumOf { it.minutes }
                val dayCount = dailyUsage.size.takeIf { it > 0 } ?: 1
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

    private fun rangeFor(period: StatsPeriod): Pair<Long, Long> {
        val now = Calendar.getInstance()
        val end = now.timeInMillis

        val start = Calendar.getInstance()
        when (period) {
            StatsPeriod.WEEK -> {
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
                start.add(Calendar.YEAR, -10)
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
     * For ALL_TIME: walks the last ALL_TIME_DAY_CAP days via UsageStatsManager,
     *   bailing out early once the OS stops retaining per-day aggregates
     *   (typically ~7-30 days back). Returned list is used by the summary
     *   arithmetic; the chart hides it for ALL_TIME.
     */
    private suspend fun loadDailyUsage(
        rangeStart: Long,
        rangeEnd: Long,
        period: StatsPeriod,
    ): List<DayUsage> = withContext(Dispatchers.IO) {
        val today = Calendar.getInstance()
        val todayKey = dateKeyFormat.format(today.time)

        val labelFormat = when (period) {
            StatsPeriod.WEEK -> SimpleDateFormat("EEEEE", Locale.ENGLISH)
            StatsPeriod.MONTH -> SimpleDateFormat("d", Locale.ENGLISH)
            StatsPeriod.ALL_TIME -> SimpleDateFormat("MMM d", Locale.ENGLISH)
        }

        fun dayMinutes(dayStart: Long): Int {
            val dayEnd = (dayStart + DAY_MS).coerceAtMost(System.currentTimeMillis())
            if (dayStart >= dayEnd) return 0
            val ms = appUsageManager.getInstagramUsageTime(dayStart, dayEnd)
            return (ms / 60_000L).toInt().coerceAtLeast(0)
        }

        when (period) {
            StatsPeriod.WEEK -> {
                val cal = Calendar.getInstance().apply { timeInMillis = rangeStart }
                val result = mutableListOf<DayUsage>()
                for (i in 0 until 7) {
                    result.add(
                        DayUsage(
                            label = labelFormat.format(cal.time),
                            minutes = dayMinutes(cal.timeInMillis),
                            isToday = dateKeyFormat.format(cal.time) == todayKey,
                        )
                    )
                    cal.add(Calendar.DAY_OF_MONTH, 1)
                }
                result
            }
            StatsPeriod.MONTH -> {
                val now = System.currentTimeMillis()
                val cursor = Calendar.getInstance().apply { timeInMillis = rangeStart }
                val calToday = Calendar.getInstance()
                val result = mutableListOf<DayUsage>()
                while (!cursor.after(calToday) && cursor.timeInMillis <= now) {
                    result.add(
                        DayUsage(
                            label = labelFormat.format(cursor.time),
                            minutes = dayMinutes(cursor.timeInMillis),
                            isToday = dateKeyFormat.format(cursor.time) == todayKey,
                        )
                    )
                    cursor.add(Calendar.DAY_OF_MONTH, 1)
                }
                result
            }
            StatsPeriod.ALL_TIME -> {
                // Walk backward from today, stopping either at the hard cap
                // or once Android has clearly stopped returning data.
                val now = System.currentTimeMillis()
                val todayCal = Calendar.getInstance()

                clearToStartOfDay(todayCal)
                val todayMidnight = todayCal.timeInMillis
                val cursor = Calendar.getInstance().apply { timeInMillis = todayMidnight }
                var consecutiveZero = 0
                val result = mutableListOf<DayUsage>()

                while (result.size < ALL_TIME_DAY_CAP) {
                    val dayStart = cursor.timeInMillis

                    if (dayStart < rangeStart) break
                    val dayEnd = (dayStart + DAY_MS).coerceAtMost(now)

                    if (dayStart >= dayEnd) break
                    val minutes = dayMinutes(dayStart)
                    result.add(
                        DayUsage(
                            label = labelFormat.format(cursor.time),
                            minutes = minutes,
                            isToday = dateKeyFormat.format(cursor.time) == todayKey,
                        )
                    )

                    if (minutes == 0) consecutiveZero++ else consecutiveZero = 0
                    if (consecutiveZero >= ALL_TIME_ZERO_EXIT_THRESHOLD &&
                        result.size > ALL_TIME_ZERO_EXIT_THRESHOLD
                    ) break
                    cursor.add(Calendar.DAY_OF_MONTH, -1)
                }
                result.asReversed()
            }
        }
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

    companion object {
        private const val ALL_TIME_DAY_CAP = 365
        private const val ALL_TIME_ZERO_EXIT_THRESHOLD = 7
        private const val DAY_MS = 24L * 60L * 60L * 1000L
    }
}
