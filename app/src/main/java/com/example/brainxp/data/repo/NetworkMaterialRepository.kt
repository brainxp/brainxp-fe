package com.example.brainxp.data.repo

import com.example.brainxp.core.network.ErrorMapper
import com.example.brainxp.core.network.FileStreamRequestBody
import com.example.brainxp.core.network.MaterialApi
import com.example.brainxp.core.network.MaterialDto
import com.example.brainxp.core.result.ApiError
import com.example.brainxp.core.result.AppResult
import com.example.brainxp.core.result.map
import com.example.brainxp.data.db.MaterialDao
import com.example.brainxp.data.db.MaterialEntity
import com.example.brainxp.data.prefs.AuthDataStore
import com.example.brainxp.domain.model.Material
import com.example.brainxp.domain.model.MaterialPage
import com.example.brainxp.domain.model.MaterialStatus
import com.example.brainxp.domain.model.MaterialType
import com.example.brainxp.domain.model.UnfinishedSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkMaterialRepository
    @Inject
    constructor(
        private val api: MaterialApi,
        private val dao: MaterialDao,
        private val auth: AuthDataStore,
        private val errors: ErrorMapper,
    ) : MaterialRepository {
        override fun observeCached(): Flow<List<Material>> = dao.observeAll().map { rows -> rows.map(MaterialEntity::toMaterial) }

        override suspend fun upload(
            title: String,
            type: MaterialType,
            paths: List<String>,
        ): AppResult<Material> {
            val subject = auth.current().subjectId ?: return AppResult.Failure(ApiError.Unauthorized)
            val files = paths.map(::File)
            if (files.isEmpty() || files.any { !it.exists() }) return AppResult.Failure(MISSING_FILE)

            val names = pageNames(title, files)
            val parts =
                files.mapIndexed { index, file ->
                    MultipartBody.Part.createFormData(
                        FIELD_FILE,
                        names[index],
                        FileStreamRequestBody(file, contentTypeFor(file.name).toMediaTypeOrNull()),
                    )
                }
            val accepted = call { api.upload(subject, parts) }
            return when (accepted) {
                is AppResult.Failure -> accepted
                is AppResult.Success -> detailOf(accepted.value.materialId)
            }
        }

        override suspend fun page(cursor: String?): AppResult<MaterialPage> {
            val subject = auth.current().subjectId ?: return AppResult.Failure(ApiError.Unauthorized)
            return call { api.library(subject) }.map { rows ->
                val items = rows.map(MaterialDto::toMaterial)
                dao.upsert(items.map(Material::toEntity))
                if (items.isEmpty()) dao.clearAll() else dao.keepOnly(items.map(Material::id))
                MaterialPage(items = items, nextCursor = null)
            }
        }

        override suspend fun detail(materialId: String): AppResult<Material> = detailOf(materialId)

        override suspend fun delete(materialId: String): AppResult<Unit> =
            call { api.delete(materialId) }.map { dao.deleteById(materialId) }

        private suspend fun detailOf(materialId: String): AppResult<Material> =
            call { api.material(materialId) }.map { dto ->
                dto.toMaterial().also { dao.upsert(listOf(it.toEntity())) }
            }

        private suspend fun <T> call(block: suspend () -> T): AppResult<T> =
            runCatching { block() }
                .fold(
                    onSuccess = { AppResult.Success(it) },
                    onFailure = { AppResult.Failure(errors.map(it)) },
                )

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
            val MISSING_FILE = ApiError.Validation(field = "file", message = null)
        }
    }

private val PAGE_SUFFIX = Regex("""\s*\(\d+/\d+\)$""")
private val EXTRA_SUFFIX = Regex("""\s*\(\+\d+\s+(foto|photo|photos)\)$""", RegexOption.IGNORE_CASE)

internal fun readableTitle(name: String?): String? {
    var value = name?.trim() ?: return null
    while (true) {
        val next = value.replace(PAGE_SUFFIX, "").replace(EXTRA_SUFFIX, "").trim()
        if (next == value) return value.ifBlank { null }
        value = next
    }
}

internal fun pageNames(
    title: String,
    files: List<File>,
): List<String> {
    val label = title.ifBlank { files.first().name }
    return if (files.size == 1) {
        listOf(label)
    } else {
        files.indices.map { page -> "$label (${page + 1}/${files.size})" }
    }
}

internal fun MaterialDto.toMaterial(): Material =
    Material(
        id = id,
        title = readableTitle(originalName) ?: topicSummary ?: FALLBACK_TITLE,
        type = if (sourceType?.startsWith("image/") == true) MaterialType.IMAGE else MaterialType.DOCUMENT,
        status = statusOf(status),
        createdAt = epochOf(createdAt),
        sessionCount = timesStudied,
        questionCount = questionCount,
        assessedLevel = assessedLevel,
        declaredLevel = declaredLevel,
        gateReason = gateReason,
        topicSummary = topicSummary,
        unfinished =
            unfinished?.let {
                UnfinishedSession(sessionId = it.sessionId, answered = it.answered, total = it.total)
            },
    )

private fun statusOf(raw: String): MaterialStatus =
    when (raw) {
        STATUS_READY -> MaterialStatus.READY
        STATUS_REJECTED, STATUS_FAILED -> MaterialStatus.FAILED
        else -> MaterialStatus.PROCESSING
    }

private fun epochOf(raw: String?): Long = raw?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() } ?: 0L

private fun Material.toEntity(): MaterialEntity =
    MaterialEntity(
        id = id,
        title = title,
        type = type.name,
        status = status.name,
        createdAt = createdAt,
        sessionCount = sessionCount,
        questionCount = questionCount,
        assessedLevel = assessedLevel,
        declaredLevel = declaredLevel,
        gateReason = gateReason,
        topicSummary = topicSummary,
        unfinishedSessionId = unfinished?.sessionId,
        unfinishedAnswered = unfinished?.answered,
        unfinishedTotal = unfinished?.total,
    )

private fun MaterialEntity.toMaterial(): Material =
    Material(
        id = id,
        title = title,
        type = runCatching { MaterialType.valueOf(type) }.getOrDefault(MaterialType.DOCUMENT),
        status = runCatching { MaterialStatus.valueOf(status) }.getOrDefault(MaterialStatus.PROCESSING),
        createdAt = createdAt,
        sessionCount = sessionCount,
        questionCount = questionCount,
        assessedLevel = assessedLevel,
        declaredLevel = declaredLevel,
        gateReason = gateReason,
        topicSummary = topicSummary,
        unfinished = unfinishedSession(),
    )

private fun MaterialEntity.unfinishedSession(): UnfinishedSession? {
    val sessionId = unfinishedSessionId ?: return null
    return UnfinishedSession(
        sessionId = sessionId,
        answered = unfinishedAnswered ?: 0,
        total = unfinishedTotal ?: 0,
    )
}

private const val FALLBACK_TITLE = "Materi"
private const val STATUS_READY = "ready"
private const val STATUS_REJECTED = "rejected"
private const val STATUS_FAILED = "failed"
