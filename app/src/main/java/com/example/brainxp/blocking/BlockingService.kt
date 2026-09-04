package com.example.brainxp.blocking

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.SystemClock
import com.example.brainxp.core.detect.ForegroundAppDetector
import com.example.brainxp.core.detect.ScreenState
import com.example.brainxp.data.repo.RestrictionRepository
import com.example.brainxp.di.DefaultDispatcher
import com.example.brainxp.domain.RestrictionPolicy
import com.example.brainxp.domain.model.RestrictionState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BlockingService : Service() {
    @Inject
    lateinit var detector: ForegroundAppDetector

    @Inject
    lateinit var screenState: ScreenState

    @Inject
    lateinit var restrictions: RestrictionRepository

    @Inject
    lateinit var notification: ProtectionNotification

    @Inject
    @DefaultDispatcher
    lateinit var dispatcher: CoroutineDispatcher

    private lateinit var scope: CoroutineScope

    private val foregroundPackage = MutableStateFlow<String?>(null)
    private val restrictionState = MutableStateFlow(RestrictionState())
    private val blocked = MutableStateFlow(false)

    override fun onCreate() {
        super.onCreate()
        scope = CoroutineScope(SupervisorJob() + dispatcher)
        notification.createChannel()
        startForeground(ProtectionNotification.ID, render())
        observeRestrictions()
        observeForegroundPackage()
        startTicking()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun observeRestrictions() {
        scope.launch {
            restrictions
                .observeRestricted()
                .map { apps -> apps.filter { it.enabled }.map { it.packageName }.toSet() }
                .collect { packages ->
                    restrictionState.value = restrictionState.value.copy(restrictedPackages = packages)
                }
        }
    }

    private fun observeForegroundPackage() {
        scope.launch {
            detector.foregroundPackage.collect { foregroundPackage.value = it }
        }
    }

    private fun startTicking() {
        scope.launch {
            ScreenGatedTicker(screenState.isScreenOn, TICK_INTERVAL_MS).ticks().collect { tick() }
        }
    }

    private fun tick() {
        val current = foregroundPackage.value
        blocked.value =
            current != null &&
            RestrictionPolicy.isBlocked(current, restrictionState.value, SystemClock.elapsedRealtime())
        getSystemService(NotificationManager::class.java).notify(ProtectionNotification.ID, render())
    }

    private fun render() = notification.build(restrictionState.value, blocked.value, SystemClock.elapsedRealtime())

    private companion object {
        const val TICK_INTERVAL_MS = 600L
    }
}
