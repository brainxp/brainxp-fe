package com.example.brainxp.blocking

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.brainxp.data.prefs.SettingsDataStore
import com.example.brainxp.data.repo.PendingSyncQueue
import com.example.brainxp.domain.model.DeviceRole
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
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
            workManager.enqueueUniqueWork(
                BindingWatchWorker.IMMEDIATE_TAG,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<BindingWatchWorker>()
                    .setConstraints(
                        Constraints
                            .Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build(),
                    ).addTag(BindingWatchWorker.IMMEDIATE_TAG)
                    .build(),
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
        private val heartbeat: ChildHeartbeat,
    ) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result = if (heartbeat.run()) Result.success() else Result.retry()

        companion object {
            const val TAG = "brainxp_binding_watch"
            const val IMMEDIATE_TAG = "brainxp_binding_watch_immediate"
        }
    }

@Singleton
class ChildHeartbeat
    @Inject
    constructor(
        private val watcher: BindingWatcher,
        private val pendingSync: PendingSyncQueue,
        private val protection: ProtectionStateHolder,
        private val health: GuardianHealthReporter,
        private val settings: SettingsDataStore,
        private val appInventory: AppInventoryPublisher,
    ) {
        suspend fun run(): Boolean {
            if (settings.settings.first().role != DeviceRole.CHILD) return true
            pendingSync.flush()
            appInventory.publish()
            watcher.check(force = true)
            val remainsChild = settings.settings.first().role == DeviceRole.CHILD
            if (remainsChild) {
                val snapshot = protection.current()
                health.report(snapshot.status, snapshot.missingPermissions)
            }
            return !remainsChild || pendingSync.queuedCount() == 0
        }
    }
