package com.bayg.services.storage.sync

import com.bayg.services.storage.AppDatabase
import com.bayg.services.storage.entities.BlockEvent
import com.bayg.services.storage.entities.Streak
import com.bayg.services.storage.entities.User
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

class PushToFirestore (db: AppDatabase) : SyncRepository(db) {
    suspend fun pushProfile(fbUser: FirebaseUser, user: User) {
        Firebase.firestore.collection("users").document(fbUser.uid).set(
            mapOf(
                "firstName" to user.firstName,
                "lastName" to user.lastName,
                "email" to fbUser.email,
                "createdAt" to user.createdAt
            )
        ).await()
    }

    suspend fun pushBlockEvent(event: BlockEvent) {
        Firebase.firestore.collection("block_events").add(
            mapOf(
                "blockDurationMinutes" to event.blockDurationMinutes,
                "triggeredAt" to event.triggeredAt,
                "userId" to event.userId
            )
        ).await()
    }

    suspend fun pushStreak(streak: Streak) {
        Firebase.firestore.collection("streaks").document(streak.firebaseId)
            .set(
                mapOf(
                    "currentStreak" to streak.currentStreak,
                    "lastStreakDate" to streak.lastStreakDate,
                    "userId" to streak.userId
                ),
                SetOptions.merge()
            ).await()
    }
}