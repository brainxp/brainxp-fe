package com.example.brainxp.feature.home

import com.example.brainxp.core.result.AppResult
import com.example.brainxp.data.prefs.AuthDataStore
import com.example.brainxp.data.repo.FamilyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileNameSource
    @Inject
    constructor(
        private val auth: AuthDataStore,
        private val family: FamilyRepository,
    ) {
        val name: Flow<String> =
            auth.auth
                .map { snapshot -> snapshot.displayName.orEmpty() }
                .distinctUntilChanged()

        suspend fun refresh() {
            val snapshot = auth.current()
            val subject = snapshot.subjectId ?: return
            val fresh =
                (family.children() as? AppResult.Success)
                    ?.value
                    ?.firstOrNull { child -> child.childId == subject }
                    ?.name
                    ?.takeIf { name -> name.isNotBlank() && name != snapshot.displayName }
            if (fresh != null) auth.saveDisplayName(fresh)
        }
    }
