package com.example.brainxp.core.time

import android.os.SystemClock
import javax.inject.Inject
import javax.inject.Singleton

interface AppClock {
    fun elapsedRealtime(): Long

    fun wallClock(): Long
}

@Singleton
class SystemAppClock
    @Inject
    constructor() : AppClock {
        override fun elapsedRealtime(): Long = SystemClock.elapsedRealtime()

        override fun wallClock(): Long = System.currentTimeMillis()
    }
