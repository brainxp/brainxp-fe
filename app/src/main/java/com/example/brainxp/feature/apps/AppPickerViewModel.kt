package com.example.brainxp.feature.apps

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainxp.blocking.InstalledApp
import com.example.brainxp.blocking.InstalledAppsSource
import com.example.brainxp.blocking.SystemCriticalFilter
import com.example.brainxp.data.repo.RestrictionRepository
import com.example.brainxp.domain.GuardedAction
import com.example.brainxp.domain.ParentLock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppPickerUiState(
    val loading: Boolean = true,
    val query: String = "",
    val apps: List<InstalledApp> = emptyList(),
    val icons: Map<String, ImageBitmap> = emptyMap(),
    val restricted: Set<String> = emptySet(),
    val blocked: Boolean = false,
) {
    val visible: List<InstalledApp>
        get() =
            if (query.isBlank()) {
                apps
            } else {
                apps.filter { it.label.contains(query, ignoreCase = true) }
            }
}

@HiltViewModel
class AppPickerViewModel
    @Inject
    constructor(
        private val source: InstalledAppsSource,
        private val parentLock: ParentLock,
        private val restrictions: RestrictionRepository,
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(AppPickerUiState())

        val state: StateFlow<AppPickerUiState> = mutableState.asStateFlow()

        init {
            load()
            observeRestricted()
        }

        fun onQueryChange(value: String) {
            mutableState.value = mutableState.value.copy(query = value)
        }

        fun onToggle(packageName: String) {
            val enabled = packageName !in mutableState.value.restricted
            viewModelScope.launch {
                if (!parentLock.allows(GuardedAction.CHANGE_RESTRICTIONS)) {
                    mutableState.value = mutableState.value.copy(blocked = true)
                    return@launch
                }
                restrictions.setRestricted(packageName, enabled)
            }
        }

        private fun load() {
            viewModelScope.launch {
                val apps = SystemCriticalFilter.selectable(source.launchableApps(), source.protectedPackages())
                val icons = iconsOf(apps)
                mutableState.value = mutableState.value.copy(loading = false, apps = apps, icons = icons)
            }
        }

        private suspend fun iconsOf(apps: List<InstalledApp>): Map<String, ImageBitmap> {
            val icons = mutableMapOf<String, ImageBitmap>()
            apps.forEach { app ->
                source.icon(app.packageName)?.let { icons[app.packageName] = it.asImageBitmap() }
            }
            return icons.toMap()
        }

        private fun observeRestricted() {
            viewModelScope.launch {
                restrictions.observeRestricted().collect { list ->
                    mutableState.value =
                        mutableState.value.copy(
                            restricted = list.filter { it.enabled }.map { it.packageName }.toSet(),
                        )
                }
            }
        }
    }
