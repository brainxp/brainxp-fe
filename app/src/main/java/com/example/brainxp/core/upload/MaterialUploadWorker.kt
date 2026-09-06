package com.example.brainxp.core.upload

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.brainxp.core.result.AppResult
import com.example.brainxp.data.repo.MaterialRepository
import com.example.brainxp.domain.model.MaterialType
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

const val KEY_CACHED_PATH = "cachedPath"
const val KEY_TITLE = "title"
const val KEY_TYPE = "type"

@Singleton
class UploadQueue
    @Inject
    constructor(
        private val workManager: WorkManager,
    ) {
        fun enqueue(
            cachedPath: String,
            title: String,
            type: MaterialType,
        ): String {
            val request =
                OneTimeWorkRequestBuilder<MaterialUploadWorker>()
                    .setInputData(
                        workDataOf(
                            KEY_CACHED_PATH to cachedPath,
                            KEY_TITLE to title,
                            KEY_TYPE to type.name,
                        ),
                    ).setConstraints(
                        Constraints
                            .Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build(),
                    ).setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
                    .addTag(TAG)
                    .build()

            workManager.enqueueUniqueWork(
                uniqueNameFor(cachedPath),
                androidx.work.ExistingWorkPolicy.KEEP,
                request,
            )
            return request.id.toString()
        }

        private fun uniqueNameFor(cachedPath: String): String = "$TAG:${File(cachedPath).name}"

        companion object {
            const val TAG = "material-upload"
            private const val BACKOFF_SECONDS = 30L
        }
    }

@HiltWorker
class MaterialUploadWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val materials: MaterialRepository,
        private val preparation: MaterialPreparation,
    ) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            val path = inputData.getString(KEY_CACHED_PATH) ?: return Result.failure()
            val file = File(path)
            if (!file.exists()) {
                return Result.failure(reason(MISSING))
            }

            val title = inputData.getString(KEY_TITLE).orEmpty()
            val type =
                runCatching { MaterialType.valueOf(inputData.getString(KEY_TYPE).orEmpty()) }
                    .getOrDefault(MaterialType.DOCUMENT)

            return when (val result = materials.upload(title, type, path)) {
                is AppResult.Success -> {
                    file.delete()
                    preparation.watch(result.value.id, title)
                    Result.success()
                }

                is AppResult.Failure -> {
                    if (result.error.retryable) Result.retry() else Result.failure(reason(REJECTED))
                }
            }
        }

        private fun reason(value: String): Data = workDataOf(KEY_REASON to value)

        private companion object {
            const val KEY_REASON = "reason"
            const val MISSING = "cache-missing"
            const val REJECTED = "rejected"
        }
    }
