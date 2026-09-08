package com.example.brainxp.core.network

import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenAuthenticator
    @Inject
    constructor(
        private val tokenStore: AuthTokenStore,
        private val refresher: TokenRefresher,
    ) : Authenticator {
        private val lock = Any()

        @Suppress("ReturnCount")
        override fun authenticate(
            route: Route?,
            response: Response,
        ): Request? {
            if (attemptCount(response) >= MAX_ATTEMPTS) return null

            val failedToken =
                response.request
                    .header(HEADER_AUTHORIZATION)
                    ?.removePrefix(BEARER_PREFIX)

            synchronized(lock) {
                val current = tokenStore.current() ?: return null

                if (failedToken != null && current.accessToken != failedToken) {
                    return response.request.withBearer(current.accessToken)
                }

                return when (val outcome = refresher.refresh(current.refreshToken)) {
                    is RefreshOutcome.Renewed -> {
                        tokenStore.update(outcome.tokens)
                        response.request.withBearer(outcome.tokens.accessToken)
                    }

                    RefreshOutcome.Rejected -> {
                        tokenStore.forget()
                        null
                    }

                    RefreshOutcome.Unreachable -> {
                        null
                    }
                }
            }
        }

        private fun attemptCount(response: Response): Int {
            var count = 1
            var prior = response.priorResponse
            while (prior != null) {
                count++
                prior = prior.priorResponse
            }
            return count
        }

        private companion object {
            const val MAX_ATTEMPTS = 2
        }
    }
