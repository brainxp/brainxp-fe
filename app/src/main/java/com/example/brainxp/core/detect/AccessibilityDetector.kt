package com.example.brainxp.core.detect

import com.example.brainxp.core.permission.PermissionReader
import com.example.brainxp.core.permission.SpecialPermission
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccessibilityDetector
    @Inject
    constructor(
        private val bridge: AccessibilityEventBridge,
        private val permissions: PermissionReader,
    ) : ForegroundAppDetector {
        override val foregroundPackage: Flow<String> = bridge.foregroundPackage.distinctUntilChanged()

        override fun isAvailable(): Boolean = enabled() && bridge.connected.value

        override fun missingRequirements(): List<SpecialPermission> =
            if (enabled()) emptyList() else listOf(SpecialPermission.ACCESSIBILITY)

        private fun enabled(): Boolean = permissions.isGranted(SpecialPermission.ACCESSIBILITY)
    }
