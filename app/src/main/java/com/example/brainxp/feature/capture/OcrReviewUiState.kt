package com.example.brainxp.feature.capture

const val MIN_USABLE_CHARS = 200
const val MAX_USABLE_CHARS = 20_000

enum class LengthVerdict {
    TOO_SHORT,
    USABLE,
    TOO_LONG,
}

data class ReviewPage(
    val pageIndex: Int,
    val text: String,
    val failedReason: String? = null,
)

data class OcrReviewUiState(
    val draftId: String = "",
    val pages: List<ReviewPage> = emptyList(),
    val index: Int = 0,
    val reading: Boolean = false,
    val saving: Boolean = false,
    val savedAtLeastOnce: Boolean = false,
) {
    val current: ReviewPage? get() = pages.getOrNull(index)

    val total: Int get() = pages.size

    val totalChars: Int get() = pages.sumOf { it.text.length }

    val verdict: LengthVerdict
        get() =
            when {
                totalChars < MIN_USABLE_CHARS -> LengthVerdict.TOO_SHORT
                totalChars > MAX_USABLE_CHARS -> LengthVerdict.TOO_LONG
                else -> LengthVerdict.USABLE
            }

    val failedPages: List<ReviewPage> get() = pages.filter { it.failedReason != null }

    val canUpload: Boolean get() = !reading && verdict == LengthVerdict.USABLE
}

fun OcrReviewUiState.withText(
    pageIndex: Int,
    text: String,
): OcrReviewUiState =
    copy(
        pages =
            pages.map { page ->
                if (page.pageIndex == pageIndex) page.copy(text = text) else page
            },
    )
