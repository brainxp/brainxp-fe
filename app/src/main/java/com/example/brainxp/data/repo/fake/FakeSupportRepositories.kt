package com.example.brainxp.data.repo.fake

import com.example.brainxp.core.result.ApiError
import com.example.brainxp.core.result.AppResult
import com.example.brainxp.data.repo.ActivityLogRepository
import com.example.brainxp.data.repo.FamilyRepository
import com.example.brainxp.domain.model.ActivityEvent
import com.example.brainxp.domain.model.ChildConfig
import com.example.brainxp.domain.model.FamilyChild
import com.example.brainxp.domain.model.PairingResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeActivityLogRepository
    @Inject
    constructor(
        private val backend: FakeBackend,
    ) : ActivityLogRepository {
        private val log = MutableStateFlow(FakeData.activityLog())
        private var pending = 0

        override fun observeRecent(limit: Int): Flow<List<ActivityEvent>> =
            log.asStateFlow().map { events -> events.sortedByDescending { it.timestamp }.take(limit) }

        override suspend fun record(event: ActivityEvent): AppResult<Unit> =
            backend.respond(FakeBackend.ACTIVITY_RECORD) {
                log.update { listOf(event) + it }
                pending++
            }

        override suspend fun flushPending(): AppResult<Int> =
            backend.respond(FakeBackend.ACTIVITY_FLUSH) {
                val flushed = pending
                pending = 0
                flushed
            }
    }

@Singleton
class FakeFamilyRepository
    @Inject
    constructor(
        private val backend: FakeBackend,
    ) : FamilyRepository {
        private val configs = ConcurrentHashMap<String, ChildConfig>()
        private val paired = MutableStateFlow(FakeData.children())

        override suspend fun children(): AppResult<List<FamilyChild>> =
            backend.respond(FakeBackend.FAMILY_CHILDREN) {
                paired.value
            }

        override suspend fun pair(code: String): AppResult<PairingResult> {
            if (code.length != CODE_LENGTH || code.any { !it.isDigit() }) {
                return AppResult.Failure(ApiError.Validation("code", "kode harus $CODE_LENGTH angka"))
            }
            if (code == EXPIRED_CODE) {
                return AppResult.Failure(ApiError.Validation("code", "kode sudah kedaluwarsa"))
            }

            return backend.respond(FakeBackend.FAMILY_PAIR) {
                val child =
                    FamilyChild(
                        childId = "child-${paired.value.size + 1}",
                        name = "Anak ${paired.value.size + 1}",
                        guardianStatus = com.example.brainxp.domain.model.GuardianStatus.UNKNOWN,
                        availableMinutes = 0,
                        sessionsThisWeek = 0,
                        accuracy = null,
                    )
                paired.update { it + child }
                PairingResult(child.childId, child.name)
            }
        }

        override suspend fun updateConfig(
            childId: String,
            config: ChildConfig,
        ): AppResult<ChildConfig> {
            if (paired.value.none { it.childId == childId }) {
                return AppResult.Failure(ApiError.Unknown(NOT_FOUND, "child $childId"))
            }
            if (config.dailyCapMinutes <= 0) {
                return AppResult.Failure(ApiError.Validation("dailyCapMinutes", "harus lebih dari nol"))
            }

            return backend.respond(FakeBackend.FAMILY_CONFIG) {
                configs[childId] = config
                config
            }
        }

        fun configFor(childId: String): ChildConfig = configs[childId] ?: FakeData.defaultChildConfig()

        private companion object {
            const val CODE_LENGTH = 6
            const val EXPIRED_CODE = "000000"
            const val NOT_FOUND = 404
        }
    }
