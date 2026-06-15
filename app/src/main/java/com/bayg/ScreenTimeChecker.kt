package com.bayg

import android.app.usage.UsageStatsManager
import android.content.Context
import java.util.Calendar

/**
 * ScreenTimeChecker
 *
 * Calculates the user's total foreground screen time today (since midnight)
 * across all apps, excluding the system UI and this app itself.
 *
 * Uses the same UsageStatsManager approach as UsageTracker so no
 * additional permissions are needed beyond PACKAGE_USAGE_STATS.
 */
object ScreenTimeChecker {

    private const val PREFS_NAME = "bayg_screentime_prefs"
    private const val KEY_TOTAL_LIMIT_MS = "total_daily_limit_ms"
    private const val KEY_NOTIFICATION_SENT_DATE = "notification_sent_date"

    // Default: notify after 2 hours of total screen time
    private const val DEFAULT_TOTAL_LIMIT_MS = 2L * 60L * 60L * 1000L

    // Packages to exclude from total (launcher, system UI, etc.)
    private val EXCLUDED_PACKAGES = setOf(
        "com.android.systemui",
        "com.android.launcher",
        "com.android.launcher3",
        "com.google.android.apps.nexuslauncher",
        "com.miui.home",
        "com.sec.android.app.launcher",
        "com.huawei.android.launcher",
        "com.bayg" // exclude our own app
    )

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Returns true if total screen time today has exceeded the limit
     * AND a notification has not already been sent today.
     */
    fun shouldNotify(context: Context): Boolean {
        if (alreadyNotifiedToday(context)) return false
        return getTodayTotalUsageMs(context) >= getTotalDailyLimitMs(context)
    }

    /**
     * Marks today as notified so we don't fire again until tomorrow.
     */
    fun markNotifiedToday(context: Context) {
        val today = todayDateString()
        prefs(context).edit().putString(KEY_NOTIFICATION_SENT_DATE, today).apply()
    }

    /**
     * Total foreground time today in milliseconds across all non-excluded apps.
     */
    fun getTodayTotalUsageMs(context: Context): Long {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val stats = usm.queryAndAggregateUsageStats(cal.timeInMillis, System.currentTimeMillis())

        return stats.entries
            .filter { (pkg, _) -> pkg !in EXCLUDED_PACKAGES }
            .sumOf { (_, stat) -> stat.totalTimeInForeground }
    }

    fun getTotalDailyLimitMs(context: Context): Long =
        prefs(context).getLong(KEY_TOTAL_LIMIT_MS, DEFAULT_TOTAL_LIMIT_MS)

    fun setTotalDailyLimitMs(context: Context, limitMs: Long) {
        prefs(context).edit().putLong(KEY_TOTAL_LIMIT_MS, limitMs).apply()
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private fun alreadyNotifiedToday(context: Context): Boolean {
        val lastSent = prefs(context).getString(KEY_NOTIFICATION_SENT_DATE, null)
        return lastSent == todayDateString()
    }

    private fun todayDateString(): String {
        val cal = Calendar.getInstance()
        return "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH)}-${cal.get(Calendar.DAY_OF_MONTH)}"
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
