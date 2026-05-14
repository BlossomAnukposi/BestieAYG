package com.bayg.services

import android.app.Service
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.bayg.managers.AppUsageManager
import androidx.core.content.edit

class BackgroundService : Service() {

    private lateinit var appUsageManager: AppUsageManager
    private lateinit var sharedPreferences: SharedPreferences

    companion object {
        private const val PREFS_NAME = "app_usage_prefs"
        private const val KEY_START_TIME = "start_time"
    }

    override fun onCreate() {
        super.onCreate()
        appUsageManager = AppUsageManager(this)
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Set startTime if not already set (first time user "accepts" usage tracking)
        if (!sharedPreferences.contains(KEY_START_TIME)) {
            sharedPreferences.edit {
                putLong(KEY_START_TIME, System.currentTimeMillis())
            }
            Log.d("BackgroundService", "Start time initialized")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, "location_channel")
            .setContentTitle("App is running")
            .setContentText("Tracking app usage...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()

        startForeground(1, notification)

        // Get stored startTime (when user accepted usage tracking)
        val startTime = sharedPreferences.getLong(KEY_START_TIME, System.currentTimeMillis())
        // endTime is current time (when fetched for dashboard/logic)
        val endTime = System.currentTimeMillis()

        val instagramTime = appUsageManager.getInstagramUsageTime(startTime, endTime)
        val hours = instagramTime / (1000.0 * 60 * 60) // Convert to hours (use Double for precision)

        Log.d("BackgroundService", "Instagram usage since start: $hours hours")

        // TODO: Do something with the data (save to database, send to dashboard, etc.)
        // For example: saveToDatabase(instagramTime, startTime, endTime)

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "location_channel",
                "Location Service",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?) = null
}