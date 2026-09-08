package com.example.brainxp.core.time

class FakeAppClock(
    var elapsed: Long = 0L,
    var wall: Long = 1_700_000_000_000L,
) : AppClock {
    override fun elapsedRealtime(): Long = elapsed

    override fun wallClock(): Long = wall

    fun advance(millis: Long) {
        elapsed += millis
        wall += millis
    }

    fun reboot(downtimeMillis: Long) {
        wall += downtimeMillis
        elapsed = 0L
    }

    fun moveWallClock(millis: Long) {
        wall += millis
    }
}
