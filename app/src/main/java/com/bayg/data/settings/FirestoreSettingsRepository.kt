package com.bayg.data.settings

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await

/**
 * Firestore-backed [SettingsRepository] for a single signed-in user.
 *
 * Doc path: `users/{uid}/settings/main`. The security rules reject any
 * read or write where `request.auth.uid != uid`, so this class must only
 * be constructed with the currently authenticated user's id.
 *
 * Writes use `set(..., merge)` so a partial update from one client never
 * blows away fields added by a newer client.
 */
class FirestoreSettingsRepository(
    private val uid: String,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : SettingsRepository {

    private fun docRef() = firestore
        .collection(USERS_COLLECTION)
        .document(uid)
        .collection(AppSettings.COLLECTION)
        .document(AppSettings.DOCUMENT_ID)

    override fun observeSettings(): Flow<AppSettings> = callbackFlow {
        val registration = docRef().addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(AppSettings())
                return@addSnapshotListener
            }
            trySend(snapshot?.toObject(AppSettings::class.java) ?: AppSettings())
        }
        awaitClose { registration.remove() }
    }.distinctUntilChanged()

    override suspend fun getSettings(): AppSettings {
        val snapshot = docRef().get().await()
        return snapshot.toObject(AppSettings::class.java) ?: AppSettings()
    }

    override suspend fun updateSettings(settings: AppSettings) {
        docRef().set(settings, SetOptions.merge()).await()
    }

    private companion object {
        const val USERS_COLLECTION = "users"
    }
}
