package com.example.brainxp.data.repo

import com.example.brainxp.core.result.AppResult
import com.example.brainxp.domain.model.AcademicLevel
import com.example.brainxp.domain.model.ActivityEvent
import com.example.brainxp.domain.model.ConsumptionEntry
import com.example.brainxp.domain.model.DeviceBinding
import com.example.brainxp.domain.model.FamilyChild
import com.example.brainxp.domain.model.GuardianStatus
import com.example.brainxp.domain.model.LedgerDirection
import com.example.brainxp.domain.model.LedgerEntry
import com.example.brainxp.domain.model.Material
import com.example.brainxp.domain.model.MaterialPage
import com.example.brainxp.domain.model.MaterialType
import com.example.brainxp.domain.model.PairingCode
import com.example.brainxp.domain.model.Progress
import com.example.brainxp.domain.model.Report
import com.example.brainxp.domain.model.RestrictedApp
import com.example.brainxp.domain.model.Standing
import kotlinx.coroutines.flow.Flow

interface MaterialRepository {
    suspend fun upload(
        title: String,
        type: MaterialType,
        contentUri: String?,
    ): AppResult<Material>

    suspend fun page(cursor: String?): AppResult<MaterialPage>

    suspend fun detail(materialId: String): AppResult<Material>

    suspend fun delete(materialId: String): AppResult<Unit>

    fun observeCached(): Flow<List<Material>>
}

interface RewardRepository {
    suspend fun standing(): AppResult<Standing>

    suspend fun progress(): AppResult<Progress>

    suspend fun reportConsumption(entries: List<ConsumptionEntry>): AppResult<Standing>

    suspend fun history(): AppResult<List<LedgerEntry>>

    suspend fun report(
        days: Int,
        subjectId: String? = null,
    ): AppResult<Report>

    suspend fun adjust(
        direction: LedgerDirection,
        seconds: Int,
        note: String,
        subjectId: String? = null,
    ): AppResult<Standing>

    suspend fun standingOf(subjectId: String): AppResult<Standing>
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

    suspend fun createChild(
        name: String,
        level: AcademicLevel,
        language: String,
    ): AppResult<FamilyChild>

    suspend fun pairingCode(childId: String): AppResult<PairingCode>

    suspend fun pair(code: String): AppResult<Unit>

    suspend fun checkBinding(): AppResult<DeviceBinding>

    suspend fun reportHealth(status: GuardianStatus): AppResult<Unit>
}
