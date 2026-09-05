package com.example.brainxp.feature.capture

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.exifinterface.media.ExifInterface
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.ArrowRight
import com.composables.icons.lucide.Camera
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Trash2
import com.example.brainxp.R
import com.example.brainxp.core.capture.CameraSession
import com.example.brainxp.core.capture.CapturedPage
import com.example.brainxp.core.ui.BrainXPTheme
import com.example.brainxp.core.ui.EmptyState
import com.example.brainxp.core.ui.LightSystemBars
import com.example.brainxp.core.ui.PillTone
import com.example.brainxp.core.ui.PrimaryButton
import com.example.brainxp.core.ui.ScreenNav
import com.example.brainxp.core.ui.StatusPill
import com.example.brainxp.core.ui.Tokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun CameraCaptureScreen(
    state: CaptureUiState,
    onShutter: () -> Unit,
    onDelete: (String) -> Unit,
    onMove: (String, Int) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    bindCamera: (suspend (PreviewView) -> Unit)? = null,
) {
    val spacing = BrainXPTheme.spacing
    val context = LocalContext.current
    LightSystemBars()

    var granted by remember {
        mutableStateOf(
            context.checkSelfPermission(Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val request =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            granted = it
        }

    LaunchedEffect(granted) {
        if (!granted) {
            request.launch(Manifest.permission.CAMERA)
        }
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(Tokens.Blue900)
                .statusBarsPadding()
                .navigationBarsPadding(),
    ) {
        Box(modifier = Modifier.padding(horizontal = spacing.screenHorizontal)) {
            ScreenNav(title = stringResource(R.string.camera_title), onBack = onBack) {
                if (state.pages.isNotEmpty()) {
                    StatusPill(
                        text = stringResource(R.string.camera_page_count, state.pages.size),
                        tone = PillTone.ON_DARK,
                    )
                }
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (granted) {
                CameraPreview(bindCamera = bindCamera)
            } else {
                EmptyState(
                    title = stringResource(R.string.camera_denied_title),
                    body = stringResource(R.string.camera_denied_body),
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        Column(
            modifier = Modifier.padding(spacing.screenHorizontal),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            if (state.failed) {
                Text(
                    text = stringResource(R.string.camera_failed),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (state.pages.isEmpty()) {
                Text(
                    text = stringResource(R.string.camera_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = SECONDARY_INK),
                )
            } else {
                PageStrip(state = state, onDelete = onDelete, onMove = onMove)
            }

            PrimaryButton(
                text = stringResource(R.string.camera_shutter),
                onClick = onShutter,
                enabled = granted && !state.capturing,
                onDark = true,
            )

            if (state.canContinue) {
                PrimaryButton(
                    text = stringResource(R.string.camera_continue, state.pages.size),
                    onClick = onContinue,
                )
            }
            Spacer(modifier = Modifier.size(spacing.xs))
        }
    }
}

@Composable
private fun CameraPreview(
    bindCamera: (suspend (PreviewView) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val view = remember { PreviewView(context) }

    LaunchedEffect(view, bindCamera) {
        if (bindCamera != null) {
            bindCamera(view)
        } else {
            CameraSession(context).bind(owner, view)
        }
    }

    AndroidView(factory = { view }, modifier = modifier.fillMaxSize())
}

@Composable
private fun PageStrip(
    state: CaptureUiState,
    onDelete: (String) -> Unit,
    onMove: (String, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(BrainXPTheme.spacing.sm),
    ) {
        items(state.pages, key = { it.id }) { page ->
            PageThumbnail(
                page = page,
                ordinal = state.pages.indexOf(page) + 1,
                onDelete = { onDelete(page.id) },
                onMoveLeft = { onMove(page.id, -1) },
                onMoveRight = { onMove(page.id, 1) },
            )
        }
    }
}

@Composable
private fun PageThumbnail(
    page: CapturedPage,
    ordinal: Int,
    onDelete: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bitmap by
        produceState<Bitmap?>(initialValue = null, page.path) {
            value = withContext(Dispatchers.IO) { decodeThumbnail(page.path) }
        }

    Column(
        modifier = modifier.size(width = THUMB_WIDTH, height = THUMB_HEIGHT),
        verticalArrangement = Arrangement.spacedBy(BrainXPTheme.spacing.xs),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().weight(1f),
            shape = MaterialTheme.shapes.small,
            color = Color.White.copy(alpha = PLATE_FILL),
        ) {
            Box(contentAlignment = Alignment.TopStart) {
                bitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Text(
                    text = ordinal.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier =
                        Modifier
                            .padding(BrainXPTheme.spacing.xs)
                            .background(Tokens.Blue900.copy(alpha = BADGE_FILL), MaterialTheme.shapes.extraSmall)
                            .padding(horizontal = BADGE_PAD),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().height(ACTION_ROW),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StripAction(Lucide.ArrowLeft, R.string.camera_move_left, onMoveLeft)
            StripAction(Lucide.Trash2, R.string.camera_delete_page, onDelete)
            StripAction(Lucide.ArrowRight, R.string.camera_move_right, onMoveRight)
        }
    }
}

@Composable
private fun StripAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: Int,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, modifier = Modifier.size(ACTION_TAP)) {
        Icon(
            imageVector = icon,
            contentDescription = stringResource(description),
            tint = Color.White.copy(alpha = SECONDARY_INK),
            modifier = Modifier.size(ACTION_GLYPH),
        )
    }
}

private fun decodeThumbnail(path: String): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    if (bounds.outWidth <= 0) {
        return null
    }
    var sample = 1
    while (bounds.outWidth / sample > THUMB_PIXELS) {
        sample *= 2
    }
    val decoded =
        BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
    return decoded?.rotated(exifDegrees(path))
}

private fun exifDegrees(path: String): Float =
    when (
        runCatching { ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }
            .getOrDefault(ExifInterface.ORIENTATION_NORMAL)
    ) {
        ExifInterface.ORIENTATION_ROTATE_90 -> QUARTER_TURN
        ExifInterface.ORIENTATION_ROTATE_180 -> HALF_TURN
        ExifInterface.ORIENTATION_ROTATE_270 -> THREE_QUARTER_TURN
        else -> 0f
    }

private fun Bitmap.rotated(degrees: Float): Bitmap =
    if (degrees == 0f) {
        this
    } else {
        Bitmap.createBitmap(this, 0, 0, width, height, Matrix().apply { postRotate(degrees) }, true)
    }

private val THUMB_WIDTH = 96.dp
private val THUMB_HEIGHT = 140.dp
private val ACTION_ROW = 32.dp
private val ACTION_TAP = 48.dp
private val ACTION_GLYPH = 15.dp
private val BADGE_PAD = 5.dp
private const val THUMB_PIXELS = 220
private const val QUARTER_TURN = 90f
private const val HALF_TURN = 180f
private const val THREE_QUARTER_TURN = 270f
private const val SECONDARY_INK = 0.68f
private const val PLATE_FILL = 0.1f
private const val BADGE_FILL = 0.7f
