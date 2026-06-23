package com.bayg.services.storage.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.bayg.services.storage.AppDatabase
import com.google.firebase.auth.FirebaseAuth
import java.util.concurrent.TimeUnit

private const val SYNC_RETRY_LIMIT = 3
private const val REPEAT_INTERVAL : Long = 15
private const val DELAY : Long = 30

class SyncWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val auth = FirebaseAuth.getInstance()
        val db = AppDatabase.getInstance(applicationContext)

        val uid = auth.currentUser?.uid ?: return Result.success()
        val localUid = db.userDao().getByFirebaseUid(uid)?.id ?: 0

        val syncPush = PushToFirestore(db)
        val syncPull = PullFromFirestore(db)

        return try {
            syncPull.pullAll(localUid)

            val blockEvents = db.blockEventDao().getUnsyncedBlockEvents(uid)
            for (event in blockEvents) {
                val firebaseId = syncPush.pushBlockEvent(event)
                db.blockEventDao().markSynced(
                    eventId = event.id,
                    syncedAt = System.currentTimeMillis(),
                    firebaseId = firebaseId
                )
            }

            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < SYNC_RETRY_LIMIT) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val WORK_NAME = "bayg_sync"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED).build()

            val request = PeriodicWorkRequestBuilder<SyncWorker>(REPEAT_INTERVAL, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, DELAY, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        /**
         * IMPORTANT! Make sure you call runOnce everytime you have made an insert or update
         * call from the Room Dao. This reduces the chances of abusers taking advantage of
         * sync time
         */
        fun runOnce(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED).build()

            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints).build()

            WorkManager.getInstance(context).enqueue(request)
        }
    }
}