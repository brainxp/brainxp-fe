package com.example.brainxp.blocking

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.SystemClock
import com.example.brainxp.MainActivity
import com.example.brainxp.core.detect.ForegroundAppDetector
import com.example.brainxp.core.detect.ScreenState
import com.example.brainxp.core.permission.PermissionStateProvider
import com.example.brainxp.di.DefaultDispatcher
import com.example.brainxp.domain.RestrictionPolicy
import com.example.brainxp.domain.UnlockSessionManager
import com.example.brainxp.domain.model.UnlockState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BlockingService : Service() {
    @Inject
    lateinit var detector: ForegroundAppDetector

    @Inject
    lateinit var screenState: ScreenState

    @Inject
    lateinit var permissions: PermissionStateProvider

    @Inject
    lateinit var protection: ProtectionStateHolder

    @Inject
    lateinit var unlocks: UnlockSessionManager

    @Inject
    lateinit var expiryWarning: ExpiryWarning

    @Inject
    lateinit var notification: ProtectionNotification

    @Inject
    lateinit var overlay: BlockOverlayController

    @Inject
    @DefaultDispatcher
    lateinit var dispatcher: CoroutineDispatcher

    private lateinit var scope: CoroutineScope

    private val foregroundPackage = MutableStateFlow<String?>(null)
    private val blocked = MutableStateFlow(false)
    private var clearTicks = 0
    private var warnedForUnlock: String? = null

    override fun onCreate() {
        super.onCreate()
        scope = CoroutineScope(SupervisorJob() + dispatcher)
        notification.createChannel()
        startForeground(ProtectionNotification.ID, render())
        scope.launch { protection.reloadUnlock() }
        scope.launch { observeUnlockForWarning() }
        scope.launch { detector.foregroundPackage.collect { foregroundPackage.value = it } }
        scope.launch {
            ScreenGatedTicker(screenState.isScreenOn, TICK_INTERVAL_MS).ticks().collect { tick() }
        }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        overlay.hide()
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun observeUnlockForWarning() {
        unlocks.state.collect { unlock ->
            if (unlock is UnlockState.Active) {
                expiryWarning.schedule(unlock)
            } else {
                expiryWarning.cancel()
                warnedForUnlock = null
            }
        }
    }

    private fun maybeWarn(unlock: UnlockState) {
        if (unlock !is UnlockState.Active || warnedForUnlock == unlock.unlockId) {
            return
        }
        val remaining = unlocks.remaining().inWholeMilliseconds
        if (remaining in 1..ExpiryWarning.LEAD_MILLIS) {
            warnedForUnlock = unlock.unlockId
            expiryWarning.post()
        }
    }

    private suspend fun tick() {
        permissions.refresh()
        val unlock = unlocks.evaluate()
        maybeWarn(unlock)
        val snapshot = protection.snapshot.value
        val current = foregroundPackage.value
        val ours = current != null && current == packageName
        val shouldBlock =
            current != null &&
                !ours &&
                RestrictionPolicy.isBlocked(current, snapshot.restriction, SystemClock.elapsedRealtime())

        if (shouldBlock) {
            clearTicks = 0
            blocked.value = true
            overlay.show(requireNotNull(current)) { launchEarnTime(it) }
        } else {
            clearTicks++
            if (ours || clearTicks >= CLEAR_TICKS_BEFORE_HIDE) {
                blocked.value = false
                overlay.hide()
            }
        }
        getSystemService(NotificationManager::class.java).notify(ProtectionNotification.ID, render())
    }

    private fun launchEarnTime(blockedPackage: String) {
        overlay.hide()
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(EXTRA_BLOCKED_PACKAGE, blockedPackage)
            },
        )
    }

    private fun render() =
        notification.build(
            snapshot = protection.snapshot.value,
            blocked = blocked.value,
            now = SystemClock.elapsedRealtime(),
        )

    companion object {
        const val EXTRA_BLOCKED_PACKAGE = "blocked_package"
        private const val TICK_INTERVAL_MS = 600L
        private const val CLEAR_TICKS_BEFORE_HIDE = 3
    }
}
