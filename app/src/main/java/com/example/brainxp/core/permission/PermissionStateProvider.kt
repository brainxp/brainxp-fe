package com.example.brainxp.core.permission

import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

interface PermissionStateProvider {
    val state: StateFlow<PermissionSnapshot>

    fun refresh()

    fun settingsIntents(permission: SpecialPermission): List<Intent>
}

@Singleton
class DefaultPermissionStateProvider
    @Inject
    constructor(
        private val reader: PermissionReader,
        private val intents: PermissionIntents,
    ) : PermissionStateProvider {
        private val mutableState = MutableStateFlow(read())

        override val state: StateFlow<PermissionSnapshot> = mutableState.asStateFlow()

        override fun refresh() {
            mutableState.value = read()
        }

        override fun settingsIntents(permission: SpecialPermission): List<Intent> = intents.settingsIntents(permission)

        private fun read(): PermissionSnapshot =
            PermissionSnapshot(
                SpecialPermission.entries.map { PermissionEntry(it, reader.isGranted(it)) },
            )
    }
