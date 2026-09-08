package com.example.brainxp.core.capture

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class CameraSession(
    private val context: Context,
) {
    private var imageCapture: ImageCapture? = null

    suspend fun bind(
        owner: LifecycleOwner,
        preview: PreviewView,
    ) {
        val provider = awaitProvider()
        val previewUseCase =
            Preview.Builder().build().also { it.surfaceProvider = preview.surfaceProvider }
        val capture =
            ImageCapture
                .Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

        provider.unbindAll()
        provider.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, previewUseCase, capture)
        imageCapture = capture
    }

    suspend fun takePicture(target: File): Result<File> {
        val capture = imageCapture ?: return Result.failure(IllegalStateException("not bound"))
        val options = ImageCapture.OutputFileOptions.Builder(target).build()

        return suspendCancellableCoroutine { continuation ->
            capture.takePicture(
                options,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        continuation.resume(Result.success(target))
                    }

                    override fun onError(exception: ImageCaptureException) {
                        continuation.resume(Result.failure(exception))
                    }
                },
            )
        }
    }

    private suspend fun awaitProvider(): ProcessCameraProvider =
        suspendCoroutine { continuation ->
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener(
                {
                    runCatching { future.get() }
                        .onSuccess { continuation.resume(it) }
                        .onFailure { continuation.resumeWithException(it) }
                },
                ContextCompat.getMainExecutor(context),
            )
        }
}
