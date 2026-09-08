package com.example.brainxp.feature.capture

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Bell
import com.composables.icons.lucide.Gamepad2
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Smartphone
import com.composables.icons.lucide.Tv
import com.example.brainxp.R
import com.example.brainxp.core.ui.BrainXPTheme
import com.example.brainxp.core.ui.Tokens
import kotlin.random.Random

private const val GRAVITY = 5.2f
private const val JUMP_PUSH = -1.65f
private const val GROUND = 0f
private const val LANE_SPEED = 0.44f
private const val SPAWN_GAP = 0.62f
private const val SPAWN_JITTER = 0.28f
private const val HERO_X = 0.16f
private const val HERO_HALF = 0.052f
private const val BLOCK_HALF = 0.042f
private const val OFF_SCREEN = -0.12f
private const val MAX_FRAME_SECONDS = 0.05f
private const val NANOS_PER_SECOND = 1_000_000_000f
private const val FACE_COUNT = 4

data class Obstacle(
    val x: Float,
    val face: Int,
)

class RunnerGameState internal constructor(
    seed: Float,
    private val faces: Random = Random.Default,
) {
    var heroY by mutableFloatStateOf(GROUND)
        private set
    var avoided by mutableIntStateOf(0)
        private set

    internal val blocks = mutableStateListOf<Obstacle>()
    private var velocity = 0f
    private var nextSpawn = seed

    val airborne: Boolean get() = heroY < GROUND

    val lift: Float get() = (-heroY / PEAK_HEIGHT).coerceIn(0f, 1f)

    fun jump() {
        if (!airborne) velocity = JUMP_PUSH
    }

    internal fun advance(seconds: Float) {
        velocity += GRAVITY * seconds
        heroY = (heroY + velocity * seconds).coerceAtMost(GROUND)
        if (heroY == GROUND) velocity = 0f

        nextSpawn -= seconds
        if (nextSpawn <= 0f) {
            blocks += Obstacle(x = 1f + BLOCK_HALF, face = faces.nextInt(FACE_COUNT))
            nextSpawn = SPAWN_GAP + SPAWN_JITTER * ((blocks.size * SPAWN_SALT) % 1f)
        }

        for (index in blocks.indices) {
            blocks[index] = blocks[index].copy(x = blocks[index].x - LANE_SPEED * seconds)
        }

        val passed = blocks.count { it.x < OFF_SCREEN }
        if (passed > 0) {
            repeat(passed) { blocks.removeAt(0) }
            avoided += passed
        }

        if (blocks.any { struck(it.x) }) restart()
    }

    private fun struck(block: Float): Boolean = kotlin.math.abs(block - HERO_X) < (HERO_HALF + BLOCK_HALF) && heroY > -HERO_HALF

    private fun restart() {
        blocks.clear()
        heroY = GROUND
        velocity = 0f
        avoided = 0
        nextSpawn = SPAWN_GAP
    }

    private companion object {
        const val SPAWN_SALT = 0.37f
        const val PEAK_HEIGHT = 0.262f
    }
}

@Composable
fun rememberRunnerGameState(): RunnerGameState = remember { RunnerGameState(seed = SPAWN_GAP) }

private enum class HopPhase {
    GROUNDED,
    TAKEOFF,
    PEAK,
}

private fun hopPhaseOf(
    airborne: Boolean,
    lift: Float,
): HopPhase =
    when {
        !airborne -> HopPhase.GROUNDED
        lift < PEAK_AT -> HopPhase.TAKEOFF
        else -> HopPhase.PEAK
    }

private fun stretchOf(phase: HopPhase): Float =
    when (phase) {
        HopPhase.GROUNDED -> 1f
        HopPhase.TAKEOFF -> SQUASH
        HopPhase.PEAK -> STRETCH
    }

private class PencilShape(
    val body: Path,
    val tip: Path,
)

private fun unitPencil(): PencilShape {
    val shoulder = HALF - TIP_SHARE
    val body =
        Path().apply {
            moveTo(-HALF, -HALF)
            lineTo(shoulder, -HALF)
            lineTo(shoulder, HALF)
            lineTo(-HALF, HALF)
            close()
        }
    val tip =
        Path().apply {
            moveTo(shoulder, -HALF)
            lineTo(HALF, 0f)
            lineTo(shoulder, HALF)
            close()
        }
    return PencilShape(body = body, tip = tip)
}

private fun DrawScope.drawPencil(
    pencil: PencilShape,
    centre: Offset,
    length: Float,
    width: Float,
    stretch: Float,
) {
    withTransform({
        translate(centre.x, centre.y)
        rotate(degrees = TILT, pivot = Offset.Zero)
        scale(scaleX = length * stretch, scaleY = width / stretch, pivot = Offset.Zero)
    }) {
        drawPath(pencil.body, Tokens.Blue300)
        drawPath(pencil.tip, Tokens.Alert)
    }
}

@Composable
fun RunnerGame(
    state: RunnerGameState,
    modifier: Modifier = Modifier,
    running: Boolean = true,
) {
    LaunchedEffect(running, state) {
        if (!running) return@LaunchedEffect
        var previous = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val seconds = ((now - previous) / NANOS_PER_SECOND).coerceAtMost(MAX_FRAME_SECONDS)
            previous = now
            state.advance(seconds)
        }
    }

    val board = stringResource(R.string.runner_board)
    val pencil = remember { unitPencil() }
    val faces = rememberObstacleFaces()
    val faceTint = remember { ColorFilter.tint(Tokens.Amber400) }
    val stretch by animateFloatAsState(
        targetValue = stretchOf(hopPhaseOf(state.airborne, state.lift)),
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "hop",
    )

    Surface(
        modifier = modifier.fillMaxWidth().height(BOARD),
        shape = MaterialTheme.shapes.large,
        color = Color.White.copy(alpha = BOARD_FILL),
    ) {
        Canvas(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = board }
                    .pointerInput(state) { detectTapGestures { state.jump() } },
        ) {
            val ground = size.height * GROUND_LINE
            val hero = size.width * HERO_HALF * 2f

            drawLine(
                color = Color.White.copy(alpha = LINE_FILL),
                start = Offset(0f, ground),
                end = Offset(size.width, ground),
                strokeWidth = size.height * LINE_WEIGHT,
            )

            val side = size.width * BLOCK_HALF * 2f
            state.blocks.forEach { block ->
                withTransform({
                    translate(size.width * block.x - side / 2f, ground - side)
                }) {
                    with(faces[block.face % faces.size]) {
                        draw(size = Size(side, side), colorFilter = faceTint)
                    }
                }
            }

            val length = hero * PENCIL_LENGTH
            val bottom = ground + size.width * state.heroY
            drawPencil(
                pencil = pencil,
                centre = Offset(size.width * HERO_X, bottom - length / 2f),
                length = length,
                width = hero * PENCIL_WIDTH,
                stretch = stretch,
            )
        }
    }
}

@Composable
private fun rememberObstacleFaces() =
    listOf(
        rememberVectorPainter(Lucide.Bell),
        rememberVectorPainter(Lucide.Smartphone),
        rememberVectorPainter(Lucide.Gamepad2),
        rememberVectorPainter(Lucide.Tv),
    )

@Composable
fun RunnerPanel(
    state: RunnerGameState,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
    running: Boolean = true,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(BrainXPTheme.spacing.sm)) {
        RunnerGame(state = state, running = running)

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BrainXPTheme.spacing.sm),
        ) {
            Text(
                text = stringResource(R.string.runner_avoided, state.avoided),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = LABEL_INK),
                modifier = Modifier.weight(1f),
            )
            OutlinedButton(onClick = onSkip, shape = MaterialTheme.shapes.medium) {
                Text(
                    text = stringResource(R.string.runner_skip),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = LABEL_INK),
                )
            }
        }

        OutlinedButton(
            onClick = state::jump,
            enabled = running,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
        ) {
            Text(
                text = stringResource(R.string.runner_jump),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
            )
        }
    }
}

private val BOARD = 132.dp
private const val BOARD_FILL = 0.07f
private const val LINE_FILL = 0.22f
private const val LINE_WEIGHT = 0.012f
private const val GROUND_LINE = 0.78f
private const val PENCIL_LENGTH = 2.3f
private const val PENCIL_WIDTH = 0.66f
private const val LABEL_INK = 0.68f
private const val TILT = -38f
private const val HALF = 0.5f
private const val TIP_SHARE = 0.3f
private const val PEAK_AT = 0.55f
private const val SQUASH = 0.86f
private const val STRETCH = 1.14f

private fun previewGame(faces: List<Int>): RunnerGameState =
    RunnerGameState(seed = SPAWN_GAP).also { game ->
        faces.forEachIndexed { index, face ->
            game.blocks += Obstacle(x = PREVIEW_FIRST + index * PREVIEW_GAP, face = face)
        }
    }

private const val PREVIEW_FIRST = 0.44f
private const val PREVIEW_GAP = 0.26f
private val PREVIEW_FACES = listOf(0, 1, 2, 3)
private val PREVIEW_TRIO = listOf(0, 1, 2)

@Preview(name = "Runner grounded", showBackground = true, backgroundColor = 0xFF08152F)
@Composable
private fun RunnerGroundedPreview() {
    BrainXPTheme(darkTheme = true) {
        Box(modifier = Modifier.padding(BrainXPTheme.spacing.lg)) {
            RunnerPanel(state = previewGame(PREVIEW_TRIO), onSkip = {}, running = false)
        }
    }
}

@Preview(name = "Runner obstacle faces", showBackground = true, backgroundColor = 0xFF08152F)
@Composable
private fun RunnerFacesPreview() {
    BrainXPTheme(darkTheme = true) {
        Box(modifier = Modifier.padding(BrainXPTheme.spacing.lg)) {
            RunnerGame(state = previewGame(PREVIEW_FACES), running = false)
        }
    }
}
