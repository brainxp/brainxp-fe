package com.example.brainxp.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

const val GUIDE_NO_ACCOUNT = "anon"

fun quizGuideKeyOf(accountId: String?): Preferences.Key<Boolean> =
    booleanPreferencesKey("guide.quiz.${accountId?.takeIf(String::isNotBlank) ?: GUIDE_NO_ACCOUNT}")

fun interface GuideAccount {
    suspend fun accountId(): String?
}

@Singleton
class AuthGuideAccount
    @Inject
    constructor(
        private val auth: AuthDataStore,
    ) : GuideAccount {
        override suspend fun accountId(): String? = auth.auth.first().subjectId
    }

@Singleton
class GuideStore
    @Inject
    constructor(
        @Named("deviceStore") private val store: DataStore<Preferences>,
        private val accounts: GuideAccount,
    ) {
        suspend fun quizGuideSeen(): Boolean = store.data.first()[keyForCurrent()] == true

        suspend fun rememberQuizGuide() {
            val key = keyForCurrent()
            store.edit { it[key] = true }
        }

        private suspend fun keyForCurrent(): Preferences.Key<Boolean> = quizGuideKeyOf(accounts.accountId())
    }
