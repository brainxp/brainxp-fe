package com.example.brainxp.core.network

import com.example.brainxp.core.result.ApiError
import com.example.brainxp.core.result.AppResult
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ErrorMapper
    @Inject
    constructor(
        private val bodyReader: ApiErrorBodyReader,
    ) {
        fun map(throwable: Throwable): ApiError =
            when {
                throwable is HttpException -> mapHttp(throwable)
                throwable is SerializationException -> serializationFailure(throwable)
                throwable is IOException -> ApiError.Network
                throwable.hasCause<SerializationException>() -> serializationFailure(throwable)
                throwable.hasCause<IOException>() -> ApiError.Network
                else -> ApiError.Unknown(code = null, message = throwable.message)
            }

        private fun mapHttp(exception: HttpException): ApiError {
            val code = exception.code()
            val body = runCatching { exception.response()?.errorBody()?.string() }.getOrNull()
            return when {
                code == HTTP_UNAUTHORIZED -> {
                    ApiError.Unauthorized
                }

                code == HTTP_TOO_MANY_REQUESTS -> {
                    ApiError.RateLimited(retryAfter(exception))
                }

                code == HTTP_BAD_REQUEST || code == HTTP_UNPROCESSABLE -> {
                    ApiError.Validation(bodyReader.field(body), bodyReader.message(body))
                }

                code in SERVER_ERROR_RANGE -> {
                    ApiError.ServerBusy
                }

                else -> {
                    ApiError.Unknown(code, bodyReader.message(body))
                }
            }
        }

        private fun retryAfter(exception: HttpException): Long? =
            exception
                .response()
                ?.headers()
                ?.get(HEADER_RETRY_AFTER)
                ?.trim()
                ?.toLongOrNull()

        private fun serializationFailure(throwable: Throwable) =
            ApiError.Unknown(code = null, message = "malformed response: ${throwable.message}")

        private inline fun <reified T : Throwable> Throwable.hasCause(): Boolean {
            var current = cause
            var depth = 0
            while (current != null && depth < MAX_CAUSE_DEPTH) {
                if (current is T) return true
                current = current.cause
                depth++
            }
            return false
        }

        private companion object {
            const val HTTP_BAD_REQUEST = 400
            const val HTTP_UNAUTHORIZED = 401
            const val HTTP_UNPROCESSABLE = 422
            const val HTTP_TOO_MANY_REQUESTS = 429
            val SERVER_ERROR_RANGE = 500..599
            const val HEADER_RETRY_AFTER = "Retry-After"
            const val MAX_CAUSE_DEPTH = 5
        }
    }

@Suppress("TooGenericExceptionCaught")
suspend fun <T> ErrorMapper.apiCall(block: suspend () -> T): AppResult<T> =
    try {
        AppResult.Success(block())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (throwable: Throwable) {
        AppResult.Failure(map(throwable))
    }
