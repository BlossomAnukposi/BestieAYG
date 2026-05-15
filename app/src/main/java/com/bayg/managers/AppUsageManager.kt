package com.bayg.managers

import android.app.usage.UsageStatsManager
import android.content.Context
import android.util.Log

class AppUsageManager(private val context: Context) {

    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    companion object {
        private const val INSTAGRAM_PACKAGE = "com.instagram.android"
        private const val LOG_TAG = "AppUsageManager"
    }

    /**
     * Gets the total foreground time Instagram was used between startTime and endTime.
     * StartTime represents when user accepted usage tracking.
     * EndTime represents when data is fetched for dashboard.
     * @param startTime Start time in milliseconds
     * @param endTime End time in milliseconds
     * @return Total time in milliseconds, or 0L if permission not granted or error
     */
    fun getInstagramUsageTime(startTime: Long, endTime: Long): Long {
        return try {
            val stats = usageStatsManager.queryAndAggregateUsageStats(startTime, endTime)
            val instagramStats = stats[INSTAGRAM_PACKAGE]
            instagramStats?.totalTimeInForeground ?: 0L
        } catch (e: SecurityException) {
            Log.e(LOG_TAG, "Security exception - Usage Stats permission not granted", e)
            0L
        }
    }
}