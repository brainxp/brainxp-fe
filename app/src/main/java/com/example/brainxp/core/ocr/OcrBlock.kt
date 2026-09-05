package com.example.brainxp.core.ocr

data class OcrBlock(
    val text: String,
    val top: Int,
    val left: Int,
)

fun assemblePageText(blocks: List<OcrBlock>): String =
    blocks
        .filter { it.text.isNotBlank() }
        .sortedWith(compareBy({ it.top }, { it.left }))
        .joinToString(BLOCK_SEPARATOR) { it.text.trim() }

private const val BLOCK_SEPARATOR = "\n"
