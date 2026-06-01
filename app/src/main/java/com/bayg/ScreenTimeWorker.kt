package com.bayg

import android.content.Context
import androidx.work.*
import androidx.work.Worker
import java.util.concurrent.TimeUnit

/**
 * ScreenTimeWorker
 *
 * A periodic WorkManager worker that runs every 15 minutes
 * (the minimum interval WorkManager allows).
 *
 * On each run it checks whether the user's total screen time today
 * has exceeded the limit. If so — and no notification has been sent
 * today yet — it fires the touch grass notification and marks today
 * as notified so it won't fire again until tomorrow.
 *
 * WorkManager is battery-efficient and survives app restarts,
 * making it the correct tool for this kind of periodic background check.
 */
class ScreenTimeWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        if (ScreenTimeChecker.shouldNotify(applicationContext)) {
            TouchGrassNotifier.notify(applicationContext)
            ScreenTimeChecker.markNotifiedToday(applicationContext)
        }
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "screen_time_check"

        /**
         * Schedules the periodic worker. Safe to call multiple times —
         * KEEP_EXISTING means it won't restart if already scheduled.
         *
         * Call this from MainActivity.onCreate after permissions are confirmed.
         */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ScreenTimeWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(false) // run even on low battery
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP_EXISTING, // don't reset if already running
                request
            )
        }

        /**
         * Cancels the worker — call this if the user disables the feature.
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
