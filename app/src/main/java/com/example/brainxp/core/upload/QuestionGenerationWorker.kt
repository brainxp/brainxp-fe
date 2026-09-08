package com.example.brainxp.core.upload

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.brainxp.core.result.AppResult
import com.example.brainxp.data.repo.MaterialRepository
import com.example.brainxp.domain.model.Material
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

const val KEY_MATERIAL_ID = "materialId"

@Singleton
class QuestionGenerationQueue
    @Inject
    constructor(
        private val workManager: WorkManager,
    ) {
        fun enqueue(materialId: String) {
            val request =
                OneTimeWorkRequestBuilder<QuestionGenerationWorker>()
                    .setInputData(workDataOf(KEY_MATERIAL_ID to materialId))
                    .setConstraints(
                        Constraints
                            .Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build(),
                    ).setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
                    .addTag(QuestionGenerationWorker.TAG)
                    .build()

            workManager.enqueueUniqueWork(
                uniqueNameFor(materialId),
                ExistingWorkPolicy.KEEP,
                request,
            )
        }

        private fun uniqueNameFor(materialId: String) = "${QuestionGenerationWorker.TAG}:$materialId"

        private companion object {
            const val BACKOFF_SECONDS = 30L
        }
    }

@HiltWorker
class QuestionGenerationWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val materials: MaterialRepository,
        private val notifier: PreparationNotifier,
    ) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            val materialId = inputData.getString(KEY_MATERIAL_ID) ?: return Result.failure()
            return watch(materialId)
        }

        private suspend fun watch(materialId: String): Result {
            var waited = 0L
            var wait = FIRST_DELAY_MILLIS

            while (waited < ATTEMPT_BUDGET_MILLIS) {
                verdictOf(materialId)?.let { return it }
                delay(wait)
                waited += wait
                wait = nextDelay(wait)
            }

            return if (runAttemptCount >= MAX_ATTEMPTS) Result.failure() else Result.retry()
        }

        private suspend fun verdictOf(materialId: String): Result? =
            when (val result = materials.detail(materialId)) {
                is AppResult.Success -> announced(result.value)
                is AppResult.Failure -> Result.failure().takeUnless { result.error.retryable }
            }

        private suspend fun announced(material: Material): Result? {
            if (!settled(material.status)) {
                return null
            }
            notifier.announce(material)
            return Result.success()
        }

        companion object {
            const val TAG = "question-generation"
            private const val ATTEMPT_BUDGET_MILLIS = 120_000L
            private const val MAX_ATTEMPTS = 5
        }
    }
