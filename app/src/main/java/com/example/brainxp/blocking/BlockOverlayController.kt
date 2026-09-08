package com.example.brainxp.blocking

import android.content.Context
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.annotation.RequiresApi
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
        private var action: ((String, BlockAction) -> Unit)? = null
        private var content: BlockContent? = null

        val isShowing: Boolean get() = container != null

        val showingFor: String? get() = target

        fun show(
            blockedPackage: String,
            appLabel: String = blockedPackage,
            info: BlockedInfo = BlockedInfo(),
            onAction: (String, BlockAction) -> Unit,
        ) {
            onMain {
                content = BlockContent(blockedPackage, appLabel, info)
                when {
                    container == null -> {
                        attach(blockedPackage, onAction)
                    }

                    target != blockedPackage -> {
                        detach()
                        attach(blockedPackage, onAction)
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
            onAction: (String, BlockAction) -> Unit,
        ) {
            val viewOwner = OverlayViewOwner().apply { attach() }
            val host = OverlayContainer(context, ::onConfigurationChanged)

            val composeView =
                ComposeView(context).apply {
                    setContent {
                        val shown =
                            content ?: BlockContent(blockedPackage, blockedPackage, BlockedInfo())
                        BlockScreen(
                            appLabel = shown.appLabel,
                            info = shown.info,
                            onAction = { kind -> onAction(blockedPackage, kind) },
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
            action = onAction
            configSignature = signatureOf(context.resources.configuration)
        }

        private fun detach() {
            val host = container ?: return
            container = null
            configSignature = null
            target = null
            action = null
            windowManager.removeView(host)
            owner?.detach()
            owner = null
        }

        private fun onConfigurationChanged(newConfig: Configuration) {
            val signature = signatureOf(newConfig)
            val blockedPackage = target
            val callback = action
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
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT,
                ).apply {
                    title = WINDOW_TITLE
                    layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                }

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
    private var backGuard: BackGuard? = null

    init {
        isFocusableInTouchMode = true
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            backGuard = BackGuard(this).also { it.register() }
        }
    }

    override fun onDetachedFromWindow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            backGuard?.unregister()
        }
        backGuard = null
        super.onDetachedFromWindow()
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        newConfig?.let(onConfiguration)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean =
        if (event.keyCode == KeyEvent.KEYCODE_BACK) true else super.dispatchKeyEvent(event)
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class BackGuard(
    private val view: View,
) {
    private val callback = OnBackInvokedCallback {}

    fun register() {
        view.findOnBackInvokedDispatcher()?.registerOnBackInvokedCallback(
            OnBackInvokedDispatcher.PRIORITY_OVERLAY,
            callback,
        )
    }

    fun unregister() {
        view.findOnBackInvokedDispatcher()?.unregisterOnBackInvokedCallback(callback)
    }
}

private data class BlockContent(
    val packageName: String,
    val appLabel: String,
    val info: BlockedInfo,
)
