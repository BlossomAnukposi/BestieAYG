package com.bayg.auth

import android.content.Context
import com.bayg.security.SecurePrefs
import com.bayg.services.storage.AppDatabase
import com.bayg.services.storage.Authenticator
import com.bayg.services.storage.entities.User
import com.bayg.services.storage.entities.UserSettings
import com.bayg.services.storage.sync.PushToFirestore
import com.bayg.services.storage.sync.SyncWorker
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class SignInResult {
    data object Verified : SignInResult()
    data object NeedsVerification : SignInResult()
}

class AuthRepository(
    private val context: Context,
    private val authenticator: Authenticator = Authenticator(),
    private val db: AppDatabase = AppDatabase.getInstance(context),
) {
    val currentUserEmail: String? get() = authenticator.currentUser?.email

    suspend fun getProfile(): ProfileUi? = withContext(Dispatchers.IO) {
        val fbUser = authenticator.currentUser ?: return@withContext null
        val user = db.userDao().getByFirebaseUid(fbUser.uid)
        val displayName = user?.let { "${it.firstName} ${it.lastName}".trim().ifBlank { it.firstName } }
            ?: fbUser.displayName?.trim()?.takeIf { it.isNotEmpty() }
            ?: "User"
        val email = user?.email ?: fbUser.email.orEmpty()
        val initial = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        ProfileUi(displayName = displayName, email = email, initial = initial)
    }

    fun signOut() {
        authenticator.signOut()
        val prefs = SecurePrefs(context.applicationContext)
        prefs.remove(SecurePrefs.Key.LAST_SIGNED_IN_UID)
        prefs.remove(SecurePrefs.Key.SETTINGS_JSON_CACHE)
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val trimmedEmail = email.trim()
            require(isValidEmail(trimmedEmail)) { "Enter your email address first." }
            authenticator.sendPasswordReset(trimmedEmail)
        }
    }

    suspend fun signUp(name: String, email: String, password: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val trimmedName = name.trim()
            val trimmedEmail = email.trim()
            require(trimmedName.isNotBlank()) { "Enter your name." }
            require(isValidEmail(trimmedEmail)) { "Enter a valid email address." }
            require(password.length >= Authenticator.MIN_PASSWORD_LENGTH) {
                "Password must be at least ${Authenticator.MIN_PASSWORD_LENGTH} characters."
            }

            val (firstName, lastName) = splitName(trimmedName)
            val fbUser = authenticator.signUp(trimmedEmail, firstName, lastName, password)
            authenticator.sendEmailVerification()

            val user = User(
                firebaseUid = fbUser.uid,
                email = trimmedEmail,
                firstName = firstName,
                lastName = lastName,
            )
            val roomId = db.userDao().insert(user)
            ensureDefaultSettings(roomId)
            rememberSignedInUid(fbUser.uid)
            PushToFirestore(db).pushProfile(fbUser, user.copy(id = roomId))
            SyncWorker.runOnce(context)
        }
    }

    suspend fun signIn(email: String, password: String): Result<SignInResult> = withContext(Dispatchers.IO) {
        runCatching {
            val trimmedEmail = email.trim()
            require(isValidEmail(trimmedEmail)) { "Enter a valid email address." }
            require(password.isNotBlank()) { "Enter your password." }

            val fbUser = authenticator.signIn(trimmedEmail, password)
            ensureLocalUser(fbUser, trimmedEmail)
            rememberSignedInUid(fbUser.uid)
            SyncWorker.runOnce(context)

            if (fbUser.isEmailVerified) SignInResult.Verified else SignInResult.NeedsVerification
        }
    }

    suspend fun resendVerificationEmail(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            authenticator.sendEmailVerification()
        }
    }

    suspend fun refreshEmailVerified(): Boolean = withContext(Dispatchers.IO) {
        authenticator.reloadUser()?.isEmailVerified == true
    }

    private suspend fun ensureLocalUser(fbUser: FirebaseUser, email: String) {
        if (db.userDao().getByFirebaseUid(fbUser.uid) != null) return

        val (firstName, lastName) = splitName(fbUser.displayName.orEmpty())
        val user = User(
            firebaseUid = fbUser.uid,
            email = fbUser.email ?: email,
            firstName = firstName.ifBlank { "User" },
            lastName = lastName,
        )
        val roomId = db.userDao().insert(user)
        ensureDefaultSettings(roomId)
        PushToFirestore(db).pushProfile(fbUser, user.copy(id = roomId))
    }

    private suspend fun ensureDefaultSettings(roomUserId: Long) {
        val userId = roomUserId.toString()
        if (db.userSettingsDao().getByUserId(userId) == null) {
            db.userSettingsDao().insert(UserSettings(userId = userId))
        }
    }

    private fun rememberSignedInUid(uid: String) {
        SecurePrefs(context.applicationContext)
            .putString(SecurePrefs.Key.LAST_SIGNED_IN_UID, uid)
    }

    private fun splitName(fullName: String): Pair<String, String> {
        val parts = fullName.trim().split("\\s+".toRegex(), limit = 2)
        return parts[0] to parts.getOrElse(1) { "" }
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}
