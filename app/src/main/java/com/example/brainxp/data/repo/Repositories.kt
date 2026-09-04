package com.example.brainxp.data.repo

import com.example.brainxp.core.result.AppResult
import com.example.brainxp.domain.model.ActivityEvent
import com.example.brainxp.domain.model.AnswerVerdict
import com.example.brainxp.domain.model.ChildConfig
import com.example.brainxp.domain.model.ConsumptionEntry
import com.example.brainxp.domain.model.FamilyChild
import com.example.brainxp.domain.model.GenerationJob
import com.example.brainxp.domain.model.LedgerDirection
import com.example.brainxp.domain.model.LedgerEntry
import com.example.brainxp.domain.model.Material
import com.example.brainxp.domain.model.MaterialDetail
import com.example.brainxp.domain.model.MaterialPage
import com.example.brainxp.domain.model.MaterialType
import com.example.brainxp.domain.model.PairingResult
import com.example.brainxp.domain.model.QuestionSession
import com.example.brainxp.domain.model.RestrictedApp
import com.example.brainxp.domain.model.SessionMode
import com.example.brainxp.domain.model.SessionResult
import com.example.brainxp.domain.model.Standing
import kotlinx.coroutines.flow.Flow

interface MaterialRepository {
    suspend fun upload(
        title: String,
        type: MaterialType,
        contentUri: String?,
        ocrText: String?,
    ): AppResult<Material>

    suspend fun page(cursor: String?): AppResult<MaterialPage>

    suspend fun detail(materialId: String): AppResult<MaterialDetail>

    fun observeCached(): Flow<List<Material>>
}

interface SessionRepository {
    suspend fun requestGeneration(
        materialId: String,
        questionCount: Int,
        mode: SessionMode,
    ): AppResult<GenerationJob>

    suspend fun pollGeneration(jobId: String): AppResult<GenerationJob>

    suspend fun session(sessionId: String): AppResult<QuestionSession>

    suspend fun submitAnswer(
        sessionId: String,
        questionId: String,
        answer: String,
        clientTimestamp: Long,
    ): AppResult<AnswerVerdict>

    suspend fun complete(sessionId: String): AppResult<SessionResult>
}

interface RewardRepository {
    suspend fun standing(): AppResult<Standing>

    suspend fun reportConsumption(entries: List<ConsumptionEntry>): AppResult<Standing>

    suspend fun history(): AppResult<List<LedgerEntry>>

    suspend fun adjust(
        direction: LedgerDirection,
        seconds: Int,
        note: String,
    ): AppResult<Standing>
}

interface RestrictionRepository {
    fun observeRestricted(): Flow<List<RestrictedApp>>

    suspend fun setRestricted(
        packageName: String,
        enabled: Boolean,
    ): AppResult<Unit>

    suspend fun replaceRestricted(packageNames: List<String>): AppResult<Unit>
}

interface ActivityLogRepository {
    fun observeRecent(limit: Int): Flow<List<ActivityEvent>>

    suspend fun record(event: ActivityEvent): AppResult<Unit>

    suspend fun flushPending(): AppResult<Int>
}

interface FamilyRepository {
    suspend fun children(): AppResult<List<FamilyChild>>

    suspend fun pair(code: String): AppResult<PairingResult>

    suspend fun updateConfig(
        childId: String,
        config: ChildConfig,
    ): AppResult<ChildConfig>
}
