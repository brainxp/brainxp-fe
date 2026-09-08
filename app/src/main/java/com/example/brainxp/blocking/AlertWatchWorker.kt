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
import com.example.brainxp.core.result.valueOrNull
import com.example.brainxp.data.prefs.AlertSeenStore
import com.example.brainxp.data.repo.AlertRepository
import com.example.brainxp.data.repo.ParentDeviceCheck
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertWatchScheduler
    @Inject
    constructor(
        private val workManager: WorkManager,
    ) {
        fun schedule() {
            val request =
                PeriodicWorkRequestBuilder<AlertWatchWorker>(INTERVAL_MINUTES, TimeUnit.MINUTES)
                    .setConstraints(
                        Constraints
                            .Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build(),
                    ).addTag(AlertWatchWorker.TAG)
                    .build()

            workManager.enqueueUniquePeriodicWork(
                AlertWatchWorker.TAG,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        private companion object {
            const val INTERVAL_MINUTES = 15L
        }
    }

@HiltWorker
class AlertWatchWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val alerts: AlertRepository,
        private val seen: AlertSeenStore,
        private val notifier: AlertNotifier,
        private val parentDevice: ParentDeviceCheck,
    ) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            if (!parentDevice.watchesAFamily()) return Result.success()
            val raised = alerts.openAlerts().valueOrNull() ?: return Result.retry()
            val known = seen.lastSeen()
            raised
                .filter { alert -> alert.id > known && !alert.acknowledged }
                .forEach { alert ->
                    notifier.announce(alert)
                    seen.remember(alert.id)
                }
            return Result.success()
        }

        companion object {
            const val TAG = "brainxp_alert_watch"
        }
    }
