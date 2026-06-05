package com.bayg.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import bayg
import com.bayg.auth.AuthViewModel
import com.bayg.widgets.Caption
import com.bayg.widgets.GreenButton
import com.bayg.widgets.Heading1
import com.bayg.widgets.Heading3
import com.bayg.widgets.ProgressBar
import com.bayg.widgets.Subtitle

private const val PROGRESS_BAR_34_PERCENT = 0.33f

@Composable
fun VerifyEmail(navController: NavController, authViewModel: AuthViewModel = viewModel()) {
    val email = authViewModel.currentUserEmail ?: "your email"

    LaunchedEffect(authViewModel.emailVerified) {
        if (authViewModel.emailVerified) {
            navController.navigate("permissions") {
                popUpTo("verifyEmail") { inclusive = true }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.bayg.black)
            .padding(40.dp, 50.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Column(modifier = Modifier.width(334.dp)) {
            Caption("Step 1 of 3")
            ProgressBar(MaterialTheme.bayg.green, PROGRESS_BAR_34_PERCENT)
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Heading1("CRASH\nOUT.", MaterialTheme.bayg.green)
            Heading3("check your email", MaterialTheme.bayg.white)
            Subtitle("We sent a verification link to $email. Tap it, then come back here.")
        }

        authViewModel.errorMessage?.let { message ->
            Text(message, color = MaterialTheme.bayg.lightRed)
        }

        GreenButton(
            onClick = { authViewModel.checkEmailVerified() },
            text = if (authViewModel.isLoading) "Checking..." else "I've verified my email",
            color = MaterialTheme.bayg.white,
            enabled = !authViewModel.isLoading,
        )

        TextButton(
            onClick = { authViewModel.resendVerification() },
            enabled = authViewModel.canResendVerification() && !authViewModel.isLoading,
        ) {
            Text(
                if (authViewModel.canResendVerification()) {
                    "Resend verification email"
                } else {
                    "Resend available in a minute"
                },
                color = MaterialTheme.bayg.textGrey,
            )
        }
    }
}
