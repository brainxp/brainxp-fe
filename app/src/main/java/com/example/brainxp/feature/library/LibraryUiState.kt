package com.example.brainxp.feature.library

import com.example.brainxp.core.result.ApiError
import com.example.brainxp.domain.model.Material

data class LibraryUiState(
    val phase: Phase = Phase.Loading,
    val items: List<Material> = emptyList(),
    val removing: String? = null,
) {
    sealed interface Phase {
        data object Loading : Phase

        data object Ready : Phase

        data class Error(
            val error: ApiError,
            val retryable: Boolean,
        ) : Phase
    }

    val empty: Boolean get() = phase == Phase.Ready && items.isEmpty()
}

sealed interface LibraryEvent {
    data object Retry : LibraryEvent

    data class Study(
        val materialId: String,
    ) : LibraryEvent

    data class Remove(
        val materialId: String,
    ) : LibraryEvent
}

sealed interface LibraryEffect {
    data class OpenMaterial(
        val materialId: String,
    ) : LibraryEffect

    data class RemoveFailed(
        val error: ApiError,
    ) : LibraryEffect
}

fun noveltyMultiplier(timesStudied: Int): Double =
    when {
        timesStudied <= 0 -> FULL
        timesStudied == 1 -> SECOND
        else -> LATER
    }

private const val FULL = 1.0
private const val SECOND = 0.6
private const val LATER = 0.3
