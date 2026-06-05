package com.bayg.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import bayg
import com.bayg.auth.AuthViewModel
import com.bayg.widgets.BaygOutlinedTextField
import com.bayg.widgets.GreenArrowButton
import com.bayg.widgets.GreenButton
import com.bayg.widgets.Heading1
import com.bayg.widgets.Heading3
import com.bayg.widgets.Subtitle

@Composable
fun Login(navController: NavController, authViewModel: AuthViewModel = viewModel()) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(authViewModel.signInSucceeded, authViewModel.signInNeedsVerification) {
        when {
            authViewModel.signInSucceeded -> {
                authViewModel.resetSignInState()
                navController.navigate("dashboard") {
                    popUpTo("onboardingStart") { inclusive = true }
                }
            }
            authViewModel.signInNeedsVerification -> {
                authViewModel.resetSignInState()
                navController.navigate("verifyEmail") {
                    popUpTo("login") { inclusive = false }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.bayg.black)
            .verticalScroll(rememberScrollState())
            .padding(40.dp, 50.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        GreenArrowButton(navController, "signUp")

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Heading1("CRASH\nOUT.", MaterialTheme.bayg.green)
            Heading3("log in", MaterialTheme.bayg.white)
            Subtitle("welcome back. no excuses today.")
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BaygOutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    authViewModel.clearError()
                },
                placeholder = "Enter your email address",
                prefix = "@",
                keyboardType = KeyboardType.Email,
            )
            BaygOutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    authViewModel.clearError()
                },
                placeholder = "Enter your password",
                prefix = "*",
                isPassword = true,
            )
        }

        authViewModel.errorMessage?.let { message ->
            Text(message, color = MaterialTheme.bayg.lightRed)
        }

        GreenButton(
            onClick = { authViewModel.signIn(email, password) },
            text = if (authViewModel.isLoading) "Signing in..." else "Log in",
            enabled = !authViewModel.isLoading,
        )

        TextButton(onClick = { navController.navigate("signUp") }) {
            Text("New here? Create an account", color = MaterialTheme.bayg.textGrey)
        }
    }
}
