package com.bayg.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Wraps [BiometricPrompt] with a [BiometricPrompt.CryptoObject] so a
 * successful unlock is a cryptographic proof rather than a boolean.
 *
 * BIOMETRIC_STRONG (Class 3) only. Class 2 face unlock cannot release a
 * Keystore-bound CryptoObject, which is the whole point of using one.
 *
 * The biometric Keystore key is separate from [KeystoreManager]'s data
 * key. It is invalidated whenever the user enrols a new fingerprint or
 * face, so an attacker who adds their own biometric cannot decrypt the
 * original user's data.
 *
 * The calling Activity must extend [FragmentActivity]. Compose still
 * works on top, since FragmentActivity extends ComponentActivity.
 */
class BiometricAuthManager(private val context: Context) {

    enum class CanAuthenticate {
        Ok, NoHardware, HardwareUnavailable, NoneEnrolled,
        SecurityUpdateRequired, Unknown,
    }

    sealed interface AuthResult {
        data class Success(val cipher: Cipher) : AuthResult
        data class Failure(val errorCode: Int, val message: String) : AuthResult
        data object UserCancelled : AuthResult
    }

    fun canAuthenticate(): CanAuthenticate {
        val manager = BiometricManager.from(context)
        return when (manager.canAuthenticate(Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> CanAuthenticate.Ok
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> CanAuthenticate.NoHardware
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> CanAuthenticate.HardwareUnavailable
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> CanAuthenticate.NoneEnrolled
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> CanAuthenticate.SecurityUpdateRequired
            else -> CanAuthenticate.Unknown
        }
    }

    /**
     * Caller MUST persist `cipher.iv` to be able to decrypt later.
     */
    fun encryptCipher(): Cipher {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateBiometricKey())
        return cipher
    }

    fun decryptCipher(iv: ByteArray): Cipher {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateBiometricKey(),
            GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv),
        )
        return cipher
    }

    /**
     * Suspends until the user authenticates, cancels, or hits a hard
     * error. Cancelling the coroutine cancels the prompt.
     */
    suspend fun authenticate(
        activity: FragmentActivity,
        cipher: Cipher,
        title: String = "Unlock BestieAYG",
        subtitle: String = "Use your fingerprint or face",
        negativeButtonText: String = "Use password",
    ): AuthResult = suspendCancellableCoroutine { cont ->
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    val unlocked = result.cryptoObject?.cipher
                    if (unlocked != null) {
                        cont.resume(AuthResult.Success(unlocked))
                    } else {
                        cont.resume(AuthResult.Failure(-1, "CryptoObject missing"))
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    val result = when (errorCode) {
                        BiometricPrompt.ERROR_USER_CANCELED,
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                        BiometricPrompt.ERROR_CANCELED,
                            -> AuthResult.UserCancelled
                        else -> AuthResult.Failure(errorCode, errString.toString())
                    }
                    cont.resume(result)
                }
            },
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(Authenticators.BIOMETRIC_STRONG)
            .setConfirmationRequired(false)
            .build()

        prompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))

        cont.invokeOnCancellation { prompt.cancelAuthentication() }
    }

    private fun getOrCreateBiometricKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        val existing = keyStore.getKey(BIOMETRIC_KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_PROVIDER,
        )
        val spec = KeyGenParameterSpec.Builder(
            BIOMETRIC_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(AES_KEY_SIZE_BITS)
            .setRandomizedEncryptionRequired(true)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private companion object {
        const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        const val BIOMETRIC_KEY_ALIAS = "bayg_biometric_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_LENGTH_BITS = 128
        const val AES_KEY_SIZE_BITS = 256
    }
}
