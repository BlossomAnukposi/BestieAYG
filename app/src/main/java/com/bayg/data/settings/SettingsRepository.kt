package com.bayg.data.settings

import kotlinx.coroutines.flow.Flow

/**
 * Abstraction over where the user's [AppSettings] live.
 *
 * Backed by [LocalSettingsRepository] before sign-in (and as a local
 * cache after), and by [FirestoreSettingsRepository] once the user is
 * signed in and verified. UI code only depends on this interface.
 */
interface SettingsRepository {

    /**
     * Cold flow that emits the current settings immediately, then again
     * on every update. Implementations return defaults when nothing has
     * been persisted yet.
     */
    fun observeSettings(): Flow<AppSettings>

    suspend fun getSettings(): AppSettings

    suspend fun updateSettings(settings: AppSettings)
}

/**
 * Abstraction over the user's [UserProfile] document. Created on first
 * sign-in, mutated rarely after that.
 */
interface UserProfileRepository {

    suspend fun getProfile(): UserProfile?

    /**
     * Creates the profile document only if it does not already exist.
     * Implementations must be idempotent so concurrent sign-ins from
     * multiple devices cannot clobber an existing profile.
     */
    suspend fun createIfMissing(profile: UserProfile)

    fun observeProfile(): Flow<UserProfile?>
}
