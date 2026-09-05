package com.example.brainxp.data.repo

import com.example.brainxp.core.network.AuthTokenStore
import com.example.brainxp.core.network.AuthTokens
import com.example.brainxp.data.prefs.AuthDataStore
import com.example.brainxp.di.AppScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRestorer
    @Inject
    constructor(
        private val store: AuthDataStore,
        private val tokens: AuthTokenStore,
        @AppScope private val scope: CoroutineScope,
    ) {
        fun restore() {
            scope.launch {
                val saved = store.current()
                val access = saved.accessToken ?: return@launch
                tokens.update(AuthTokens(access, saved.refreshToken.orEmpty()))
            }
        }
    }
