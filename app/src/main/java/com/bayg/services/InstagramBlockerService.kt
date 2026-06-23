package com.bayg.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
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

    /** Why `launchBlockScreen` was invoked. Only the poll path is throttled. */
    private enum class LaunchReason { WINDOW_EVENT, POLL }

    private val handler = Handler(Looper.getMainLooper())
    private var lastBlockLaunchedMs = 0L
    private var pollScheduled = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Configure the service to listen for window state changes only —
        // this is the most battery-efficient approach.
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

        // Only act when Instagram comes to the foreground
        if (packageName != UsageTracker.INSTAGRAM_PACKAGE) return

        // Read these once so the diagnostic log matches the decision below
        // and we don't make duplicate IPC calls to UsageStatsManager.
        val usedMs = UsageTracker.getTodayUsageMs(this)
        val limitMs = UsageTracker.getDailyLimitMs(this)
        val exceeded = usedMs >= limitMs

        Log.i(LOG_TAG, "instagram-foreground: exceeded=$exceeded used=${usedMs}ms limit=${limitMs}ms")

        if (exceeded) {
            launchBlockScreen(LaunchReason.WINDOW_EVENT)
        }
    }

    private fun runPollCycle() {
        // Read these once so the diagnostic log matches the decision below
        // and we don't make duplicate IPC calls to UsageStatsManager.
        val usedMs = UsageTracker.getTodayUsageMs(this)
        val limitMs = UsageTracker.getDailyLimitMs(this)
        val exceeded = usedMs >= limitMs
        Log.i(LOG_TAG, "poll: exceeded=$exceeded used=${usedMs}ms limit=${limitMs}ms")
        if (exceeded) launchBlockScreen(LaunchReason.POLL)
        // Re-arm unconditionally; cancellation lives in onServiceConnected
        // (via pollScheduled) and onDestroy (via removeCallbacksAndMessages).
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
            Log.i(LOG_TAG, "poll-skip: last launch ${sinceLast}ms ago")
            return
        }
        Log.i(LOG_TAG, "launching BlockedActivity reason=$reason")
        lastBlockLaunchedMs = now
        val intent = Intent(this, BlockedActivity::class.java).apply {
            // NEW_TASK is required when starting an Activity from a Service
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            // Clear the back stack so pressing Back doesn't return to Instagram
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            // Don't animate — the transition should feel instant and jarring
            addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        }
        startActivity(intent)
    }

    override fun onInterrupt() {
        // Required override — called when the service is interrupted.
        // Nothing to clean up here.
    }

    override fun onDestroy() {
        // Belt-and-braces cleanup: if the OS reclaims the service while
        // we still have a poll Runnable queued, drop it so it can't fire
        // against a stale state on a future bind.
        handler.removeCallbacksAndMessages(null)
        pollScheduled = false
        lastBlockLaunchedMs = 0L
        super.onDestroy()
    }

    companion object {
        // Minimum ms between event callbacks — keeps CPU usage low
        private const val CHECK_INTERVAL_MS = 100L

        // Greppable log tag. Filter via `adb logcat -s InstagramBlocker:I`.
        private const val LOG_TAG = "InstagramBlocker"

        // How often the in-service poll re-checks the daily limit while
        // the user is already inside Instagram (where window-state events
        // don't fire). One UsageStats IPC per minute — well within budget.
        private const val POLL_INTERVAL_MS = 60_000L

        // Minimum gap between two poll-driven block launches. Long enough
        // to swallow the "poll-fires-right-after-window-event" race;
        // short enough that re-opening IG within the gap is caught by the
        // window-event path (which is unthrottled).
        private const val MIN_RELAUNCH_GAP_MS = 5L * 60_000L

        /**
         * Returns true if the Accessibility Service is currently enabled
         * in the device's Accessibility Settings.
         */
        fun isEnabled(context: Context): Boolean {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            val componentName = "${context.packageName}/.services.InstagramBlockerService"
            return enabledServices.contains(componentName)
        }
    }
}
