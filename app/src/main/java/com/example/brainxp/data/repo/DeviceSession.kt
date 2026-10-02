package com.example.brainxp.data.repo

import com.example.brainxp.core.network.AuthTokenStore
import com.example.brainxp.core.network.AuthTokens
import com.example.brainxp.core.network.TokenDto
import com.example.brainxp.data.prefs.AuthDataStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceSession
    @Inject
    constructor(
        private val store: AuthDataStore,
        private val tokens: AuthTokenStore,
    ) {
        suspend fun adopt(token: TokenDto) {
            store.saveTokens(token.accessToken, token.refreshToken)
            store.saveIdentity(token.subjectId, token.familyId, token.role)
            tokens.update(AuthTokens(token.accessToken, token.refreshToken))
        }

        suspend fun saveSubject(subjectId: String) = store.saveSubject(subjectId)
    }
