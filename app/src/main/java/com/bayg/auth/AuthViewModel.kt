package com.bayg.auth

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bayg.services.storage.Authenticator
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AuthRepository(application)

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var signUpSucceeded by mutableStateOf(false)
        private set

    var signInNeedsVerification by mutableStateOf(false)
        private set

    var signInSucceeded by mutableStateOf(false)
        private set

    var emailVerified by mutableStateOf(false)
        private set

    private var lastResendMs by mutableLongStateOf(0L)

    val currentUserEmail: String? get() = repository.currentUserEmail

    fun clearError() {
        errorMessage = null
    }

    fun resetSignUpState() {
        signUpSucceeded = false
    }

    fun resetSignInState() {
        signInSucceeded = false
        signInNeedsVerification = false
    }

    fun canResendVerification(): Boolean {
        return System.currentTimeMillis() - lastResendMs >= RESEND_COOLDOWN_MS
    }

    fun signUp(name: String, email: String, password: String) {
        if (isLoading) return
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            signUpSucceeded = false

            repository.signUp(name, email, password)
                .onSuccess { signUpSucceeded = true }
                .onFailure { error ->
                    errorMessage = when (error) {
                        is IllegalArgumentException -> error.message
                        else -> Authenticator.mapSignUpError(error)
                    }
                }

            isLoading = false
        }
    }

    fun signIn(email: String, password: String) {
        if (isLoading) return
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            signInSucceeded = false
            signInNeedsVerification = false

            repository.signIn(email, password)
                .onSuccess { result ->
                    when (result) {
                        SignInResult.Verified -> signInSucceeded = true
                        SignInResult.NeedsVerification -> signInNeedsVerification = true
                    }
                }
                .onFailure { error ->
                    errorMessage = when (error) {
                        is IllegalArgumentException -> error.message
                        else -> Authenticator.mapSignInError(error)
                    }
                }

            isLoading = false
        }
    }

    fun resendVerification() {
        if (isLoading || !canResendVerification()) return
        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            repository.resendVerificationEmail()
                .onSuccess { lastResendMs = System.currentTimeMillis() }
                .onFailure { error ->
                    errorMessage = error.message ?: "Could not resend email. Try again."
                }

            isLoading = false
        }
    }

    fun checkEmailVerified() {
        if (isLoading) return
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            emailVerified = false

            val verified = repository.refreshEmailVerified()
            if (verified) {
                emailVerified = true
            } else {
                errorMessage = "Email not verified yet. Open the link we sent you, then try again."
            }

            isLoading = false
        }
    }

    companion object {
        private const val RESEND_COOLDOWN_MS = 60_000L
    }
}
