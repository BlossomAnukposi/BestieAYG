package com.bayg.security

import android.content.Context
import android.util.Base64
import java.security.SecureRandom

/**
 * Provides the 256-bit passphrase used by SQLCipher to encrypt the
 * Room database.
 *
 * The passphrase is generated once on first launch with [SecureRandom],
 * Base64-encoded, and stored inside [SecurePrefs] under
 * [SecurePrefs.Key.DB_PASSPHRASE_B64]. Because [SecurePrefs] is itself
 * encrypted with a MasterKey kept in the Android Keystore (AES-256-GCM
 * for values, AES-256-SIV for keys), the passphrase is never on disk
 * in plaintext.
 *
 * Threat model coverage:
 *   - "Room DB stored unencrypted" (Test 3 finding)
 *   - "Database extracted via ADB backup"
 *   - "Sensitive local data read on rooted device"
 *
 * Even an attacker who recovers the on-device files cannot read the
 * database without either:
 *   - the Android Keystore key that decrypts SecurePrefs (which is
 *     bound to the device hardware and cannot be exported), or
 *   - the running process's memory, at which point the passphrase has
 *     already been used to open the DB and the attacker has every
 *     other privilege anyway.
 */
class DbPassphraseProvider(private val securePrefs: SecurePrefs) {

    /**
     * Returns the raw 32-byte passphrase. Generates and persists one on
     * first call, then returns the same value for the lifetime of the
     * install.
     */
    fun loadOrCreate(): ByteArray {
        val existing = securePrefs.getString(SecurePrefs.Key.DB_PASSPHRASE_B64)
        if (existing != null) {
            return Base64.decode(existing, Base64.NO_WRAP)
        }
        val fresh = ByteArray(PASSPHRASE_LENGTH_BYTES).also { SecureRandom().nextBytes(it) }
        securePrefs.putString(
            SecurePrefs.Key.DB_PASSPHRASE_B64,
            Base64.encodeToString(fresh, Base64.NO_WRAP),
        )
        return fresh
    }

    companion object {
        // SQLCipher accepts any byte length; 32 bytes (256 bits) matches
        // the AES-256 key size and is what the SQLCipher docs recommend
        // for raw-key mode.
        const val PASSPHRASE_LENGTH_BYTES = 32

        /** Convenience constructor for callers that only have a Context. */
        fun create(context: Context): DbPassphraseProvider =
            DbPassphraseProvider(SecurePrefs(context.applicationContext))
    }
}

/** New SecurePrefs key added for the SQLCipher passphrase. */
internal val SecurePrefsKeyPassphrase: SecurePrefs.Key
    get() = SecurePrefs.Key.DB_PASSPHRASE_B64
