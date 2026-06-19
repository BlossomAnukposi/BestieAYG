package com.bayg.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Hardware-backed AES-256-GCM key utility.
 *
 * The key is generated and kept inside the Android Keystore (TEE or
 * StrongBox). The raw key material never leaves the secure hardware, so
 * even on a rooted device an attacker can use the key through the OS but
 * cannot extract it.
 *
 * Used by the future SQLCipher integration to encrypt the database
 * passphrase at rest. Not used by [SecurePrefs] (which manages its own
 * Keystore key via MasterKey) or [BiometricAuthManager] (which uses a
 * separate biometric-gated key).
 */
object KeystoreManager {

    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val DATA_KEY_ALIAS = "bayg_data_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val AES_KEY_SIZE_BITS = 256

    fun getOrCreateDataKey(): SecretKey {
        return loadKey(DATA_KEY_ALIAS) ?: generateDataKey()
    }

    /**
     * Returns a Cipher initialised for encryption. The caller MUST persist
     * `cipher.iv` alongside the ciphertext, otherwise the value cannot be
     * decrypted later.
     */
    fun encryptCipher(): Cipher {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateDataKey())
        return cipher
    }

    fun decryptCipher(iv: ByteArray): Cipher {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateDataKey(),
            GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv),
        )
        return cipher
    }

    /**
     * Test helper. Deleting the key makes all data encrypted under it
     * permanently unreadable. Never call from production code paths.
     */
    fun deleteDataKey() {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        if (keyStore.containsAlias(DATA_KEY_ALIAS)) {
            keyStore.deleteEntry(DATA_KEY_ALIAS)
        }
    }

    private fun generateDataKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_PROVIDER,
        )
        val spec = KeyGenParameterSpec.Builder(
            DATA_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(AES_KEY_SIZE_BITS)
            .setRandomizedEncryptionRequired(true)
            // Background data key, so no biometric prompt on each use.
            .setUserAuthenticationRequired(false)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private fun loadKey(alias: String): SecretKey? {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        return keyStore.getKey(alias, null) as? SecretKey
    }
}
