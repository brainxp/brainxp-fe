package com.example.brainxp.core.network

import com.example.brainxp.core.result.ApiError
import com.example.brainxp.core.result.AppResult
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import java.util.concurrent.TimeUnit

@Serializable
data class Payload(
    val id: String,
    val count: Int,
)

interface TestApi {
    @GET("/thing")
    suspend fun thing(): Payload
}

class ErrorMapperTest {
    private lateinit var server: MockWebServer
    private lateinit var api: TestApi
    private lateinit var mapper: ErrorMapper

    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        mapper = ErrorMapper(ApiErrorBodyReader(json))
        api =
            Retrofit
                .Builder()
                .baseUrl(server.url("/"))
                .client(
                    OkHttpClient
                        .Builder()
                        .callTimeout(2, TimeUnit.SECONDS)
                        .retryOnConnectionFailure(false)
                        .build(),
                ).addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(TestApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private suspend fun call(): AppResult<Payload> = mapper.apiCall { api.thing() }

    private fun failure(result: AppResult<Payload>): ApiError {
        assertTrue("expected a failure, got $result", result is AppResult.Failure)
        return (result as AppResult.Failure).error
    }

    @Test
    fun successReturnsTheParsedBody() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody("""{"id":"abc","count":3}"""),
            )

            val result = call()

            assertEquals(AppResult.Success(Payload("abc", 3)), result)
        }

    @Test
    fun connectionFailureMapsToNetwork() =
        runTest {
            server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

            assertEquals(ApiError.Network, failure(call()))
        }

    @Test
    fun serverDownMapsToNetwork() =
        runTest {
            server.shutdown()

            assertEquals(ApiError.Network, failure(call()))
        }

    @Test
    fun aConflictKeepsTheServerSentence() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setResponseCode(409)
                    .setBody(
                        """{"detail":{"code":"incomplete_session","message":"Masih ada 8 soal yang belum dijawab."}}""",
                    ),
            )

            val error = failure(call())

            assertTrue("expected Unknown but got $error", error is ApiError.Unknown)
            assertEquals(
                "Masih ada 8 soal yang belum dijawab.",
                (error as ApiError.Unknown).message,
            )
            assertEquals(409, error.code)
        }

    @Test
    fun unauthorizedMapsToUnauthorized() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(401))

            val error = failure(call())

            assertEquals(ApiError.Unauthorized, error)
            assertFalse(error.retryable)
        }

    @Test
    fun tooManyRequestsMapsToRateLimitedWithRetryAfter() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setResponseCode(429)
                    .setHeader("Retry-After", "42"),
            )

            val error = failure(call())

            assertEquals(ApiError.RateLimited(42L), error)
            assertTrue(error.retryable)
        }

    @Test
    fun rateLimitedWithoutHeaderHasNullRetryAfter() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(429))

            assertEquals(ApiError.RateLimited(null), failure(call()))
        }

    @Test
    fun rateLimitedWithHttpDateRetryAfterDoesNotCrash() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setResponseCode(429)
                    .setHeader("Retry-After", "Wed, 21 Oct 2026 07:28:00 GMT"),
            )

            assertEquals(ApiError.RateLimited(null), failure(call()))
        }

    @Test
    fun serviceUnavailableMapsToServerBusy() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(503))

            val error = failure(call())

            assertEquals(ApiError.ServerBusy, error)
            assertTrue(error.retryable)
        }

    @Test
    fun internalServerErrorMapsToServerBusy() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(500))

            assertEquals(ApiError.ServerBusy, failure(call()))
        }

    @Test
    fun unprocessableEntityMapsToValidationWithFlatField() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setResponseCode(422)
                    .setBody("""{"field":"minutes","message":"must be positive"}"""),
            )

            assertEquals(
                ApiError.Validation(field = "minutes", message = "must be positive"),
                failure(call()),
            )
        }

    @Test
    fun validationReadsFieldFromNestedDetailArray() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setResponseCode(422)
                    .setBody(
                        """{"detail":[{"loc":["body","questionCount"],"msg":"field required"}]}""",
                    ),
            )

            assertEquals(
                ApiError.Validation(field = "questionCount", message = "field required"),
                failure(call()),
            )
        }

    @Test
    fun badRequestMapsToValidationEvenWithUnparsableBody() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setResponseCode(400)
                    .setBody("plain text explanation"),
            )

            val error = failure(call())

            assertTrue(error is ApiError.Validation)
            assertNull((error as ApiError.Validation).field)
            assertEquals("plain text explanation", error.message)
        }

    @Test
    fun forbiddenMapsToUnknownRatherThanUnauthorized() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(403))

            assertEquals(ApiError.Unknown(403, null), failure(call()))
        }

    @Test
    fun teapotMapsToUnknownWithCode() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(418))

            val error = failure(call())

            assertEquals(ApiError.Unknown(418, null), error)
            assertTrue(error.retryable)
        }

    @Test
    fun malformedJsonMapsToUnknownNotACrash() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody("""{"id":"abc","count":""" + """"not a number"}"""),
            )

            val error = failure(call())

            assertTrue("expected Unknown, got $error", error is ApiError.Unknown)
            assertTrue((error as ApiError.Unknown).message.orEmpty().contains("malformed response"))
        }

    @Test
    fun missingRequiredFieldMapsToUnknown() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody("""{"id":"abc"}"""),
            )

            assertTrue(failure(call()) is ApiError.Unknown)
        }

    @Test
    fun truncatedBodyMapsToNetworkOrUnknownButNeverThrows() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody("""{"id":"abc","count":3}""")
                    .setSocketPolicy(SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY),
            )

            val error = failure(call())

            assertTrue(
                "expected Network or Unknown, got $error",
                error is ApiError.Network || error is ApiError.Unknown,
            )
        }
}
