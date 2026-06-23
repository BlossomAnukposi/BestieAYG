package com.bayg

import BAYGTheme
import android.annotation.SuppressLint
import android.content.Context
import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bayg.auth.AuthNavigation
import com.bayg.auth.BiometricUnlockGate
import com.bayg.auth.requiresBiometricUnlock
import com.bayg.managers.PermissionManager
import com.bayg.ui.screens.AppSetup
import com.bayg.ui.screens.Dashboard
import com.bayg.ui.screens.Login
import com.bayg.ui.screens.OnboardingStart
import com.bayg.ui.screens.Permissions
import com.bayg.ui.screens.ProfileSettings
import com.bayg.ui.screens.SignUp
import com.bayg.ui.screens.VerifyEmail
import com.bayg.services.storage.sync.SyncWorker
import com.bayg.ui.screens.Stats

class MainActivity : FragmentActivity() {

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
            else -> {
                // Permission denied
            }
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
            scheduleScreenTimeWorker(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        permissionManager = PermissionManager(this)
        permissionManager.initialize(
            notificationPermissionLauncher,
            locationPermissionLauncher,
            usageStatsLauncher,
            accessibilityLauncher
        )

        // Register notification channel early (no-op if already exists)
        TouchGrassNotifier.createChannel(this)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            scheduleScreenTimeWorker(this)
        }

        SyncWorker.schedule(this)
        SyncWorker.runOnce(this)

        setContent {
            BAYGTheme {
                val navController = rememberNavController()
                var startDestination by remember { mutableStateOf<String?>(null) }
                var biometricUnlocked by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    startDestination = AuthNavigation.resolveStartDestination(this@MainActivity)
                    if (!requiresBiometricUnlock(this@MainActivity)) {
                        biometricUnlocked = true
                    }
                }

                when {
                    startDestination == null -> Unit
                    requiresBiometricUnlock(this@MainActivity) && !biometricUnlocked -> {
                        BiometricUnlockGate(
                            activity = this@MainActivity,
                            onUnlocked = { biometricUnlocked = true },
                        )
                    }
                    else -> {
                        NavHost(
                            navController = navController,
                            startDestination = startDestination!!,
                        ) {
                            composable("onboardingStart") { OnboardingStart(navController) }
                            composable("signUp") { SignUp(navController) }
                            composable("login") { Login(navController) }
                            composable("verifyEmail") { VerifyEmail(navController) }
                            composable("permissions") { Permissions(navController, permissionManager) }
                            composable("appSetup") { AppSetup(navController) }
                            composable("dashboard") { Dashboard(navController) }
                            composable("settings") { ProfileSettings(navController) }
                            composable("stats") { Stats(navController) }
                        }
                    }
                }
            }
        }
    }

    private fun scheduleScreenTimeWorker(context: Context) {
        if (permissionManager.hasUsageStatsPermission()) {
            ScreenTimeWorker.schedule(context)
        }
        // If usage stats aren't granted yet, schedule() is called again
        // inside onUsageStatsPermissionGranted() below.
    }

    private fun onLocationPermissionGranted() {
        // Location logic will be added later
    }

    private fun onUsageStatsPermissionGranted() {
        ScreenTimeWorker.schedule(this)

        if (!permissionManager.hasAccessibilityPermission()) {
            permissionManager.requestAccessibilityPermission()
        }
    }

    private fun onAccessibilityPermissionGranted() {
        // Instagram blocker is now active — nothing else needed
    }
}