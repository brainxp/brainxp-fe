package com.example.brainxp.data.repo

import com.example.brainxp.core.network.ErrorMapper
import com.example.brainxp.core.network.FileStreamRequestBody
import com.example.brainxp.core.network.MaterialApi
import com.example.brainxp.core.network.MaterialDto
import com.example.brainxp.core.result.AppResult
import com.example.brainxp.domain.model.Material
import com.example.brainxp.domain.model.MaterialStatus
import com.example.brainxp.domain.model.MaterialType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkMaterialUploader
    @Inject
    constructor(
        private val api: MaterialApi,
        private val errors: ErrorMapper,
    ) {
        suspend fun upload(
            subjectId: String,
            cachedPath: String,
            method: String,
        ): AppResult<String> {
            val file = File(cachedPath)
            val body =
                FileStreamRequestBody(file, contentTypeFor(file.name).toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData(FIELD_FILE, file.name, body)

            return call { api.upload(subjectId, part, method.toRequestBody(PLAIN)) }
                .map { it.materialId }
        }

        suspend fun material(materialId: String): AppResult<Material> = call { api.material(materialId) }.map { it.toMaterial() }

        private suspend fun <T> call(block: suspend () -> T): AppResult<T> =
            runCatching { block() }
                .fold(
                    onSuccess = { AppResult.Success(it) },
                    onFailure = { AppResult.Failure(errors.map(it)) },
                )

        private fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> =
            when (this) {
                is AppResult.Success -> AppResult.Success(transform(value))
                is AppResult.Failure -> AppResult.Failure(error)
            }

        private fun contentTypeFor(name: String): String =
            when (name.substringAfterLast('.', "").lowercase()) {
                "pdf" -> "application/pdf"
                "txt", "md" -> "text/plain"
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                "webp" -> "image/webp"
                else -> "application/octet-stream"
            }

        private companion object {
            const val FIELD_FILE = "file"
            val PLAIN = "text/plain".toMediaTypeOrNull()
        }
    }

private fun MaterialDto.toMaterial(): Material =
    Material(
        id = id,
        title = originalName ?: topicSummary ?: "Materi",
        type = if (sourceType?.startsWith("image/") == true) MaterialType.IMAGE else MaterialType.DOCUMENT,
        status =
            when (status) {
                "ready" -> MaterialStatus.READY
                "rejected", "failed" -> MaterialStatus.FAILED
                "uploaded", "processing" -> MaterialStatus.PROCESSING
                else -> MaterialStatus.PROCESSING
            },
        charCount = 0,
        createdAt = 0L,
        sessionCount = timesStudied,
    )
