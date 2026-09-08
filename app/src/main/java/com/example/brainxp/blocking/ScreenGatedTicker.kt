package com.example.brainxp.blocking

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transformLatest

class ScreenGatedTicker(
    private val screenOn: Flow<Boolean>,
    private val intervalMs: Long,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun ticks(): Flow<Long> =
        screenOn.transformLatest { on ->
            if (!on) {
                return@transformLatest
            }
            var index = 0L
            while (true) {
                emit(index++)
                delay(intervalMs)
            }
        }
}
