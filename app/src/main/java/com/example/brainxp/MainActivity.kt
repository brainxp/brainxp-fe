package com.example.brainxp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavKey
import com.example.brainxp.blocking.BlockingService
import com.example.brainxp.core.ui.BrainXPTheme
import com.example.brainxp.core.upload.PreparationNotifier
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var deepLink by mutableStateOf<NavKey?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        deepLink = linkOf(intent)
        setContent {
            BrainXPTheme {
                BrainXPApp(deepLink = deepLink)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        linkOf(intent)?.let { deepLink = it }
    }

    private fun linkOf(intent: Intent?): NavKey? {
        val material = intent?.getStringExtra(PreparationNotifier.EXTRA_READY_MATERIAL)
        if (material != null) return MainRoute.Questions(material)
        return intent?.getStringExtra(BlockingService.EXTRA_BLOCKED_PACKAGE)?.let { MainRoute.Capture }
    }
}
