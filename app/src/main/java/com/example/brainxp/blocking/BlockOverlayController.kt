package com.example.brainxp.blocking

import android.content.Context
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlockOverlayController
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val main = Handler(Looper.getMainLooper())
        private val windowManager = context.getSystemService(WindowManager::class.java)

        private var container: OverlayContainer? = null
        private var owner: OverlayViewOwner? = null
        private var configSignature: String? = null
        private var target: String? = null
        private var earnTime: ((String) -> Unit)? = null

        val isShowing: Boolean get() = container != null

        val showingFor: String? get() = target

        fun show(
            blockedPackage: String,
            onEarnTime: (String) -> Unit,
        ) {
            onMain {
                when {
                    container == null -> {
                        attach(blockedPackage, onEarnTime)
                    }

                    target != blockedPackage -> {
                        detach()
                        attach(blockedPackage, onEarnTime)
                    }

                    else -> {
                        Unit
                    }
                }
            }
        }

        fun hide() {
            onMain { detach() }
        }

        private fun attach(
            blockedPackage: String,
            onEarnTime: (String) -> Unit,
        ) {
            val viewOwner = OverlayViewOwner().apply { attach() }
            val host = OverlayContainer(context, ::onConfigurationChanged)

            val composeView =
                ComposeView(context).apply {
                    setContent {
                        BlockScreen(
                            blockedPackage = blockedPackage,
                            onEarnTime = { onEarnTime(blockedPackage) },
                        )
                    }
                }

            host.setViewTreeLifecycleOwner(viewOwner)
            host.setViewTreeViewModelStoreOwner(viewOwner)
            host.setViewTreeSavedStateRegistryOwner(viewOwner)
            host.addView(composeView)

            windowManager.addView(host, layoutParams())

            container = host
            owner = viewOwner
            target = blockedPackage
            earnTime = onEarnTime
            configSignature = signatureOf(context.resources.configuration)
        }

        private fun detach() {
            val host = container ?: return
            container = null
            configSignature = null
            target = null
            earnTime = null
            windowManager.removeView(host)
            owner?.detach()
            owner = null
        }

        private fun onConfigurationChanged(newConfig: Configuration) {
            val signature = signatureOf(newConfig)
            val blockedPackage = target
            val callback = earnTime
            if (signature == configSignature || blockedPackage == null || callback == null) {
                return
            }
            configSignature = signature
            detach()
            attach(blockedPackage, callback)
        }

        private fun layoutParams(): WindowManager.LayoutParams =
            WindowManager
                .LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                    PixelFormat.TRANSLUCENT,
                ).apply { title = WINDOW_TITLE }

        private fun signatureOf(configuration: Configuration): String =
            listOf(
                configuration.orientation,
                configuration.densityDpi,
                configuration.uiMode,
                configuration.screenWidthDp,
                configuration.screenHeightDp,
            ).joinToString(":")

        private fun onMain(block: () -> Unit) {
            if (Looper.myLooper() == Looper.getMainLooper()) block() else main.post(block)
        }

        companion object {
            const val WINDOW_TITLE = "BrainXPBlockOverlay"
        }
    }

private class OverlayContainer(
    context: Context,
    private val onConfiguration: (Configuration) -> Unit,
) : FrameLayout(context) {
    init {
        isFocusableInTouchMode = true
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        newConfig?.let(onConfiguration)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean =
        if (event.keyCode == KeyEvent.KEYCODE_BACK) true else super.dispatchKeyEvent(event)
}
