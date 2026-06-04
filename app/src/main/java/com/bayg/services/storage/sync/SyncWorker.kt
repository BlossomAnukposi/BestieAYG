package com.bayg.services.storage.sync

import android.content.Context
import androidx.work.*
import com.bayg.services.storage.AppDatabase
import com.bayg.services.storage.entities.BlockEvent
import com.bayg.services.storage.sync.SyncRepository
import com.google.firebase.auth.FirebaseAuth
import java.util.concurrent.TimeUnit

class SyncWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid ?: return Result.success()

        val db = AppDatabase.getInstance(applicationContext)
        val syncPush = PushToFirestore(db)
        val syncPull = PullFromFirestore(db)

        return try {
            val streak = db.streakDao().getByUserId(uid)
            if (streak != null) syncPush.pushStreak(streak)

//            val blockEvents = db.blockEventDao().getAllBlockEvents(uid)
//            blockEvents.collect { events ->
//                for (event in events) { syncPush.pushBlockEvent(event) }
//            }

            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val WORK_NAME = "bayg_sync"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED).build()

            val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun runOnce(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED).build()

            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints).build()

            WorkManager.getInstance(context).enqueue(request)
        }
    }
}