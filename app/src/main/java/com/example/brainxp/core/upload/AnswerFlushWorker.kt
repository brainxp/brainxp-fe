package com.example.brainxp.core.upload

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.brainxp.data.repo.AnswerQueue
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnswerFlushScheduler
    @Inject
    constructor(
        private val workManager: WorkManager,
    ) {
        fun schedule() {
            val request =
                OneTimeWorkRequestBuilder<AnswerFlushWorker>()
                    .setConstraints(
                        Constraints
                            .Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build(),
                    ).addTag(AnswerFlushWorker.TAG)
                    .build()

            workManager.enqueueUniqueWork(AnswerFlushWorker.TAG, ExistingWorkPolicy.REPLACE, request)
        }
    }

@HiltWorker
class AnswerFlushWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val answers: AnswerQueue,
    ) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            answers.flush()
            return if (answers.queuedCount() == 0) Result.success() else Result.retry()
        }

        companion object {
            const val TAG = "answer-flush"
        }
    }
