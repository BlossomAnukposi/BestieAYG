package com.bayg.auth

import android.content.Context
import com.bayg.security.SecurePrefs

object OnboardingStore {
    private fun key(uid: String) = "onboarding_complete_$uid"

    fun isComplete(context: Context, uid: String): Boolean {
        return SecurePrefs(context.applicationContext).getBoolean(key(uid), false)
    }

    fun markComplete(context: Context, uid: String) {
        SecurePrefs(context.applicationContext).putBoolean(key(uid), true)
    }
}
