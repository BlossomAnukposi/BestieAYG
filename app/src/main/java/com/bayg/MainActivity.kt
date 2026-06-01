package com.bayg

import android.annotation.SuppressLint
import android.content.Intent
import BAYGTheme
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
            permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false -> {
                onLocationPermissionGranted()
            }
            permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false -> {
                onLocationPermissionGranted()
            }
            else -> { /* denied */ }
        }
    }

    private val usageStatsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        if (permissionManager.hasUsageStatsPermission()) {
            onUsageStatsPermissionGranted()
        }
    }

    // NEW — launcher for Accessibility Settings
    // When the user returns from the settings screen we check if they enabled it.
    private val accessibilityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        if (permissionManager.hasAccessibilityPermission()) {
            onAccessibilityPermissionGranted()
        }
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        permissionManager = PermissionManager(this)
        // Pass all three launchers — accessibilityLauncher is the new addition
        permissionManager.initialize(
            locationPermissionLauncher,
            usageStatsLauncher,
            accessibilityLauncher   // NEW
        )

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

    private fun onLocationPermissionGranted() {
        // Location logic — unchanged
    }

    private fun onUsageStatsPermissionGranted() {
        // Now that usage stats are granted, nudge user to also enable accessibility
        if (!permissionManager.hasAccessibilityPermission()) {
            permissionManager.requestAccessibilityPermission()
        }
    }

    // NEW
    private fun onAccessibilityPermissionGranted() {
        // Both permissions are now active — the InstagramBlockerService
        // will automatically start monitoring. Nothing else to do here.
    }
}