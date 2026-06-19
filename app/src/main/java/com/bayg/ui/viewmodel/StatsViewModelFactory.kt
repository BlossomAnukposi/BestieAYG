package com.bayg.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModelProvider


/**
 * Factory for StatsViewModel — mirrors how ProfileSettingsViewModel is
 * expected to be constructed (context + firebase userId).
 *
 * TODO: replace "currentUserId" with however the app resolves the
 * logged-in user's Firebase UID elsewhere (e.g. FirebaseAuth.getInstance().uid).
 */
class StatsViewModelFactory(
    private val context: Context,
    private val userId: String,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return StatsViewModel(context, userId) as T
    }
}