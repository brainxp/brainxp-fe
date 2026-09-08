package com.example.brainxp.feature.capture

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
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
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

internal object RunnerLane {
    const val GROUND_LINE = 0.78f
    const val SPAN = 0.40f
    const val PEAK = 0.50f
    const val BLOCK = 0.24f
    const val HERO_X = 0.5f
    const val SPEED = 2.2f
    const val AIRTIME = 0.78f
    const val SPAWN_GAP = 1.05f
    const val SPAWN_JITTER = 0.35f
    const val JUMP_AT = 0.86f
    const val TILT = -38f
    const val SLIM = 0.287f
    const val FLASH_SECONDS = 0.45f
    const val DEFAULT_TRACK = 3.1f
    const val SHORTEST_TRACK = 1.6f
    const val FACES = 4

    private const val DEGREES_TO_RADIANS = 0.017453292f
    private const val PEAK_TO_GRAVITY = 8f

    private val lean = abs(TILT) * DEGREES_TO_RADIANS
    private val rise = sin(lean)
    private val reach = cos(lean)

    val PENCIL_TALL = (rise + SLIM * reach) / 2f
    val PENCIL_WIDE = (reach + SLIM * rise) / 2f
    val PENCIL_LENGTH = SPAN / (2f * PENCIL_TALL)
    val HERO_HALF = PENCIL_LENGTH * PENCIL_WIDE
    const val BLOCK_HALF = BLOCK / 2f
    const val GRAVITY = PEAK_TO_GRAVITY * PEAK / (AIRTIME * AIRTIME)

    val JUMP_PUSH = -sqrt(2f * GRAVITY * PEAK)
}

private const val GROUND = 0f
private const val MAX_FRAME_SECONDS = 0.05f
private const val NANOS_PER_SECOND = 1_000_000_000f

data class Obstacle(
    val x: Float,
    val face: Int,
)

class RunnerGameState internal constructor(
    seed: Float,
    avoided: Int = 0,
    private val chance: Random = Random.Default,
) {
    var heroY by mutableFloatStateOf(GROUND)
        private set
    var avoided by mutableIntStateOf(avoided)
        private set
    var stumbled by mutableFloatStateOf(GROUND)
        private set

    internal val blocks = mutableStateListOf<Obstacle>()
    private var velocity = 0f
    private var nextSpawn = seed
    private var track = RunnerLane.DEFAULT_TRACK

    val airborne: Boolean get() = heroY < GROUND

    val lift: Float get() = (-heroY / RunnerLane.PEAK).coerceIn(0f, 1f)

    fun jump() {
        if (!airborne) velocity = RunnerLane.JUMP_PUSH
    }

    internal fun resize(lanes: Float) {
        track = lanes.coerceAtLeast(RunnerLane.SHORTEST_TRACK)
    }

    internal fun advance(seconds: Float) {
        velocity += RunnerLane.GRAVITY * seconds
        heroY = (heroY + velocity * seconds).coerceAtMost(GROUND)
        if (heroY == GROUND) velocity = 0f
        if (stumbled > 0f) stumbled = (stumbled - seconds / RunnerLane.FLASH_SECONDS).coerceAtLeast(0f)

        nextSpawn -= seconds
        if (nextSpawn <= 0f) {
            blocks += Obstacle(x = track + RunnerLane.BLOCK_HALF, face = chance.nextInt(RunnerLane.FACES))
            nextSpawn = RunnerLane.SPAWN_GAP + RunnerLane.SPAWN_JITTER * chance.nextFloat()
        }

        val behind = RunnerLane.HERO_X - (RunnerLane.HERO_HALF + RunnerLane.BLOCK_HALF)
        var cleared = 0
        for (index in blocks.indices) {
            val block = blocks[index]
            val moved = block.x - RunnerLane.SPEED * seconds
            if (block.x >= behind && moved < behind) cleared++
            blocks[index] = block.copy(x = moved)
        }
        avoided += cleared

        while (blocks.isNotEmpty() && blocks.first().x < -RunnerLane.BLOCK_HALF) {
            blocks.removeAt(0)
        }

        if (blocks.any { struck(it) }) stumble()
    }

    private fun struck(block: Obstacle): Boolean =
        abs(block.x - RunnerLane.HERO_X) < RunnerLane.HERO_HALF + RunnerLane.BLOCK_HALF &&
            heroY > -RunnerLane.BLOCK

    private fun stumble() {
        blocks.clear()
        heroY = GROUND
        velocity = 0f
        avoided = 0
        nextSpawn = RunnerLane.SPAWN_GAP
        stumbled = 1f
    }
}

private val RunnerSaver =
    Saver<RunnerGameState, Int>(
        save = { it.avoided },
        restore = { RunnerGameState(seed = RunnerLane.SPAWN_GAP, avoided = it) },
    )

@Composable
fun rememberRunnerGameState(): RunnerGameState = rememberSaveable(saver = RunnerSaver) { RunnerGameState(seed = RunnerLane.SPAWN_GAP) }

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
        rotate(degrees = RunnerLane.TILT, pivot = Offset.Zero)
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
        color = lerp(Color.White.copy(alpha = BOARD_FILL), Tokens.Alert.copy(alpha = FLASH_FILL), state.stumbled),
    ) {
        Canvas(
            modifier =
                Modifier
                    .fillMaxSize()
                    .onSizeChanged { size ->
                        if (size.height > 0) {
                            state.resize(size.width / (size.height * RunnerLane.GROUND_LINE))
                        }
                    }.semantics { contentDescription = board }
                    .pointerInput(state) { detectTapGestures { state.jump() } },
        ) {
            val lane = size.height * RunnerLane.GROUND_LINE

            drawLine(
                color = Color.White.copy(alpha = LINE_FILL),
                start = Offset(0f, lane),
                end = Offset(size.width, lane),
                strokeWidth = size.height * LINE_WEIGHT,
            )

            val side = lane * RunnerLane.BLOCK
            state.blocks.forEach { block ->
                withTransform({
                    translate(lane * block.x - side / 2f, lane - side)
                }) {
                    with(faces[block.face % faces.size]) {
                        draw(size = Size(side, side), colorFilter = faceTint)
                    }
                }
            }

            val length = lane * RunnerLane.PENCIL_LENGTH
            drawPencil(
                pencil = pencil,
                centre = Offset(lane * RunnerLane.HERO_X, lane * (1f + state.heroY) - lane * RunnerLane.SPAN / 2f),
                length = length,
                width = length * RunnerLane.SLIM,
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
    val edge = BorderStroke(HAIRLINE, Color.White.copy(alpha = EDGE_INK))

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
            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier.heightIn(min = TAP_TARGET),
                shape = MaterialTheme.shapes.medium,
                border = edge,
            ) {
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
            modifier = Modifier.fillMaxWidth().height(ACTION_HEIGHT),
            shape = MaterialTheme.shapes.medium,
            border = edge,
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
private val ACTION_HEIGHT = 52.dp
private val TAP_TARGET = 48.dp
private val HAIRLINE = 1.5.dp
private const val BOARD_FILL = 0.07f
private const val FLASH_FILL = 0.34f
private const val LINE_FILL = 0.22f
private const val LINE_WEIGHT = 0.012f
private const val LABEL_INK = 0.68f
private const val EDGE_INK = 0.22f
private const val HALF = 0.5f
private const val TIP_SHARE = 0.3f
private const val PEAK_AT = 0.55f
private const val SQUASH = 0.86f
private const val STRETCH = 1.14f

private fun previewGame(faces: List<Int>): RunnerGameState =
    RunnerGameState(seed = RunnerLane.SPAWN_GAP).also { game ->
        faces.forEachIndexed { index, face ->
            game.blocks += Obstacle(x = PREVIEW_FIRST + index * PREVIEW_GAP, face = face)
        }
    }

private const val PREVIEW_FIRST = 0.95f
private const val PREVIEW_GAP = 0.7f
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
