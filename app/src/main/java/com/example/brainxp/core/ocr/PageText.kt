package com.example.brainxp.core.ocr

sealed interface PageText {
    val pageId: String

    data class Extracted(
        override val pageId: String,
        val text: String,
    ) : PageText

    data class Failed(
        override val pageId: String,
        val reason: String,
    ) : PageText
}

data class OcrBatch(
    val pages: List<PageText> = emptyList(),
) {
    val extracted: List<PageText.Extracted> get() = pages.filterIsInstance<PageText.Extracted>()

    val failed: List<PageText.Failed> get() = pages.filterIsInstance<PageText.Failed>()

    val anyExtracted: Boolean get() = extracted.isNotEmpty()

    val allFailed: Boolean get() = pages.isNotEmpty() && extracted.isEmpty()
}
