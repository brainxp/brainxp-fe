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
    private var openedNotification by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        deepLink = linkOf(intent)
        openedNotification = intent.notificationId()
        setContent {
            BrainXPTheme {
                BrainXPApp(deepLink = deepLink, openedNotification = openedNotification)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        linkOf(intent)?.let { deepLink = it }
        intent.notificationId()?.let { openedNotification = it }
    }

    private fun Intent?.notificationId(): String? = this?.getStringExtra(PreparationNotifier.EXTRA_NOTIFICATION_ID)

    private fun linkOf(intent: Intent?): NavKey? =
        notificationRoute(
            rejectedMaterial = intent?.getStringExtra(PreparationNotifier.EXTRA_REJECTED_MATERIAL),
            readyMaterial = intent?.getStringExtra(PreparationNotifier.EXTRA_READY_MATERIAL),
            blockedPackage = intent?.getStringExtra(BlockingService.EXTRA_BLOCKED_PACKAGE),
        )
}
