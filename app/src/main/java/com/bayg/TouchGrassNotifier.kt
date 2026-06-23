package com.bayg

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * TouchGrassNotifier
 *
 * Builds and fires a single "touch grass" notification.
 * Tapping it opens TouchGrassActivity directly.
 *
 * The notification channel is created lazily — safe to call
 * createChannel() multiple times (it's a no-op if already exists).
 */
object TouchGrassNotifier {

    private const val CHANNEL_ID = "touch_grass_channel"
    private const val CHANNEL_NAME = "Touch Grass Reminders"
    private const val CHANNEL_DESC = "Reminds you to go outside when you've been on your phone too long."
    private const val NOTIFICATION_ID = 1001

    private val MESSAGES = listOf(
        "You've been staring at your phone for a while. The outside world misses you. 🌿",
        "Screen time limit hit. A short walk counts as touching grass. 🌳",
        "Your phone will still be here later. Go breathe some real air. ☀️",
        "Skill issue: spending too long on your phone. Grass is the fix. 🍃",
        "Your eyes need a break. So does your brain. Outside time, now. 🌱"
    )

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Call once at app startup (e.g. in MainActivity.onCreate) to register
     * the notification channel on Android 8+. Safe to call repeatedly.
     */
    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESC
                enableVibration(true)
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    /**
     * Fire the touch grass notification. Checks for POST_NOTIFICATIONS
     * permission on Android 13+ before attempting to notify.
     */
    fun notify(context: Context) {
        val tapIntent = Intent(context, TouchGrassActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val usedMs = ScreenTimeChecker.getTodayTotalUsageMs(context)
        val usedFormatted = UsageTracker.formatDuration(usedMs)
        val message = MESSAGES.random()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Time to touch grass 🌿")
            .setContentText("$usedFormatted on your phone today.")
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // dismiss when tapped
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS not granted on Android 13+ — silently skip.
            // The WorkManager check in ScreenTimeWorker handles this gracefully.
        }
    }
}
