package com.example.brainxp.core.network

import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
)

interface AuthTokenStore {
    fun current(): AuthTokens?

    fun update(tokens: AuthTokens?)
}

interface TokenRefresher {
    fun refresh(refreshToken: String): AuthTokens?
}

@Singleton
class InMemoryAuthTokenStore
    @Inject
    constructor() : AuthTokenStore {
        private val ref = AtomicReference<AuthTokens?>(null)

        override fun current(): AuthTokens? = ref.get()

        override fun update(tokens: AuthTokens?) = ref.set(tokens)
    }

@Singleton
class UnavailableTokenRefresher
    @Inject
    constructor() : TokenRefresher {
        override fun refresh(refreshToken: String): AuthTokens? = null
    }
