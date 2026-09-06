package com.example.brainxp.feature.questions

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.brainxp.core.ui.BrainXPTheme
import com.example.brainxp.core.ui.PreviewColumn

@Composable
internal fun SwipeHint(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val supplied = remember { runCatching { context.assets.open(SWIPE_ASSET).close() }.isSuccess }

    if (!supplied) {
        DrawnSwipeHint(modifier)
        return
    }

    val composition by rememberLottieComposition(LottieCompositionSpec.Asset(SWIPE_ASSET))
    val film = composition

    if (film == null) {
        DrawnSwipeHint(modifier)
        return
    }

    LottieAnimation(
        composition = film,
        iterations = LottieConstants.IterateForever,
        modifier = modifier.fillMaxWidth().height(FILM_HEIGHT),
    )
}

private const val SWIPE_ASSET = "swipe_hint.json"
private val FILM_HEIGHT = 120.dp

@Preview(showBackground = true)
@Composable
private fun SwipeHintPreview() {
    BrainXPTheme {
        PreviewColumn { SwipeHint() }
    }
}
