package com.example.brainxp.core.detect

import com.example.brainxp.core.permission.SpecialPermission
import kotlinx.coroutines.flow.Flow

interface ForegroundAppDetector {
    val foregroundPackage: Flow<String>

    fun isAvailable(): Boolean

    fun missingRequirements(): List<SpecialPermission>
}
