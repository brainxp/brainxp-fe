package com.example.brainxp.core.detect

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccessibilityEventBridge
    @Inject
    constructor() {
        private val events =
            MutableSharedFlow<String>(
                extraBufferCapacity = BUFFER,
                onBufferOverflow = BufferOverflow.DROP_OLDEST,
            )
        private val connectedState = MutableStateFlow(false)

        val foregroundPackage: Flow<String> = events.asSharedFlow()

        val connected: StateFlow<Boolean> = connectedState.asStateFlow()

        fun publish(packageName: String) {
            events.tryEmit(packageName)
        }

        fun setConnected(value: Boolean) {
            connectedState.value = value
        }

        private companion object {
            const val BUFFER = 32
        }
    }
