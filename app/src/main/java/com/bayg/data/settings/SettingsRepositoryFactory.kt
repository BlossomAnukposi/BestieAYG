package com.bayg.data.settings

import android.content.Context
import com.bayg.security.SecurePrefs

object SettingsRepositoryFactory {
    fun local(context: Context): SettingsRepository {
        return LocalSettingsRepository(SecurePrefs(context.applicationContext))
    }
}
