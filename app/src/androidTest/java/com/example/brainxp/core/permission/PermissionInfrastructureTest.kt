package com.example.brainxp.core.permission

import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class PermissionInfrastructureTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var provider: PermissionStateProvider

    @Inject
    lateinit var reader: PermissionReader

    @Inject
    lateinit var intents: PermissionIntents

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun readerAnswersForEverySpecialPermissionWithoutThrowing() {
        SpecialPermission.entries.forEach { permission ->
            reader.isGranted(permission)
        }
    }

    @Test
    fun providerExposesOneEntryPerSpecialPermission() {
        val snapshot = provider.state.value

        assertEquals(SpecialPermission.entries, snapshot.entries.map { it.permission })
    }

    @Test
    fun providerAgreesWithTheReader() {
        provider.refresh()

        SpecialPermission.entries.forEach { permission ->
            assertEquals(
                "status mismatch for $permission",
                reader.isGranted(permission),
                provider.state.value.isGranted(permission),
            )
        }
    }

    @Test
    fun everyPermissionOffersAPrimaryAndAFallbackIntent() {
        SpecialPermission.entries.forEach { permission ->
            val candidates = intents.settingsIntents(permission)

            assertEquals("candidate count for $permission", 2, candidates.size)
            assertEquals(
                "fallback action for $permission",
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                candidates.last().action,
            )
        }
    }

    @Test
    fun primaryIntentsUseTheExpectedSettingsAction() {
        val expected =
            mapOf(
                SpecialPermission.USAGE_ACCESS to Settings.ACTION_USAGE_ACCESS_SETTINGS,
                SpecialPermission.OVERLAY to Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                SpecialPermission.NOTIFICATIONS to Settings.ACTION_APP_NOTIFICATION_SETTINGS,
                SpecialPermission.BATTERY_EXEMPTION to Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS,
                SpecialPermission.ACCESSIBILITY to Settings.ACTION_ACCESSIBILITY_SETTINGS,
            )

        expected.forEach { (permission, action) ->
            assertEquals(action, intents.settingsIntents(permission).first().action)
        }
    }

    @Test
    fun everyPermissionCanReachSettingsOnThisDevice() {
        SpecialPermission.entries.forEach { permission ->
            val reachable = intents.settingsIntents(permission).any { resolves(it) }

            assertTrue("no Settings screen resolves for $permission", reachable)
        }
    }

    @Test
    fun overlayAndNotificationIntentsTargetThisApp() {
        val overlay = intents.settingsIntents(SpecialPermission.OVERLAY).first()
        assertEquals(context.packageName, overlay.data?.schemeSpecificPart)

        val notifications = intents.settingsIntents(SpecialPermission.NOTIFICATIONS).first()
        assertEquals(context.packageName, notifications.getStringExtra(Settings.EXTRA_APP_PACKAGE))
    }

    @Test
    fun resumeRefreshesStateThroughARealLifecycle() {
        val recorder = RecordingProvider()
        val owner = RegistryOwner()

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            owner.registry.addObserver(PermissionResumeObserver(recorder))
            owner.registry.currentState = Lifecycle.State.RESUMED
        }

        assertEquals(1, recorder.refreshes)
    }

    @Test
    fun stateSurvivesRepeatedResumes() {
        val recorder = RecordingProvider()
        val owner = RegistryOwner()

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            owner.registry.addObserver(PermissionResumeObserver(recorder))
            owner.registry.currentState = Lifecycle.State.RESUMED
            owner.registry.currentState = Lifecycle.State.CREATED
            owner.registry.currentState = Lifecycle.State.RESUMED
        }

        assertEquals(2, recorder.refreshes)
    }

    private fun resolves(intent: Intent): Boolean = context.packageManager.resolveActivity(intent, 0) != null

    private class RegistryOwner : LifecycleOwner {
        val registry = LifecycleRegistry(this)

        override val lifecycle: Lifecycle get() = registry
    }

    private class RecordingProvider : PermissionStateProvider {
        var refreshes = 0

        override val state: StateFlow<PermissionSnapshot> get() = throw UnsupportedOperationException()

        override fun refresh() {
            refreshes++
        }

        override fun settingsIntents(permission: SpecialPermission): List<Intent> = emptyList()
    }
}
