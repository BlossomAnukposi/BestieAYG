package com.bayg

import BAYGTheme
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bayg.screens.AppSetup
import com.bayg.screens.OnboardingStart
import com.bayg.screens.Permissions
import com.bayg.screens.SignIn

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            BAYGTheme {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "onboardingStart") {
                    composable("onboardingStart") {OnboardingStart(navController)}
                    composable("signIn") {SignIn(navController)}
                    composable("permissions") {Permissions(navController)}
                    composable("appSetup") {AppSetup(navController)}
                }
            }
        }
    }
}
