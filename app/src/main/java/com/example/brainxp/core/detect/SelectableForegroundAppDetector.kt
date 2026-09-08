package com.example.brainxp.core.detect

import com.example.brainxp.core.permission.SpecialPermission
import com.example.brainxp.data.prefs.DetectorChoice
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest

class SelectableForegroundAppDetector(
    private val usageStats: ForegroundAppDetector,
    private val accessibility: ForegroundAppDetector,
    private val choice: StateFlow<DetectorChoice>,
) : ForegroundAppDetector {
    @OptIn(ExperimentalCoroutinesApi::class)
    override val foregroundPackage: Flow<String> = choice.flatMapLatest { working(it).foregroundPackage }

    override fun isAvailable(): Boolean = selected(choice.value).isAvailable()

    override fun missingRequirements(): List<SpecialPermission> = selected(choice.value).missingRequirements()

    private fun working(value: DetectorChoice): ForegroundAppDetector {
        val chosen = selected(value)
        if (chosen.isAvailable()) return chosen
        val spare = selected(other(value))
        return if (spare.isAvailable()) spare else chosen
    }

    private fun other(value: DetectorChoice): DetectorChoice =
        when (value) {
            DetectorChoice.USAGE_STATS -> DetectorChoice.ACCESSIBILITY
            DetectorChoice.ACCESSIBILITY -> DetectorChoice.USAGE_STATS
        }

    private fun selected(value: DetectorChoice): ForegroundAppDetector =
        when (value) {
            DetectorChoice.USAGE_STATS -> usageStats
            DetectorChoice.ACCESSIBILITY -> accessibility
        }
}
