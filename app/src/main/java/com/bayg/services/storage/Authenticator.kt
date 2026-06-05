package com.bayg.services.storage

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.tasks.await

class Authenticator(private val auth: FirebaseAuth = FirebaseAuth.getInstance()) {
    val currentUser: FirebaseUser? get() = auth.currentUser
    val isSignedIn: Boolean get() = currentUser != null
    val isEmailVerified: Boolean get() = currentUser?.isEmailVerified == true

    suspend fun signUp(email: String, firstName: String, lastName: String, password: String): FirebaseUser {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val user = result.user ?: error("User creation returned null")

        val profileUpdate = UserProfileChangeRequest.Builder()
            .setDisplayName("$firstName $lastName".trim())
            .build()
        user.updateProfile(profileUpdate).await()

        return user
    }

    suspend fun signIn(email: String, password: String): FirebaseUser {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        return result.user ?: error("Sign in returned null user")
    }

    suspend fun sendEmailVerification() {
        val user = currentUser ?: error("Not signed in")
        user.sendEmailVerification().await()
    }

    suspend fun reloadUser(): FirebaseUser? {
        currentUser?.reload()?.await()
        return currentUser
    }

    fun signOut() = auth.signOut()

    suspend fun sendPasswordReset(email: String) {
        auth.sendPasswordResetEmail(email).await()
    }

    companion object {
        const val MIN_PASSWORD_LENGTH = 12

        fun mapSignInError(e: Throwable): String = "Invalid email or password."

        fun mapSignUpError(e: Throwable): String {
            val code = (e as? FirebaseAuthException)?.errorCode
            return when (code) {
                "ERROR_WEAK_PASSWORD" -> "Password must be at least $MIN_PASSWORD_LENGTH characters."
                "ERROR_EMAIL_ALREADY_IN_USE" -> "An account with this email already exists."
                "ERROR_INVALID_EMAIL" -> "Enter a valid email address."
                else -> "Could not create account. Try again."
            }
        }
    }
}