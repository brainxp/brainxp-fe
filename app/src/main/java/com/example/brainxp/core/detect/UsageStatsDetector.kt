package com.example.brainxp.core.detect

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import com.example.brainxp.core.permission.PermissionReader
import com.example.brainxp.core.permission.SpecialPermission
import com.example.brainxp.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsageStatsDetector
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val permissions: PermissionReader,
        private val screenState: ScreenState,
        @IoDispatcher private val io: CoroutineDispatcher,
    ) : ForegroundAppDetector {
        @OptIn(ExperimentalCoroutinesApi::class)
        override val foregroundPackage: Flow<String> =
            screenState.isScreenOn
                .flatMapLatest { screenOn -> if (screenOn) pollLoop() else emptyFlow() }
                .flowOn(io)

        override fun isAvailable(): Boolean = permissions.isGranted(SpecialPermission.USAGE_ACCESS)

        override fun missingRequirements(): List<SpecialPermission> =
            if (isAvailable()) emptyList() else listOf(SpecialPermission.USAGE_ACCESS)

        private fun pollLoop(): Flow<String> =
            flow {
                var lastSeen: String? = null
                var window = CATCH_UP_WINDOW_MS
                while (true) {
                    latestResumedPackage(window)?.let { lastSeen = it }
                    window = QUERY_WINDOW_MS
                    lastSeen?.let { emit(it) }
                    delay(POLL_INTERVAL_MS)
                }
            }.distinctUntilChanged()

        private fun latestResumedPackage(windowMs: Long): String? {
            val manager = context.getSystemService(UsageStatsManager::class.java) ?: return null
            val now = System.currentTimeMillis()
            val events =
                try {
                    manager.queryEvents(now - windowMs, now)
                } catch (ignored: SecurityException) {
                    return null
                }

            val event = UsageEvents.Event()
            var latestPackage: String? = null
            var latestAt = Long.MIN_VALUE

            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED && event.timeStamp > latestAt) {
                    latestAt = event.timeStamp
                    latestPackage = event.packageName
                }
            }
            return latestPackage
        }

        private companion object {
            const val POLL_INTERVAL_MS = 600L
            const val QUERY_WINDOW_MS = 5_000L
            const val CATCH_UP_WINDOW_MS = 300_000L
        }
    }
