package com.bayg

import BAYGTheme
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
import com.bayg.permissions.PermissionManager
import com.bayg.screens.AppSetup
import com.bayg.screens.Dashboard
import com.bayg.screens.Login
import com.bayg.screens.OnboardingStart
import com.bayg.screens.Permissions
import com.bayg.screens.ProfileSettings
import com.bayg.screens.SignUp
import com.bayg.screens.VerifyEmail
import com.bayg.services.storage.sync.SyncWorker
import com.bayg.screens.Stats

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
            scheduleScreenTimeWorker()
        }
    }

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
        ScreenTimeWorker.schedule(this)

        if (!permissionManager.hasAccessibilityPermission()) {
            permissionManager.requestAccessibilityPermission()
        }
    }

    private fun onAccessibilityPermissionGranted() {
        // Instagram blocker is now active — nothing else needed
    }
}