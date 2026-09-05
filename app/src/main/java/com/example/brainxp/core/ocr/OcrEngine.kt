package com.example.brainxp.core.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

interface OcrEngine {
    suspend fun extract(
        pageId: String,
        path: String,
    ): PageText

    suspend fun extractBatch(pages: List<Pair<String, String>>): OcrBatch
}

@Singleton
class MlKitOcrEngine
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : OcrEngine {
        private val recognizer by lazy {
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        }

        override suspend fun extract(
            pageId: String,
            path: String,
        ): PageText {
            val file = File(path)
            if (!file.exists()) {
                return PageText.Failed(pageId, MISSING_FILE)
            }
            return runCatching { InputImage.fromFilePath(context, Uri.fromFile(file)) }
                .fold(
                    onSuccess = { recognise(pageId, it) },
                    onFailure = { PageText.Failed(pageId, it.reason()) },
                )
        }

        override suspend fun extractBatch(pages: List<Pair<String, String>>): OcrBatch =
            OcrBatch(pages.map { (pageId, path) -> extract(pageId, path) })

        private suspend fun recognise(
            pageId: String,
            image: InputImage,
        ): PageText =
            suspendCancellableCoroutine { continuation ->
                recognizer
                    .process(image)
                    .addOnSuccessListener { result ->
                        val blocks =
                            result.textBlocks.mapNotNull { block ->
                                val box = block.boundingBox ?: return@mapNotNull null
                                OcrBlock(block.text, box.top, box.left)
                            }
                        continuation.resume(PageText.Extracted(pageId, assemblePageText(blocks)))
                    }.addOnFailureListener { error ->
                        continuation.resume(PageText.Failed(pageId, error.reason()))
                    }
            }

        private companion object {
            const val MISSING_FILE = "berkasnya tidak ditemukan"
            const val UNKNOWN = "penyebabnya belum diketahui"
        }

        private fun Throwable.reason(): String = message?.takeIf { it.isNotBlank() } ?: UNKNOWN
    }
