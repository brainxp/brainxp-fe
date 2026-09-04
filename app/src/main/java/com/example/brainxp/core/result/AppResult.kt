package com.example.brainxp.core.result

sealed interface AppResult<out T> {
    data class Success<out T>(
        val value: T,
    ) : AppResult<T>

    data class Failure(
        val error: ApiError,
    ) : AppResult<Nothing>
}

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> =
    when (this) {
        is AppResult.Success -> AppResult.Success(transform(value))
        is AppResult.Failure -> this
    }

fun <T> AppResult<T>.valueOrNull(): T? =
    when (this) {
        is AppResult.Success -> value
        is AppResult.Failure -> null
    }

fun <T> AppResult<T>.errorOrNull(): ApiError? =
    when (this) {
        is AppResult.Success -> null
        is AppResult.Failure -> error
    }
