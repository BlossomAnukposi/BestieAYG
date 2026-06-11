package com.bayg.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.app.usage.UsageStatsManager
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import android.util.Log
import androidx.activity.ComponentActivity
import com.bayg.services.InstagramBlockerService

class PermissionManager(private val activity: ComponentActivity) {

    private var locationPermissionLauncher: ActivityResultLauncher<Array<String>>? = null
    private var usageStatsLauncher: ActivityResultLauncher<Intent>? = null
    private var accessibilityLauncher: ActivityResultLauncher<Intent>? = null  // NEW

    companion object {
        private const val PERMISSION_CHECK_WINDOW_MS = 1000L
        private const val LOG_TAG = "PermissionManager"
    }

    fun initialize(
        locationPermissionLauncher: ActivityResultLauncher<Array<String>>,
        usageStatsLauncher: ActivityResultLauncher<Intent>,
        accessibilityLauncher: ActivityResultLauncher<Intent>? = null  // NEW (optional so existing callers don't break)
    ) {
        this.locationPermissionLauncher = locationPermissionLauncher
        this.usageStatsLauncher = usageStatsLauncher
        this.accessibilityLauncher = accessibilityLauncher
    }

    fun requestLocationPermissions() {
        if (!hasLocationPermission()) {
            locationPermissionLauncher?.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    fun requestUsageStatsPermission() {
        if (!hasUsageStatsPermission()) {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            usageStatsLauncher?.launch(intent)
        }
    }

    // NEW — sends user to Accessibility Settings to enable the blocker service
    fun requestAccessibilityPermission() {
        if (!hasAccessibilityPermission()) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            accessibilityLauncher?.launch(intent)
                ?: activity.startActivity(intent) // fallback if launcher not registered
        }
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
        return InstagramBlockerService.isEnabled(activity)
    }

    // NEW — true only when ALL permissions required for blocking are granted
    fun hasAllBlockingPermissions(): Boolean {
        return hasUsageStatsPermission() && hasAccessibilityPermission()
    }
}