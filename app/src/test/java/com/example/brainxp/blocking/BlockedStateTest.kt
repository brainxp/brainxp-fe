package com.example.brainxp.blocking

import com.example.brainxp.domain.model.BlockReason
import org.junit.Assert.assertEquals
import org.junit.Test

class BlockedStateTest {
    private fun state(
        reason: BlockReason,
        balance: Int,
    ) = blockedStateOf(reason, balance)

    @Test
    fun `no balance and no reason reads as an empty balance`() {
        assertEquals(BlockedState.NO_BALANCE, state(BlockReason.NONE, balance = 0))
    }

    @Test
    fun `balance with no reason means the session was never started`() {
        assertEquals(BlockedState.NOT_STARTED, state(BlockReason.NONE, balance = 1_500))
    }

    @Test
    fun `an explicit empty balance still reads as empty`() {
        assertEquals(BlockedState.NO_BALANCE, state(BlockReason.NO_BALANCE, balance = 0))
    }

    @Test
    fun `the daily cap outranks a standing balance`() {
        assertEquals(BlockedState.DAILY_CAP, state(BlockReason.DAILY_CAP, balance = 1_500))
    }

    @Test
    fun `a held balance outranks the daily cap`() {
        assertEquals(BlockedState.IDLE_HOLD, state(BlockReason.IDLE, balance = 1_500))
    }

    @Test
    fun `a stale guardian outranks every other reason`() {
        assertEquals(
            BlockedState.GUARDIAN_STALE,
            state(BlockReason.GUARDIAN_STALE, balance = 1_500),
        )
        assertEquals(BlockedState.GUARDIAN_STALE, state(BlockReason.GUARDIAN_STALE, balance = 0))
    }

    @Test
    fun `a held balance is reported even when the balance reads zero`() {
        assertEquals(BlockedState.IDLE_HOLD, state(BlockReason.IDLE, balance = 0))
    }

    @Test
    fun `the cap is reported even when the balance reads zero`() {
        assertEquals(BlockedState.DAILY_CAP, state(BlockReason.DAILY_CAP, balance = 0))
    }
}
