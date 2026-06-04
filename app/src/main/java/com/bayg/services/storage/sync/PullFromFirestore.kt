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
        pullBlockEvents(localUserId)
        pullStreak(localUserId)
    }

    private suspend fun pullProfile(localUserId: Long) {
        val snaps = Firebase.firestore.collection("users").get().await()

        snaps.documents.forEach { doc ->
            val user = User(
                id = localUserId,
                firstName = doc.getString("firstName") ?: "",
                lastName = doc.getString("lastName") ?: "",
                email = doc.getString("email") ?: ""
            )
            db.userDao().insert(user)
        }
    }

    private suspend fun pullBlockEvents(localUserId: Long) {
        val snaps = Firebase.firestore.collection("block_events")
            .whereEqualTo("userId", userDoc())
            .orderBy("triggeredAt").get().await()

        snaps.documents.forEach { doc ->
            val event = BlockEvent(
                id = localUserId,
                firebaseId = doc.id,
                userId = uid,
                triggeredAt = doc.getLong("triggeredAt") ?: return@forEach,
                blockDurationMinutes = (doc.getLong("blockDurationMinutes") ?: 30).toInt(),
            )
            db.blockEventDao().insert(event)
        }
    }

    private suspend fun pullStreak(localUserId: Long) {
        val snap = Firebase.firestore.collection("streaks")
            .whereEqualTo("userId", userDoc())
            .limit(1).get().await().documents.firstOrNull()
            ?: return

        val streak = Streak(
            id = localUserId,
            firebaseId = snap.id,
            userId = uid,
            currentStreak = (snap.getLong("currentStreak") ?: 0).toInt(),
            lastStreakDate = snap.getLong("lastStreakDate")
        )
        db.streakDao().insert(streak)
    }
}