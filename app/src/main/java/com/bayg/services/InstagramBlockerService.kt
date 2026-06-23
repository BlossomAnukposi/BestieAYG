package com.bayg.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import com.bayg.BlockedActivity
import com.bayg.UsageTracker

/**
 * InstagramBlockerService
 *
 * An AccessibilityService that fires whenever any window changes
 * on the device. When it detects Instagram coming to the foreground
 * and the user has exceeded their daily limit, it immediately launches
 * BlockedActivity on top, effectively locking them out of the app.
 *
 * The window-state-change callback only fires on transitions *into*
 * Instagram — it does nothing while the user is already inside the
 * app. To catch the case where the user crosses the limit mid-session,
 * we also schedule a Handler-driven poll every [POLL_INTERVAL_MS]
 * that re-checks UsageTracker. The poll path is throttled by
 * [MIN_RELAUNCH_GAP_MS] so it can't stack BlockedActivity instances
 * on top of each other; the window-event path is intentionally
 * unthrottled so a user who dismisses the block and re-opens IG
 * is re-blocked immediately.
 *
 * Registration: declared in AndroidManifest.xml and res/xml/accessibility_service_config.xml
 * Enabling: the user must manually enable it in Settings → Accessibility.
 */
class InstagramBlockerService : AccessibilityService() {
    private enum class LaunchReason { WINDOW_EVENT, POLL }

    private val handler = Handler(Looper.getMainLooper())
    private var lastBlockLaunchedMs = 0L
    private var pollScheduled = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            notificationTimeout = CHECK_INTERVAL_MS
        }
        Log.i(LOG_TAG, "service connected")
        if (!pollScheduled) {
            pollScheduled = true
            handler.postDelayed(::runPollCycle, POLL_INTERVAL_MS)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return
        if (packageName != UsageTracker.INSTAGRAM_PACKAGE) return

        val usedMs = UsageTracker.getTodayUsageMs(this)
        val limitMs = UsageTracker.getDailyLimitMs(this)
        val exceeded = usedMs >= limitMs

        if (exceeded) {
            launchBlockScreen(LaunchReason.WINDOW_EVENT)
        }
    }

    private fun runPollCycle() {
        val usedMs = UsageTracker.getTodayUsageMs(this)
        val limitMs = UsageTracker.getDailyLimitMs(this)
        val exceeded = usedMs >= limitMs
        if (exceeded) launchBlockScreen(LaunchReason.POLL)
        handler.postDelayed(::runPollCycle, POLL_INTERVAL_MS)
    }

    /**
     * Launch the BlockedActivity takeover. The poll path is throttled by
     * [MIN_RELAUNCH_GAP_MS] to prevent stacking instances; the
     * window-event path is intentionally unthrottled so a user who
     * dismisses the block and re-opens IG immediately is re-blocked.
     */
    private fun launchBlockScreen(reason: LaunchReason) {
        val now = System.currentTimeMillis()
        val sinceLast = now - lastBlockLaunchedMs
        if (reason == LaunchReason.POLL &&
            lastBlockLaunchedMs > 0 &&
            sinceLast < MIN_RELAUNCH_GAP_MS
        ) {
            Log.i(LOG_TAG, "poll-skip")
            return
        }
        Log.i(LOG_TAG, "launching BlockedActivity")
        lastBlockLaunchedMs = now
        val intent = Intent(this, BlockedActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        }
        startActivity(intent)
    }

    override fun onInterrupt() { /* ignore */ }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        pollScheduled = false
        lastBlockLaunchedMs = 0L
        super.onDestroy()
    }

    companion object {
        private const val CHECK_INTERVAL_MS = 100L
        private const val LOG_TAG = "InstagramBlocker"
        private const val POLL_INTERVAL_MS = 60_000L
        private const val MIN_RELAUNCH_GAP_MS = 5L * 60_000L

        /**
         * Returns true if the Accessibility Service is currently enabled
         * in the device's Accessibility Settings.
         */
        fun isEnabled(context: Context): Boolean {
            val expected = ComponentName(
                context,
                InstagramBlockerService::class.java
            )

            val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE)
                    as AccessibilityManager

            return manager
                .getEnabledAccessibilityServiceList(
                    AccessibilityServiceInfo.FEEDBACK_ALL_MASK
                )
                .any {
                    it.resolveInfo.serviceInfo.packageName == expected.packageName &&
                            it.resolveInfo.serviceInfo.name == expected.className
                }
        }
    }
}
