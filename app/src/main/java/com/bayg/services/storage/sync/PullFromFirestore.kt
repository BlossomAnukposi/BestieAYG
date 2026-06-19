package com.bayg.services.storage.sync

import com.bayg.services.storage.AppDatabase
import com.bayg.services.storage.entities.BlockEvent
import com.bayg.services.storage.entities.Streak
import com.bayg.services.storage.entities.User
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

class PullFromFirestore(db: AppDatabase) : SyncRepository(db) {

    suspend fun pullAll(localUserId: Long) {
        pullProfile(localUserId)
        pullBlockEvents()
        pullStreak()
    }

    /**
     * Reads only the signed-in user's document. Listing the whole users
     * collection is denied by the owner-only security rules.
     */
    private suspend fun pullProfile(localUserId: Long) {
        val snap = userDoc().get().await()
        if (!snap.exists()) return

        val existing = db.userDao().getByFirebaseUid(uid)
        val user = User(
            id = existing?.id ?: if (localUserId > 0) localUserId else 0,
            firebaseUid = uid,
            firstName = snap.getString("firstName") ?: "",
            lastName = snap.getString("lastName") ?: "",
            email = snap.getString("email") ?: "",
            createdAt = snap.getLong("createdAt") ?: System.currentTimeMillis(),
        )

        if (existing != null) {
            db.userDao().update(user.copy(id = existing.id))
        } else {
            db.userDao().insert(user)
        }
    }

    private suspend fun pullBlockEvents() {
        val snaps = Firebase.firestore.collection("block_events")
            .whereEqualTo("userId", uid)
            .orderBy("triggeredAt")
            .get()
            .await()

        snaps.documents.forEach { doc ->
            val event = BlockEvent(
                firebaseId = doc.id,
                userId = uid,
                triggeredAt = doc.getLong("triggeredAt") ?: return@forEach,
                blockDurationMinutes = (doc.getLong("blockDurationMinutes") ?: 30).toInt(),
            )
            db.blockEventDao().insert(event)
        }
    }

    private suspend fun pullStreak() {
        val snap = Firebase.firestore.collection("streaks")
            .whereEqualTo("userId", uid)
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull()
            ?: return

        val existing = db.streakDao().getByUserId(uid)
        val streak = Streak(
            id = existing?.id ?: 0,
            firebaseId = snap.id,
            userId = uid,
            currentStreak = (snap.getLong("currentStreak") ?: 0).toInt(),
            lastStreakDate = snap.getLong("lastStreakDate"),
        )
        db.streakDao().insert(streak)
    }
}
