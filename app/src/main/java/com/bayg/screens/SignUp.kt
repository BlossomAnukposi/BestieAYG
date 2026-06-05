package com.bayg.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import bayg
import com.bayg.R
import com.bayg.auth.AuthViewModel
import com.bayg.widgets.BaygOutlinedTextField
import com.bayg.widgets.Caption
import com.bayg.widgets.GreenArrowButton
import com.bayg.widgets.GreenButton
import com.bayg.widgets.GreyOutlinedCard
import com.bayg.widgets.Heading1
import com.bayg.widgets.Heading3
import com.bayg.widgets.Paragraph
import com.bayg.widgets.ProgressBar
import com.bayg.widgets.Subtitle

private const val PROGRESS_BAR_34_PERCENT = 0.33f

@Composable
fun SignUp(navController: NavController, authViewModel: AuthViewModel = viewModel()) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(authViewModel.signUpSucceeded) {
        if (authViewModel.signUpSucceeded) {
            authViewModel.resetSignUpState()
            navController.navigate("verifyEmail") {
                popUpTo("signUp") { inclusive = false }
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
        Column {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.width(334.dp).padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GreenArrowButton(navController, "onboardingStart")
                Caption("Step 1 of 3")
            }
            ProgressBar(MaterialTheme.bayg.green, PROGRESS_BAR_34_PERCENT)
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Heading1("CRASH\nOUT.", MaterialTheme.bayg.green)
            Heading3("sign up", MaterialTheme.bayg.white)
            Subtitle("we need to know who you are before we snitch on you")
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BaygOutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    authViewModel.clearError()
                },
                placeholder = "Enter your name",
                prefix = "Aa",
            )
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
                placeholder = "Set a new password",
                prefix = "*",
                isPassword = true,
            )
        }

        GreyOutlinedCard {
            Row {
                Image(
                    painter = painterResource(id = R.drawable.security),
                    contentDescription = "Security icon",
                    modifier = Modifier.size(60.dp),
                )
                Column {
                    Paragraph("Firebase Auth + email verification", MaterialTheme.bayg.white, true)
                    Caption("Your password never leaves Firebase. We pinky promise.")
                }
            }
        }

        authViewModel.errorMessage?.let { message ->
            Text(message, color = MaterialTheme.bayg.lightRed)
        }

        GreenButton(
            onClick = { authViewModel.signUp(name, email, password) },
            text = if (authViewModel.isLoading) "Creating account..." else "Next",
            color = MaterialTheme.bayg.white,
            enabled = !authViewModel.isLoading,
        )

        TextButton(onClick = { navController.navigate("login") }) {
            Text("Already have an account? Log in", color = MaterialTheme.bayg.textGrey)
        }
    }
}
