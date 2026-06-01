package com.bayg

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

/**
 * InstagramBlockerService
 *
 * An AccessibilityService that fires whenever any window changes
 * on the device. When it detects Instagram coming to the foreground
 * and the user has exceeded their daily limit, it immediately launches
 * BlockedActivity on top, effectively locking them out of the app.
 *
 * Registration: declared in AndroidManifest.xml and res/xml/accessibility_service_config.xml
 * Enabling: the user must manually enable it in Settings → Accessibility.
 *
 * NOTE: This service runs in the background as long as it is enabled
 * in Accessibility settings — no additional lifecycle management needed.
 */
class InstagramBlockerService : AccessibilityService() {

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
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return

        // Only act when Instagram comes to the foreground
        if (packageName != UsageTracker.INSTAGRAM_PACKAGE) return

        // Check if they've hit their limit
        if (UsageTracker.isLimitExceeded(this)) {
            launchBlockScreen()
        }
    }

    private fun launchBlockScreen() {
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

    companion object {
        // Minimum ms between event callbacks — keeps CPU usage low
        private const val CHECK_INTERVAL_MS = 100L

        /**
         * Returns true if the Accessibility Service is currently enabled
         * in the device's Accessibility Settings.
         */
        fun isEnabled(context: android.content.Context): Boolean {
            val enabledServices = android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            val componentName = "${context.packageName}/.InstagramBlockerService"
            return enabledServices.contains(componentName)
        }
    }
}
