package com.example.brainxp.data.repo

import com.example.brainxp.core.result.AppResult
import com.example.brainxp.data.db.RestrictedAppDao
import com.example.brainxp.data.db.RestrictedAppEntity
import com.example.brainxp.domain.model.RestrictedApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomRestrictionRepository
    @Inject
    constructor(
        private val dao: RestrictedAppDao,
    ) : RestrictionRepository {
        override fun observeRestricted(): Flow<List<RestrictedApp>> =
            dao.observeAll().map { rows ->
                rows.map { RestrictedApp(packageName = it.packageName, label = it.packageName, enabled = it.enabled) }
            }

        override suspend fun setRestricted(
            packageName: String,
            enabled: Boolean,
        ): AppResult<Unit> {
            dao.insertIgnoring(listOf(entity(packageName, enabled)))
            dao.setEnabled(packageName, enabled)
            return AppResult.Success(Unit)
        }

        override suspend fun replaceRestricted(packageNames: List<String>): AppResult<Unit> {
            dao.insertIgnoring(packageNames.map { entity(it, enabled = true) })
            val known = dao.observeAll().first().map { it.packageName }
            (known + packageNames).distinct().forEach { packageName ->
                dao.setEnabled(packageName, packageName in packageNames)
            }
            return AppResult.Success(Unit)
        }

        private fun entity(
            packageName: String,
            enabled: Boolean,
        ) = RestrictedAppEntity(
            packageName = packageName,
            enabled = enabled,
            addedAt = System.currentTimeMillis(),
        )
    }
