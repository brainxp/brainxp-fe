package com.example.brainxp.core.network

import com.example.brainxp.data.prefs.AuthDataStore
import com.example.brainxp.data.repo.SessionTeardown
import com.example.brainxp.di.RefreshNetworkModule
import kotlinx.coroutines.runBlocking
import retrofit2.HttpException
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton

data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
)

sealed interface RefreshOutcome {
    data class Renewed(
        val tokens: AuthTokens,
    ) : RefreshOutcome

    data object Rejected : RefreshOutcome

    data object Unreachable : RefreshOutcome
}

interface AuthTokenStore {
    fun current(): AuthTokens?

    fun update(tokens: AuthTokens?)

    fun forget()
}

interface TokenRefresher {
    fun refresh(refreshToken: String): RefreshOutcome
}

@Singleton
class PersistentAuthTokenStore
    @Inject
    constructor(
        private val store: AuthDataStore,
        private val teardown: SessionTeardown,
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

        override fun forget() {
            cached.set(null)
            runBlocking { teardown.run() }
        }
    }

@Singleton
class InMemoryAuthTokenStore
    @Inject
    constructor() : AuthTokenStore {
        private val ref = AtomicReference<AuthTokens?>(null)

        override fun current(): AuthTokens? = ref.get()

        override fun update(tokens: AuthTokens?) = ref.set(tokens)

        override fun forget() = ref.set(null)
    }

@Singleton
class NetworkTokenRefresher
    @Inject
    constructor(
        @Named(RefreshNetworkModule.REFRESH) private val api: Provider<AuthApi>,
        private val store: AuthDataStore,
    ) : TokenRefresher {
        override fun refresh(refreshToken: String): RefreshOutcome =
            runBlocking {
                runCatching {
                    val token = api.get().refresh(RefreshRequestDto(refreshToken))
                    store.saveTokens(token.accessToken, token.refreshToken)
                    store.saveIdentity(token.subjectId, token.familyId, token.role)
                    AuthTokens(token.accessToken, token.refreshToken)
                }.fold(
                    onSuccess = { renewed -> RefreshOutcome.Renewed(renewed) },
                    onFailure = { failure -> outcomeOf(failure) },
                )
            }

        private fun outcomeOf(failure: Throwable): RefreshOutcome =
            if (failure is HttpException && failure.code() in REJECTING_CODES) {
                RefreshOutcome.Rejected
            } else {
                RefreshOutcome.Unreachable
            }

        private companion object {
            val REJECTING_CODES = setOf(401, 403)
        }
    }

@Singleton
class UnavailableTokenRefresher
    @Inject
    constructor() : TokenRefresher {
        override fun refresh(refreshToken: String): RefreshOutcome = RefreshOutcome.Unreachable
    }
