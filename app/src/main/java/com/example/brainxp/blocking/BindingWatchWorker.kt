package com.example.brainxp.blocking

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BindingWatchScheduler
    @Inject
    constructor(
        private val workManager: WorkManager,
    ) {
        fun schedule() {
            val request =
                PeriodicWorkRequestBuilder<BindingWatchWorker>(INTERVAL_MINUTES, TimeUnit.MINUTES)
                    .setConstraints(
                        Constraints
                            .Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build(),
                    ).addTag(BindingWatchWorker.TAG)
                    .build()

            workManager.enqueueUniquePeriodicWork(
                BindingWatchWorker.TAG,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        private companion object {
            const val INTERVAL_MINUTES = 15L
        }
    }

@HiltWorker
class BindingWatchWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val watcher: BindingWatcher,
    ) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            watcher.check()
            return Result.success()
        }

        companion object {
            const val TAG = "brainxp_binding_watch"
        }
    }
