package com.bayg.data.settings

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await

/**
 * Firestore-backed [UserProfileRepository] for a single signed-in user.
 *
 * Doc path: `users/{uid}/profile/main`. [createIfMissing] uses a
 * transaction so two parallel sign-ins from different devices cannot
 * clobber each other.
 */
class FirestoreUserProfileRepository(
    private val uid: String,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : UserProfileRepository {

    private fun docRef() = firestore
        .collection(USERS_COLLECTION)
        .document(uid)
        .collection(UserProfile.COLLECTION)
        .document(UserProfile.DOCUMENT_ID)

    override suspend fun getProfile(): UserProfile? {
        val snapshot = docRef().get().await()
        return snapshot.toObject(UserProfile::class.java)
    }

    override suspend fun createIfMissing(profile: UserProfile) {
        val ref = docRef()
        firestore.runTransaction { txn ->
            if (!txn.get(ref).exists()) {
                txn.set(ref, profile, SetOptions.merge())
            }
            null
        }.await()
    }

    override fun observeProfile(): Flow<UserProfile?> = callbackFlow {
        val registration = docRef().addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(null)
                return@addSnapshotListener
            }
            trySend(snapshot?.toObject(UserProfile::class.java))
        }
        awaitClose { registration.remove() }
    }.distinctUntilChanged()

    private companion object {
        const val USERS_COLLECTION = "users"
    }
}
