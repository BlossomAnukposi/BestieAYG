package com.bayg.managers

import android.Manifest
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import com.bayg.ScreenTimeWorker
import com.bayg.services.InstagramBlockerService

class PermissionManager(private val activity: ComponentActivity) {

    private var locationPermissionLauncher: ActivityResultLauncher<Array<String>>? = null
    private var usageStatsLauncher: ActivityResultLauncher<Intent>? = null
    private var notificationsPermissionLauncher: ActivityResultLauncher<String>? = null
    private var accessibilityLauncher: ActivityResultLauncher<Intent>? = null  // NEW

    companion object {
        private const val PERMISSION_CHECK_WINDOW_MS = 24 * 60 * 60 * 1000L
        private const val LOG_TAG = "PermissionManager"
    }

    fun initialize(
        notificationsPermissionLauncher: ActivityResultLauncher<String>,
        locationPermissionLauncher: ActivityResultLauncher<Array<String>>,
        usageStatsLauncher: ActivityResultLauncher<Intent>,
        accessibilityLauncher: ActivityResultLauncher<Intent>? = null  // NEW (optional so existing callers don't break)
    ) {
        this.notificationsPermissionLauncher = notificationsPermissionLauncher
        this.locationPermissionLauncher = locationPermissionLauncher
        this.usageStatsLauncher = usageStatsLauncher
        this.accessibilityLauncher = accessibilityLauncher
    }

    fun requestLocationPermissions() {
        if (hasLocationPermission()) {
            openLocationSettings()
        } else {
            locationPermissionLauncher?.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            ) ?: activity.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", activity.packageName, null)
                }
            )
        }
    }

    fun openLocationSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", activity.packageName, null)
        }
        activity.startActivity(intent)
    }

    fun requestUsageStatsPermission() {
        if (hasUsageStatsPermission()) {
            openUsageStatsSettings()
        } else {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            usageStatsLauncher?.launch(intent)
                ?: activity.startActivity(intent)
        }
    }

    private fun openUsageStatsSettings() {
        activity.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }

    // NEW — sends user to Accessibility Settings to enable the blocker service
    fun requestAccessibilityPermission() {
        if (hasAccessibilityPermission()) {
            openAccessibilitySettings()
        } else {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            accessibilityLauncher?.launch(intent)
                ?: activity.startActivity(intent) // fallback if launcher not registered
        }
    }

    private fun openAccessibilitySettings() {
        activity.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun openNotificationSettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", activity.packageName, null)
            }
        }
        activity.startActivity(intent)
    }

    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasUsageStatsPermission(): Boolean {
        return try {
            val usageStatsManager =
                activity.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val currentTime = System.currentTimeMillis()
            val stats = usageStatsManager.queryAndAggregateUsageStats(
                currentTime - PERMISSION_CHECK_WINDOW_MS,
                currentTime
            )
            stats.isNotEmpty()
        } catch (e: SecurityException) {
            Log.e(LOG_TAG, "Security exception - Usage Stats permission not granted", e)
            false
        }
    }

    // NEW — checks if the AccessibilityService is enabled in device settings
    fun hasAccessibilityPermission(): Boolean {
        return InstagramBlockerService.Companion.isEnabled(activity)
    }

    // NEW — true only when ALL permissions required for blocking are granted
    fun hasAllBlockingPermissions(): Boolean {
        return hasUsageStatsPermission() && hasAccessibilityPermission()
    }

    fun requestNotificationsPermission() {
        if (hasNotificationsPermission()) {
            openNotificationSettings()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationsPermissionLauncher?.launch(Manifest.permission.POST_NOTIFICATIONS)
                ?: openNotificationSettings() // fallback if launcher not registered
        }
    }

    fun hasNotificationsPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            // On Android 12 and lower, notification permission is not required
            return true
        }
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }
}