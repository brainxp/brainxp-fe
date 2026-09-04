package com.example.brainxp.core.detect

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BlockingAccessibilityService : AccessibilityService() {
    @Inject
    lateinit var bridge: AccessibilityEventBridge

    override fun onServiceConnected() {
        super.onServiceConnected()
        bridge.setConnected(true)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }
        event.packageName?.toString()?.let(bridge::publish)
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        bridge.setConnected(false)
        super.onDestroy()
    }
}
