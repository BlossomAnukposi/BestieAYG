package com.bayg.auth

import android.content.Context
import com.google.firebase.auth.FirebaseAuth

object AuthNavigation {
    fun resolveStartDestination(context: Context): String {
        val user = FirebaseAuth.getInstance().currentUser ?: return "onboardingStart"
        if (!user.isEmailVerified) return "verifyEmail"
        if (!OnboardingStore.isComplete(context)) return "permissions"
        return "dashboard"
    }

    fun resolvePostSignInDestination(context: Context): String {
        if (FirebaseAuth.getInstance().currentUser == null) return "onboardingStart"
        return if (OnboardingStore.isComplete(context)) "dashboard" else "permissions"
    }
}
