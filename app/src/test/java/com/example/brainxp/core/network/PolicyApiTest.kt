package com.example.brainxp.core.network

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class PolicyApiTest {
    private lateinit var server: MockWebServer
    private lateinit var api: PolicyApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val json = Json { ignoreUnknownKeys = true }
        api =
            Retrofit
                .Builder()
                .baseUrl(server.url("/"))
                .client(OkHttpClient())
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(PolicyApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `locking an app posts to the locked-apps path for that package`() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(204))

            api.lockApp("subject-1", "com.mobile.legends")

            val request = server.takeRequest()
            assertEquals("POST", request.method)
            assertEquals("/policies/subject-1/locked-apps/com.mobile.legends", request.path)
        }

    @Test
    fun `unlocking an app deletes the same path`() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(204))

            api.unlockApp("subject-1", "com.instagram.android")

            val request = server.takeRequest()
            assertEquals("DELETE", request.method)
            assertEquals("/policies/subject-1/locked-apps/com.instagram.android", request.path)
        }

    @Test
    fun `a package name is not split into extra path segments`() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(204))

            api.lockApp("subject-1", "com.a.b.c")

            assertEquals("/policies/subject-1/locked-apps/com.a.b.c", server.takeRequest().path)
        }
}
