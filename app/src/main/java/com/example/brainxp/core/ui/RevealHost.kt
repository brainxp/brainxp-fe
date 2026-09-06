package com.example.brainxp.core.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import com.svenjacobs.reveal.RevealCanvas
import com.svenjacobs.reveal.RevealCanvasState
import com.svenjacobs.reveal.rememberRevealCanvasState

val LocalRevealCanvas =
    compositionLocalOf<RevealCanvasState> {
        error("No RevealCanvas in the tree. Wrap the app in RevealHost.")
    }

@Composable
fun RevealHost(content: @Composable () -> Unit) {
    val canvas = rememberRevealCanvasState()

    RevealCanvas(revealCanvasState = canvas, modifier = Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalRevealCanvas provides canvas) { content() }
    }
}
