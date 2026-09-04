package com.example.brainxp.core.network

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor
    @Inject
    constructor(
        private val tokenStore: AuthTokenStore,
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val alreadyAuthenticated = request.header(HEADER_AUTHORIZATION) != null
            val accessToken = if (alreadyAuthenticated) null else tokenStore.current()?.accessToken
            return chain.proceed(accessToken?.let { request.withBearer(it) } ?: request)
        }
    }

internal const val HEADER_AUTHORIZATION = "Authorization"
internal const val BEARER_PREFIX = "Bearer "

internal fun okhttp3.Request.withBearer(accessToken: String) =
    newBuilder()
        .header(HEADER_AUTHORIZATION, BEARER_PREFIX + accessToken)
        .build()
