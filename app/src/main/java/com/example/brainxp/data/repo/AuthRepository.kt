package com.example.brainxp.data.repo

import com.example.brainxp.core.network.AuthApi
import com.example.brainxp.core.network.AuthTokenStore
import com.example.brainxp.core.network.AuthTokens
import com.example.brainxp.core.network.ErrorMapper
import com.example.brainxp.core.network.LoginRequestDto
import com.example.brainxp.core.network.RefreshRequestDto
import com.example.brainxp.core.network.RegisterRequestDto
import com.example.brainxp.core.network.TokenDto
import com.example.brainxp.core.result.AppResult
import com.example.brainxp.data.prefs.AuthDataStore
import javax.inject.Inject
import javax.inject.Singleton

data class Identity(
    val role: String,
    val subjectId: String?,
)

@Singleton
class AuthRepository
    @Inject
    constructor(
        private val api: AuthApi,
        private val store: AuthDataStore,
        private val tokens: AuthTokenStore,
        private val errors: ErrorMapper,
    ) {
        suspend fun register(
            email: String,
            password: String,
            displayName: String,
            personal: Boolean,
        ): AppResult<Identity> =
            call {
                api.register(
                    RegisterRequestDto(
                        email = email,
                        password = password,
                        displayName = displayName,
                        mode = if (personal) MODE_PERSONAL else MODE_FAMILY,
                    ),
                )
            }

        suspend fun login(
            email: String,
            password: String,
        ): AppResult<Identity> = call { api.login(LoginRequestDto(email, password)) }

        suspend fun signOut() {
            store.current().refreshToken?.let { refresh ->
                runCatching { api.logout(RefreshRequestDto(refresh)) }
            }
            tokens.update(null)
            store.clear()
        }

        private suspend fun call(block: suspend () -> TokenDto): AppResult<Identity> =
            runCatching { block() }
                .fold(
                    onSuccess = { token ->
                        store.saveTokens(token.accessToken, token.refreshToken)
                        store.saveIdentity(token.subjectId, token.familyId, token.role)
                        tokens.update(AuthTokens(token.accessToken, token.refreshToken))
                        AppResult.Success(Identity(token.role, token.subjectId))
                    },
                    onFailure = { AppResult.Failure(errors.map(it)) },
                )

        private companion object {
            const val MODE_PERSONAL = "personal"
            const val MODE_FAMILY = "family"
        }
    }
