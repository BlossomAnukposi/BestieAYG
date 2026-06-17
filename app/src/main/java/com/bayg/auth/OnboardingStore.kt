package com.bayg.auth

import android.content.Context
import com.bayg.security.SecurePrefs

/**
 * Device-local onboarding completion flag. Stored in encrypted SecurePrefs.
 * One flag per install — permissions and app setup are device setup, not per-account.
 */
object OnboardingStore {
    private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"

    fun isComplete(context: Context): Boolean {
        return SecurePrefs(context.applicationContext).getBoolean(KEY_ONBOARDING_COMPLETE, false)
    }

    fun markComplete(context: Context) {
        SecurePrefs(context.applicationContext).putBoolean(KEY_ONBOARDING_COMPLETE, true)
    }
}
