package com.example.brainxp.core.network

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

private class RecordingRefresher(
    private val issue: (Int) -> AuthTokens?,
) : TokenRefresher {
    val calls = AtomicInteger(0)
    var beforeRefresh: (() -> Unit)? = null

    override fun refresh(refreshToken: String): AuthTokens? {
        beforeRefresh?.invoke()
        return issue(calls.incrementAndGet())
    }
}

class TokenAuthenticatorTest {
    private lateinit var server: MockWebServer
    private lateinit var store: AuthTokenStore

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        store = InMemoryAuthTokenStore()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun client(refresher: TokenRefresher): OkHttpClient =
        OkHttpClient
            .Builder()
            .addInterceptor(AuthInterceptor(store))
            .authenticator(TokenAuthenticator(store, refresher))
            .retryOnConnectionFailure(false)
            .callTimeout(5, TimeUnit.SECONDS)
            .build()

    private fun get(client: OkHttpClient) =
        client
            .newCall(Request.Builder().url(server.url("/thing")).build())
            .execute()

    @Test
    fun interceptorAttachesBearerTokenWhenPresent() {
        store.update(AuthTokens("access-1", "refresh-1"))
        server.enqueue(MockResponse().setResponseCode(200))

        get(client(RecordingRefresher { null })).close()

        assertEquals("Bearer access-1", server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun interceptorSendsNoHeaderWhenNoToken() {
        server.enqueue(MockResponse().setResponseCode(200))

        get(client(RecordingRefresher { null })).close()

        assertNull(server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun refreshesOnceThenRetriesWithNewToken() {
        store.update(AuthTokens("stale", "refresh-1"))
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(200))

        val refresher = RecordingRefresher { AuthTokens("fresh", "refresh-2") }
        val response = get(client(refresher))

        assertEquals(200, response.code)
        response.close()
        assertEquals(1, refresher.calls.get())
        assertEquals("Bearer stale", server.takeRequest().getHeader("Authorization"))
        assertEquals("Bearer fresh", server.takeRequest().getHeader("Authorization"))
        assertEquals(AuthTokens("fresh", "refresh-2"), store.current())
    }

    @Test
    fun failedRefreshClearsTokensAndSurfaces401() {
        store.update(AuthTokens("stale", "refresh-1"))
        server.enqueue(MockResponse().setResponseCode(401))

        val refresher = RecordingRefresher { null }
        val response = get(client(refresher))

        assertEquals(401, response.code)
        response.close()
        assertEquals(1, refresher.calls.get())
        assertNull(store.current())
    }

    @Test
    fun doesNotLoopWhenRefreshedTokenIsAlsoRejected() {
        store.update(AuthTokens("stale", "refresh-1"))
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(401))

        val refresher = RecordingRefresher { AuthTokens("fresh-$it", "refresh-next") }
        val response = get(client(refresher))

        assertEquals(401, response.code)
        response.close()
        assertEquals(1, refresher.calls.get())
        assertEquals(2, server.requestCount)
    }

    @Test
    fun givesUpWhenStoreIsEmpty() {
        server.enqueue(MockResponse().setResponseCode(401))

        val refresher = RecordingRefresher { AuthTokens("fresh", "refresh-2") }
        val response = get(client(refresher))

        assertEquals(401, response.code)
        response.close()
        assertEquals(0, refresher.calls.get())
    }

    @Test
    fun concurrentUnauthorizedResponsesRefreshOnlyOnce() {
        store.update(AuthTokens("stale", "refresh-1"))
        server.dispatcher =
            object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    val accepted = request.getHeader("Authorization") == "Bearer fresh"
                    return MockResponse().setResponseCode(if (accepted) 200 else 401)
                }
            }

        val gate = CountDownLatch(1)
        val refresher = RecordingRefresher { AuthTokens("fresh", "refresh-2") }
        refresher.beforeRefresh = { gate.await(1, TimeUnit.SECONDS) }

        val http = client(refresher)
        val ready = CountDownLatch(CONCURRENCY)
        val codes = java.util.Collections.synchronizedList(mutableListOf<Int>())

        val threads =
            (1..CONCURRENCY).map {
                Thread {
                    ready.countDown()
                    get(http).use { codes.add(it.code) }
                }.apply { start() }
            }

        ready.await(2, TimeUnit.SECONDS)
        gate.countDown()
        threads.forEach { it.join(TimeUnit.SECONDS.toMillis(10)) }

        assertEquals(CONCURRENCY, codes.size)
        assertEquals(listOf(200), codes.distinct())
        assertEquals(1, refresher.calls.get())
        assertEquals(AuthTokens("fresh", "refresh-2"), store.current())
    }

    private companion object {
        const val CONCURRENCY = 4
    }
}
