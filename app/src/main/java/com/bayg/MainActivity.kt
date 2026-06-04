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
import com.bayg.services.storage.sync.SyncWorker

class MainActivity : ComponentActivity() {

    private lateinit var permissionManager: PermissionManager

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false -> {
                // Fine location granted
                onLocationPermissionGranted()
            }
            permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false -> {
                // Coarse location granted
                onLocationPermissionGranted()
            }
            else -> {
                // Permission denied
            }
        }
    }

    private val usageStatsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
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

                NavHost(navController = navController, startDestination = "dashboard") {
                    composable("onboardingStart") { OnboardingStart(navController) }
                    composable("signIn") { SignIn(navController) }
                    composable("permissions") { Permissions(navController, permissionManager) }
                    composable("appSetup") { AppSetup(navController) }
                    composable("dashboard") { Dashboard(navController) }
                    composable("settings") { ProfileSettings(navController) }
                }
            }
        }

//        findViewById<androidx.cardview.widget.CardView>(R.id.tile_touch_grass)
//            .setOnClickListener {
//                startActivity(Intent(this, TouchGrassActivity::class.java))
//            }
    }

    private fun onLocationPermissionGranted() {
        // Location logic will be added later
    }

    private fun onUsageStatsPermissionGranted() {
        // Usage stats logic will be added later
    }
}