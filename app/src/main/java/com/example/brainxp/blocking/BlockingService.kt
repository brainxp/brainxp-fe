package com.example.brainxp.blocking

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.example.brainxp.MainActivity
import com.example.brainxp.core.detect.ForegroundAppDetector
import com.example.brainxp.core.detect.ScreenState
import com.example.brainxp.core.permission.PermissionStateProvider
import com.example.brainxp.data.prefs.SettingsDataStore
import com.example.brainxp.data.repo.RewardReconciler
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
import kotlinx.coroutines.flow.first
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
    lateinit var settings: SettingsDataStore

    @Inject
    lateinit var installedApps: InstalledAppsSource

    @Inject
    lateinit var rewards: RewardReconciler

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
    private var appLabels: Map<String, String> = emptyMap()

    override fun onCreate() {
        super.onCreate()
        scope = CoroutineScope(SupervisorJob() + dispatcher)
        notification.createChannel()
        expiryWarning.createChannel()
        startForeground(ProtectionNotification.ID, render())
        scope.launch { protection.reloadUnlock() }
        scope.launch { observeUnlockForWarning() }
        scope.launch { appLabels = installedApps.launchableApps().associate { it.packageName to it.label } }
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
            if (unlock !is UnlockState.Active) {
                expiryWarning.cancel()
                warnedForUnlock = null
            }
        }
    }

    private suspend fun maybeWarn(unlock: UnlockState) {
        if (unlock !is UnlockState.Active || warnedForUnlock == unlock.unlockId) {
            return
        }
        val remainingSeconds = unlocks.remaining().inWholeSeconds
        val lead = settings.settings.first().warningLeadSeconds
        if (remainingSeconds in 1..lead) {
            warnedForUnlock = unlock.unlockId
            expiryWarning.post(remainingSeconds.toInt())
        }
    }

    private suspend fun tick() {
        permissions.refresh()
        val snapshot = protection.snapshot.value
        val current = foregroundPackage.value
        val ours = current != null && current == packageName
        val unlock = unlocks.meter(current?.takeUnless { ours })
        maybeWarn(unlock)

        val restriction = snapshot.restriction.copy(unlock = unlock)
        val shouldBlock = current != null && !ours && RestrictionPolicy.isBlocked(current, restriction)

        if (shouldBlock) {
            clearTicks = 0
            blocked.value = true
            val blockedPackage = requireNotNull(current)
            val balance = rewards.state.value
            overlay.show(
                blockedPackage = blockedPackage,
                appLabel = appLabels[blockedPackage] ?: blockedPackage,
                info = blockedInfoOf(balance.standing, balance.balanceSeconds),
            ) { pkg, action ->
                when (action) {
                    BlockAction.STUDY -> launchEarnTime(pkg)
                    BlockAction.START_SESSION -> startSessionFrom(pkg)
                }
            }
        } else {
            clearTicks++
            if (ours || clearTicks >= CLEAR_TICKS_BEFORE_HIDE) {
                blocked.value = false
                overlay.hide()
            }
        }
        getSystemService(NotificationManager::class.java).notify(ProtectionNotification.ID, render())
    }

    private fun startSessionFrom(blockedPackage: String) {
        scope.launch {
            val seconds = rewards.state.value.balanceSeconds
            val allowed = protection.snapshot.value.restriction.restrictedPackages
            val alreadyRunning = unlocks.state.value is UnlockState.Active
            if (alreadyRunning || seconds <= 0 || allowed.isEmpty()) {
                launchEarnTime(blockedPackage)
                return@launch
            }
            unlocks.start(seconds, allowed)
            clearTicks = 0
            blocked.value = false
            overlay.hide()
        }
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
            remaining = unlocks.remaining(),
        )

    companion object {
        const val EXTRA_BLOCKED_PACKAGE = "blocked_package"
        private const val TICK_INTERVAL_MS = 600L
        private const val CLEAR_TICKS_BEFORE_HIDE = 3
    }
}
