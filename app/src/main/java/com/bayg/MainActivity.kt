package com.bayg

import android.annotation.SuppressLint
import android.os.Bundle
import BAYGTheme
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
import com.google.firebase.auth.FirebaseAuth

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

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        permissionManager = PermissionManager(this)
        permissionManager.initialize(locationPermissionLauncher, usageStatsLauncher)

        SyncWorker.schedule(this)
        SyncWorker.runOnce(this)

        setContent {
            BAYGTheme {
                val navController = rememberNavController()
                var startDestination by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(Unit) {
                    startDestination = resolveStartDestination()
                }

                if (startDestination != null) {
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
                    }
                }
            }
        }
    }

    private fun resolveStartDestination(): String {
        val user = FirebaseAuth.getInstance().currentUser
        return when {
            user == null -> "onboardingStart"
            !user.isEmailVerified -> "verifyEmail"
            else -> "dashboard"
        }
    }

    private fun onLocationPermissionGranted() {
        // Location logic will be added later
    }

    private fun onUsageStatsPermissionGranted() {
        // Usage stats logic will be added later
    }
}
