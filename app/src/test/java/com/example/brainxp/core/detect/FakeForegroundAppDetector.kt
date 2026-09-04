package com.example.brainxp.core.detect

import com.example.brainxp.core.permission.SpecialPermission
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow

class FakeForegroundAppDetector(
    var available: Boolean = true,
    var missing: List<SpecialPermission> = emptyList(),
) : ForegroundAppDetector {
    private val packages = MutableSharedFlow<String>(extraBufferCapacity = BUFFER)

    var collections = 0
        private set

    override val foregroundPackage: Flow<String> =
        flow {
            collections++
            packages.collect { emit(it) }
        }

    override fun isAvailable(): Boolean = available

    override fun missingRequirements(): List<SpecialPermission> = missing

    suspend fun emit(packageName: String) {
        packages.emit(packageName)
    }

    private companion object {
        const val BUFFER = 16
    }
}
