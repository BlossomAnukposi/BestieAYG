package com.bayg.auth

import android.content.Context

object OnboardingStore {
    private const val PREFS_NAME = "bayg_onboarding"

    fun isComplete(context: Context, uid: String): Boolean {
        return prefs(context).getBoolean(key(uid), false)
    }

    fun markComplete(context: Context, uid: String) {
        prefs(context).edit().putBoolean(key(uid), true).apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun key(uid: String) = "onboarding_complete_$uid"
}
