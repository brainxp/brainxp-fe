package com.example.brainxp.di

import com.example.brainxp.core.detect.AccessibilityDetector
import com.example.brainxp.core.detect.ForegroundAppDetector
import com.example.brainxp.core.detect.SelectableForegroundAppDetector
import com.example.brainxp.core.detect.UsageStatsDetector
import com.example.brainxp.data.prefs.DetectorChoice
import com.example.brainxp.data.prefs.SettingsDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DetectorModule {
    @Provides
    @Singleton
    @AppScope
    fun provideAppScope(
        @DefaultDispatcher dispatcher: CoroutineDispatcher,
    ): CoroutineScope = CoroutineScope(SupervisorJob() + dispatcher)

    @Provides
    @Singleton
    fun provideDetectorChoice(
        settings: SettingsDataStore,
        @AppScope scope: CoroutineScope,
    ): StateFlow<DetectorChoice> =
        settings.settings
            .map { it.detector }
            .distinctUntilChanged()
            .stateIn(
                scope = scope,
                started = SharingStarted.Eagerly,
                initialValue = DetectorChoice.USAGE_STATS,
            )

    @Provides
    @Singleton
    fun provideForegroundAppDetector(
        usageStats: UsageStatsDetector,
        accessibility: AccessibilityDetector,
        choice: @JvmSuppressWildcards StateFlow<DetectorChoice>,
    ): ForegroundAppDetector =
        SelectableForegroundAppDetector(
            usageStats = usageStats,
            accessibility = accessibility,
            choice = choice,
        )
}
