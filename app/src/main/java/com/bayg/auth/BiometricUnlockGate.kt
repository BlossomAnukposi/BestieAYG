package com.bayg.auth

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import bayg
import com.bayg.security.BiometricAuthManager
import com.bayg.widgets.GreenButton
import com.bayg.widgets.Heading3
import com.bayg.widgets.Subtitle
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

fun requiresBiometricUnlock(context: Context): Boolean {
    val user = FirebaseAuth.getInstance().currentUser ?: return false
    if (!user.isEmailVerified) return false
    if (!OnboardingStore.isComplete(context)) return false
    return BiometricAuthManager(context).canAuthenticate() == BiometricAuthManager.CanAuthenticate.Ok
}

@Composable
fun BiometricUnlockGate(
    activity: FragmentActivity,
    onUnlocked: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val biometricManager = remember { BiometricAuthManager(context) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    suspend fun runUnlock() {
        isLoading = true
        errorMessage = null
        val cipher = biometricManager.encryptCipher()
        when (val result = biometricManager.authenticate(activity, cipher)) {
            is BiometricAuthManager.AuthResult.Success -> onUnlocked()
            is BiometricAuthManager.AuthResult.UserCancelled -> {
                errorMessage = "Unlock cancelled. Try again to open the app."
            }
            is BiometricAuthManager.AuthResult.Failure -> {
                errorMessage = result.message
            }
        }
        isLoading = false
    }

    LaunchedEffect(Unit) {
        if (biometricManager.canAuthenticate() != BiometricAuthManager.CanAuthenticate.Ok) {
            onUnlocked()
            return@LaunchedEffect
        }
        runUnlock()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.bayg.black)
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
    ) {
        Heading3("unlock crashout", MaterialTheme.bayg.white)
        Subtitle("Use your fingerprint or face to continue")

        errorMessage?.let { message ->
            Text(message, color = MaterialTheme.bayg.lightRed)
        }

        GreenButton(
            onClick = { scope.launch { runUnlock() } },
            text = if (isLoading) "Waiting..." else "Try again",
            enabled = !isLoading,
        )
    }
}
