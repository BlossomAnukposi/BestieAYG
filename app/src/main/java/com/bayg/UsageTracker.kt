package com.bayg

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.SharedPreferences
import java.util.Calendar

/**
 * UsageTracker
 *
 * Reads today's Instagram screen time using UsageStatsManager.
 * The daily limit is stored in SharedPreferences so it can be
 * adjusted in the future (e.g. from a settings screen).
 *
 * Default limit: 30 minutes per day.
 */
object UsageTracker {

    const val INSTAGRAM_PACKAGE = "com.instagram.android"

    private const val PREFS_NAME = "bayg_usage_prefs"
    private const val KEY_DAILY_LIMIT_MS = "instagram_daily_limit_ms"
    private const val DEFAULT_LIMIT_MS = 45L * 60L * 1000L

    // ── Public API ────────────────────────────────────────────────────────────

    fun isLimitExceeded(context: Context): Boolean {
        val used = getTodayUsageMs(context)
        val limit = getDailyLimitMs(context)
        return used >= limit
    }

    fun getTodayUsageMs(context: Context): Long {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = cal.timeInMillis
        val now = System.currentTimeMillis()

        val stats = usm.queryAndAggregateUsageStats(startOfDay, now)
        return stats[INSTAGRAM_PACKAGE]?.totalTimeInForeground ?: 0L
    }

    fun getDailyLimitMs(context: Context): Long =
        prefs(context).getLong(KEY_DAILY_LIMIT_MS, DEFAULT_LIMIT_MS)

    fun setDailyLimitMs(context: Context, limitMs: Long) {
        prefs(context).edit().putLong(KEY_DAILY_LIMIT_MS, limitMs).apply()
    }

    fun formatDuration(ms: Long): String {
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        return when {
            hours > 0 -> "${hours}h ${minutes}min"
            else -> "${minutes}min"
        }
    }

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
