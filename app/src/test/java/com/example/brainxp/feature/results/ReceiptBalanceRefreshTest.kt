package com.example.brainxp.feature.results

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

private const val RECEIPT_VIEW_MODEL = "com/example/brainxp/feature/results/ReceiptViewModel.kt"

private fun sourceOf(relative: String): File {
    val roots = listOf("src/main/java", "app/src/main/java", "../app/src/main/java")
    return roots
        .map { File(it, relative) }
        .firstOrNull { it.exists() }
        ?: File(roots.first(), relative)
}

class ReceiptBalanceRefreshTest {
    private val source = sourceOf(RECEIPT_VIEW_MODEL).readText()

    @Test
    fun `the receipt holds the balance that home renders`() {
        assertTrue(
            "ReceiptViewModel cannot refresh the balance without RewardReconciler",
            source.contains("private val rewards: RewardReconciler,"),
        )
    }

    @Test
    fun `a settled session refreshes that balance`() {
        assertTrue(
            "credited seconds stay invisible on home until something reconciles them",
            source.contains("rewards.reconcile()"),
        )
    }

    @Test
    fun `the refresh sits on the branch that produced a receipt`() {
        val settled =
            source
                .substringAfter("if (result is AppResult.Success) {")
                .substringBefore("} else {")

        assertTrue(
            "reconcile must follow a receipt, not a failed submit",
            settled.contains("rewards.reconcile()"),
        )
    }
}
