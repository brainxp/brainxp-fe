package com.example.brainxp.core.network

import com.example.brainxp.data.prefs.AuthDataStore
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Provider
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
class PersistentAuthTokenStore
    @Inject
    constructor(
        private val store: AuthDataStore,
    ) : AuthTokenStore {
        private val cached = AtomicReference<AuthTokens?>(null)

        override fun current(): AuthTokens? =
            cached.get() ?: runBlocking {
                val saved = store.current()
                saved.accessToken?.let { access ->
                    AuthTokens(access, saved.refreshToken.orEmpty()).also { cached.set(it) }
                }
            }

        override fun update(tokens: AuthTokens?) = cached.set(tokens)
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
class NetworkTokenRefresher
    @Inject
    constructor(
        private val api: Provider<AuthApi>,
        private val store: AuthDataStore,
    ) : TokenRefresher {
        override fun refresh(refreshToken: String): AuthTokens? =
            runBlocking {
                runCatching { api.get().refresh(RefreshRequestDto(refreshToken)) }
                    .getOrNull()
                    ?.let { token ->
                        store.saveTokens(token.accessToken, token.refreshToken)
                        store.saveIdentity(token.subjectId, token.familyId, token.role)
                        AuthTokens(token.accessToken, token.refreshToken)
                    }
            }
    }

@Singleton
class UnavailableTokenRefresher
    @Inject
    constructor() : TokenRefresher {
        override fun refresh(refreshToken: String): AuthTokens? = null
    }
