package com.bayg.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class PermissionManager(private val activity: AppCompatActivity) {

    private var locationPermissionLauncher: ActivityResultLauncher<Array<String>>? = null
    private var usageStatsLauncher: ActivityResultLauncher<Intent>? = null

    fun initialize(
        locationPermissionLauncher: ActivityResultLauncher<Array<String>>,
        usageStatsLauncher: ActivityResultLauncher<Intent>
    ) {
        this.locationPermissionLauncher = locationPermissionLauncher
        this.usageStatsLauncher = usageStatsLauncher
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
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        usageStatsLauncher?.launch(intent)
    }

    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasUsageStatsPermission(): Boolean {
        // This is a special permission, must be checked manually
        return try {
            val usageStatsManager = activity.getSystemService(Context.USAGE_STATS_SERVICE) as android.app.usage.UsageStatsManager
            val stats = usageStatsManager.queryAndAggregateUsageStats(System.currentTimeMillis() - 1000, System.currentTimeMillis())
            stats.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
}