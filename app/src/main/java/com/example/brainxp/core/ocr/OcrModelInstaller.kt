package com.example.brainxp.core.ocr

import android.content.Context
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

enum class OcrModelState {
    UNKNOWN,
    PREPARING,
    READY,
    UNAVAILABLE,
}

@Singleton
class OcrModelInstaller
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val mutableState = MutableStateFlow(OcrModelState.UNKNOWN)
        val state: StateFlow<OcrModelState> = mutableState.asStateFlow()

        suspend fun ensureAvailable(): OcrModelState {
            if (mutableState.value == OcrModelState.READY) {
                return OcrModelState.READY
            }
            mutableState.value = OcrModelState.PREPARING

            val recogniser = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val client = ModuleInstall.getClient(context)

            val alreadyThere =
                awaitBoolean { onDone ->
                    client
                        .areModulesAvailable(recogniser)
                        .addOnSuccessListener { onDone(it.areModulesAvailable()) }
                        .addOnFailureListener { onDone(false) }
                }

            val outcome =
                if (alreadyThere) {
                    true
                } else {
                    awaitBoolean { onDone ->
                        client
                            .installModules(ModuleInstallRequest.newBuilder().addApi(recogniser).build())
                            .addOnSuccessListener { onDone(true) }
                            .addOnFailureListener { onDone(false) }
                    }
                }

            mutableState.value =
                if (outcome) OcrModelState.READY else OcrModelState.UNAVAILABLE
            return mutableState.value
        }

        private suspend fun awaitBoolean(block: ((Boolean) -> Unit) -> Unit): Boolean =
            suspendCancellableCoroutine { continuation ->
                block { result -> continuation.resume(result) }
            }
    }
