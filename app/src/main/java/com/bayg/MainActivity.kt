package com.bayg

import android.annotation.SuppressLint
import android.content.Intent
import BAYGTheme
import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bayg.screens.AppSetup
import com.bayg.screens.OnboardingStart
import com.bayg.screens.Permissions
import com.bayg.screens.SignIn
import com.bayg.screens.Dashboard
import com.bayg.screens.ProfileSettings
import androidx.activity.result.contract.ActivityResultContracts
import com.bayg.permissions.PermissionManager

class MainActivity : ComponentActivity() {

    private lateinit var permissionManager: PermissionManager

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false -> onLocationPermissionGranted()
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false -> onLocationPermissionGranted()
        }
    }

    private val usageStatsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        if (permissionManager.hasUsageStatsPermission()) {
            onUsageStatsPermissionGranted()
        }
    }

    private val accessibilityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        if (permissionManager.hasAccessibilityPermission()) {
            onAccessibilityPermissionGranted()
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            scheduleScreenTimeWorker()
        }
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        permissionManager = PermissionManager(this)
        permissionManager.initialize(
            locationPermissionLauncher,
            usageStatsLauncher,
            accessibilityLauncher
        )

        // Register notification channel early (no-op if already exists)
        TouchGrassNotifier.createChannel(this)

        // Request notification permission on Android 13+, then schedule worker
        requestNotificationPermissionAndSchedule()

        setContent {
            BAYGTheme {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "ProfileSettings") {
                    composable("onboardingStart") { OnboardingStart(navController) }
                    composable("signIn") { SignIn(navController) }
                    composable("permissions") { Permissions(navController, permissionManager) }
                    composable("appSetup") { AppSetup(navController) }
                    composable("dashboard") { Dashboard() }
                    composable("ProfileSettings") { ProfileSettings(navController) }
                }
            }
        }
    }

    private fun requestNotificationPermissionAndSchedule() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            scheduleScreenTimeWorker()
        }
    }

    private fun scheduleScreenTimeWorker() {
        if (permissionManager.hasUsageStatsPermission()) {
            ScreenTimeWorker.schedule(this)
        }
        // If usage stats aren't granted yet, schedule() is called again
        // inside onUsageStatsPermissionGranted() below.
    }

    private fun onLocationPermissionGranted() {
        // Location logic will be added later
    }

    private fun onUsageStatsPermissionGranted() {
        // Start the screen time worker now that we can read usage stats
        ScreenTimeWorker.schedule(this)

        if (!permissionManager.hasAccessibilityPermission()) {
            permissionManager.requestAccessibilityPermission()
        }
    }

    private fun onAccessibilityPermissionGranted() {
        // Instagram blocker is now active — nothing else needed
    }
}