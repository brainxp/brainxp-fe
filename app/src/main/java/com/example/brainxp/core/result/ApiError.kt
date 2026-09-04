package com.example.brainxp.core.result

sealed interface ApiError {
    val retryable: Boolean

    data object Network : ApiError {
        override val retryable = true
    }

    data object Unauthorized : ApiError {
        override val retryable = false
    }

    data class RateLimited(
        val retryAfterSeconds: Long?,
    ) : ApiError {
        override val retryable = true
    }

    data object ServerBusy : ApiError {
        override val retryable = true
    }

    data class Validation(
        val field: String?,
        val message: String?,
    ) : ApiError {
        override val retryable = false
    }

    data class Unknown(
        val code: Int?,
        val message: String?,
    ) : ApiError {
        override val retryable = true
    }
}
