package com.example.brainxp.feature.capture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val FRAME = 1f / 60f

private fun RunnerGameState.run(frames: Int) = repeat(frames) { advance(FRAME) }

class RunnerGameStateTest {
    private val game = RunnerGameState(seed = 0.6f)

    @Test
    fun `the runner starts on the ground`() {
        assertEquals(0f, game.heroY, 0f)
        assertTrue(!game.airborne)
    }

    @Test
    fun `a jump lifts the runner and gravity brings it back down`() {
        game.jump()
        game.run(frames = 4)

        assertTrue("the runner should have left the ground", game.airborne)

        game.run(frames = 120)

        assertEquals("the runner should have landed again", 0f, game.heroY, 0f)
    }

    @Test
    fun `jumping again mid air changes nothing`() {
        val tapped = RunnerGameState(seed = 0.6f)
        val left = RunnerGameState(seed = 0.6f)

        tapped.jump()
        left.jump()
        tapped.run(frames = 4)
        left.run(frames = 4)

        tapped.jump()

        tapped.run(frames = 20)
        left.run(frames = 20)

        assertEquals("a mid-air tap must not re-launch the runner", left.heroY, tapped.heroY, 0f)
    }

    @Test
    fun `obstacles appear and travel towards the runner`() {
        game.run(frames = 60)
        val spawned = game.blocks.toList()

        assertTrue("an obstacle should have spawned", spawned.isNotEmpty())

        game.run(frames = 10)

        assertTrue("obstacles should move left", game.blocks.first().x < spawned.first().x)
    }

    @Test
    fun `running into an obstacle resets the run and its tally`() {
        game.run(frames = 600)

        assertEquals("a collision should have reset the tally", 0, game.avoided)
        assertEquals(0f, game.heroY, 0f)
    }

    @Test
    fun `every obstacle carries a face inside the drawable range`() {
        game.run(frames = 240)

        assertTrue("obstacles should have spawned", game.blocks.isNotEmpty())
        game.blocks.forEach { block ->
            assertTrue("face ${block.face} is not drawable", block.face in 0..3)
        }
    }

    @Test
    fun `lift reads zero on the ground and climbs towards the peak`() {
        assertEquals(0f, game.lift, 0f)

        game.jump()
        game.run(frames = 4)
        val early = game.lift

        game.run(frames = 8)

        assertTrue("lift should rise while climbing", game.lift > early)
        assertTrue("lift must stay normalised", game.lift <= 1f)
    }

    @Test
    fun `the game never leaves the ground when it is not advanced`() {
        game.jump()

        assertEquals("no frames means no movement", 0f, game.heroY, 0f)
    }
}
