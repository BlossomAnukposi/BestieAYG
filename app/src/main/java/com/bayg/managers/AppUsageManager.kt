package com.bayg.managers

import android.app.usage.UsageStatsManager
import android.content.Context
import android.util.Log
import java.util.concurrent.TimeUnit

class AppUsageManager(private val context: Context) {

    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    companion object {
        private const val INSTAGRAM_PACKAGE = "com.instagram.android"
        private const val LOG_TAG = "AppUsageManager"
    }

    /**
     * Gets the total foreground time Instagram was used between startTime and endTime
     * @param startTime Start time in milliseconds (e.g., System.currentTimeMillis() - 24*60*60*1000 for last 24 hours)
     * @param endTime End time in milliseconds (e.g., System.currentTimeMillis())
     * @return Total time in milliseconds, or 0L if permission not granted or error
     */
//    TODO: The starttime starts when the user accepted the usage of the app.
//    TODO: The endtime is when the it is being fetched onto the dahsboard or whenever th elogic is being called.

    fun getInstagramUsageTime(startTime: Long, endTime: Long): Long {
        return try {
            val stats = usageStatsManager.queryAndAggregateUsageStats(startTime, endTime)
            val instagramStats = stats["com.instagram.android"]
            instagramStats?.totalTimeInForeground ?: 0L
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error getting Instagram usage stats", e)
            0L
        }
    }
}