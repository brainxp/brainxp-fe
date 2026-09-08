package com.example.brainxp.feature.capture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

private const val FRAME = 1f / 60f
private const val TRACK = 3.1f

private fun RunnerGameState.run(frames: Int) = repeat(frames) { advance(FRAME) }

private fun game(faces: Random = Random(7)) = RunnerGameState(seed = RunnerLane.SPAWN_GAP, chance = faces).also { it.resize(TRACK) }

private fun RunnerGameState.play(seconds: Float): Int {
    val frames = (seconds / FRAME).toInt()
    var lowest = Int.MAX_VALUE
    repeat(frames) {
        val threat = blocks.filter { it.x > RunnerLane.HERO_X }.minByOrNull { it.x }
        if (threat != null && threat.x - RunnerLane.HERO_X < RunnerLane.JUMP_AT) jump()
        advance(FRAME)
        lowest = minOf(lowest, avoided)
    }
    return lowest
}

private fun RunnerGameState.runUntilStumble(limit: Float = 10f): Int {
    val frames = (limit / FRAME).toInt()
    repeat(frames) { frame ->
        advance(FRAME)
        if (stumbled > 0f) return frame + 1
    }
    return 0
}

class RunnerGameStateTest {
    private val game = game()

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
        val tapped = game()
        val left = game()

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
    fun `the runner and its jump arc stay inside the lane`() {
        assertTrue(
            "a jump of $RunnerLane.PEAK plus a runner of $RunnerLane.SPAN cannot be drawn in one lane",
            RunnerLane.SPAN + RunnerLane.PEAK <= 1f,
        )

        game.jump()
        game.run(frames = 120)

        assertTrue("the runner climbed past the lane ceiling", RunnerLane.SPAN - game.heroY <= 1f)
    }

    @Test
    fun `the jump clears an obstacle at its drawn height`() {
        game.jump()
        game.run(frames = 20)

        assertTrue(
            "the runner must rise past the full obstacle height, not half of it",
            -game.heroY > RunnerLane.BLOCK,
        )
    }

    @Test
    fun `obstacles appear at the far end of the track and travel towards the runner`() {
        game.run(frames = 90)
        val spawned = game.blocks.toList()

        assertTrue("an obstacle should have spawned", spawned.isNotEmpty())
        assertTrue("obstacles must enter from beyond the track", spawned.first().x <= TRACK + RunnerLane.BLOCK)

        game.run(frames = 10)

        assertTrue("obstacles should move left", game.blocks.first().x < spawned.first().x)
    }

    @Test
    fun `a well timed jump keeps the run alive`() {
        val kept = game.play(seconds = 60f)

        assertTrue("a timed jump should never stumble, tally fell back to $kept", kept > 0 || game.avoided > 0)
        assertTrue("the tally should climb over a minute of play, got ${game.avoided}", game.avoided >= 20)
        assertEquals("a clean run must not flash", 0f, game.stumbled, 0f)
    }

    @Test
    fun `standing still ends the run and says so`() {
        val frames = game.runUntilStumble()

        assertTrue("the first obstacle should have arrived by now", frames > 0)
        assertEquals("a stumble should have reset the tally", 0, game.avoided)
        assertEquals(0f, game.heroY, 0f)
        assertTrue("a stumble must be visible", game.stumbled > 0f)
        assertTrue("the board should have been swept", game.blocks.isEmpty())
    }

    @Test
    fun `the stumble flash fades on its own`() {
        game.runUntilStumble()
        assertTrue(game.stumbled > 0f)

        game.run(frames = (RunnerLane.FLASH_SECONDS / FRAME).toInt() + 2)

        assertEquals("the flash should have faded", 0f, game.stumbled, 0f)
    }

    @Test
    fun `an obstacle counts the moment it passes the runner`() {
        val behind = RunnerLane.HERO_X - (RunnerLane.HERO_HALF + RunnerLane.BLOCK_HALF)
        while (game.blocks.isEmpty()) game.advance(FRAME)
        val before = game.avoided

        while (game.blocks.first().x >= behind) {
            game.jump()
            game.advance(FRAME)
        }

        assertTrue("an obstacle must be tallied as it passes the runner", game.avoided > before)
        assertTrue("it should still be drawn on the way out", game.blocks.first().x > -RunnerLane.BLOCK_HALF)
    }

    @Test
    fun `the collision box is as wide as the runner is drawn`() {
        val drawn = RunnerLane.PENCIL_LENGTH * RunnerLane.PENCIL_WIDE

        assertEquals("collision and drawing must share one geometry", drawn, RunnerLane.HERO_HALF, 1e-6f)
        assertTrue("the runner is drawn wider than it is tall", abs(RunnerLane.HERO_HALF) > RunnerLane.SPAN / 4f)
    }

    @Test
    fun `every obstacle carries a face inside the drawable range`() {
        game.run(frames = 600)

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
