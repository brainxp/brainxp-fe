package com.example.brainxp.data.repo.fake

import com.example.brainxp.core.result.ApiError
import com.example.brainxp.core.result.AppResult
import com.example.brainxp.data.repo.MaterialRepository
import com.example.brainxp.domain.model.Material
import com.example.brainxp.domain.model.MaterialDetail
import com.example.brainxp.domain.model.MaterialPage
import com.example.brainxp.domain.model.MaterialStatus
import com.example.brainxp.domain.model.MaterialType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeMaterialRepository
    @Inject
    constructor(
        private val backend: FakeBackend,
    ) : MaterialRepository {
        private val cache = MutableStateFlow(FakeData.materials())

        override fun observeCached(): StateFlow<List<Material>> = cache.asStateFlow()

        override suspend fun upload(
            title: String,
            type: MaterialType,
            contentUri: String?,
        ): AppResult<Material> =
            backend.respond(FakeBackend.MATERIAL_UPLOAD) {
                val material =
                    Material(
                        id = "mat-${UUID.randomUUID().toString().take(ID_LENGTH)}",
                        title = title.ifBlank { "Materi tanpa judul" },
                        type = type,
                        status = MaterialStatus.PROCESSING,
                        charCount = 0,
                        createdAt = FakeData.now,
                        sessionCount = 0,
                    )
                cache.update { listOf(material) + it }
                material
            }

        override suspend fun page(cursor: String?): AppResult<MaterialPage> =
            backend.respond(FakeBackend.MATERIAL_PAGE) {
                val all = cache.value
                val start = cursor?.toIntOrNull() ?: 0
                val end = minOf(start + PAGE_SIZE, all.size)
                MaterialPage(
                    items = all.subList(start.coerceAtMost(all.size), end),
                    nextCursor = if (end < all.size) end.toString() else null,
                )
            }

        override suspend fun delete(materialId: String): AppResult<Unit> =
            backend.respond(FakeBackend.MATERIAL_DELETE) {
                cache.value = cache.value.filterNot { it.id == materialId }
            }

        override suspend fun detail(materialId: String): AppResult<MaterialDetail> {
            val material =
                cache.value.firstOrNull { it.id == materialId }
                    ?: return AppResult.Failure(ApiError.Unknown(NOT_FOUND, "material $materialId"))

            return backend.respond(FakeBackend.MATERIAL_DETAIL) {
                MaterialDetail(material = material, sessions = FakeData.sessionSummaries(materialId))
            }
        }

        private companion object {
            const val PAGE_SIZE = 3
            const val ID_LENGTH = 6
            const val NOT_FOUND = 404
        }
    }
