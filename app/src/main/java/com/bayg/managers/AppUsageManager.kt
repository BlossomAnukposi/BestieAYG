package com.bayg.managers

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.util.Log

class AppUsageManager(private val context: Context) {

    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    companion object {
        private const val INSTAGRAM_PACKAGE = "com.instagram.android"
        private const val LOG_TAG = "AppUsageManager"
        private const val DAY_MS = 24L * 60L * 60L * 1000L
    }

    /**
     * Total foreground time Instagram was used within the [startTime, endTime]
     * window.
     *
     * Implementation note: [UsageStatsManager.queryAndAggregateUsageStats]
     * can overcount events that span the queried window — e.g. an IG session
     * that opened yesterday evening and was still active when today began
     * gets the whole session attributed to today's bucket instead of being
     * split across midnight. We pull raw `UsageEvents` and re-derive
     * `totalTimeInForeground` with each foreground interval clamped to the
     * requested window. A 24-hour pre-roll buffer captures a foreground event
     * whose start happened before [startTime].
     */
    fun getInstagramUsageTime(startTime: Long, endTime: Long): Long {
        return try {
            val paddedStart = startTime - DAY_MS
            val events = usageStatsManager.queryEvents(paddedStart, endTime)
                ?: return 0L

            var totalForegroundMs = 0L
            var sessionStart: Long? = null
            val event = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.packageName != INSTAGRAM_PACKAGE) continue
                when (event.eventType) {
                    UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                        sessionStart = event.timeStamp
                    }
                    UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                        val start = sessionStart ?: continue
                        val clampedStart = maxOf(start, startTime)
                        val clampedEnd = minOf(event.timeStamp, endTime)
                        if (clampedEnd > clampedStart) {
                            totalForegroundMs += clampedEnd - clampedStart
                        }
                        sessionStart = null
                    }
                }
            }
            // IG is still in the foreground at endTime (callers clamp
            // endTime to System.currentTimeMillis via dayMinutes). Credit
            // the active session up to endTime so a currently-running
            // session isn't dropped on the floor.
            sessionStart?.let { start ->
                val clampedStart = maxOf(start, startTime)
                if (endTime > clampedStart) {
                    totalForegroundMs += endTime - clampedStart
                }
            }
            totalForegroundMs
        } catch (e: SecurityException) {
            Log.e(LOG_TAG, "Security exception - Usage Stats permission not granted", e)
            0L
        }
    }
}
