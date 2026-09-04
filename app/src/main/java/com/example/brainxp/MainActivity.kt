package com.example.brainxp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.brainxp.blocking.BlockingService
import com.example.brainxp.core.ui.BrainXPTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val blockedPackage = intent?.getStringExtra(BlockingService.EXTRA_BLOCKED_PACKAGE)
        setContent {
            BrainXPTheme {
                BrainXPApp(deepLink = blockedPackage?.let { MainRoute.Capture })
            }
        }
    }
}
