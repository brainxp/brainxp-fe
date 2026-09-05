package com.example.brainxp.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class GuideStore
    @Inject
    constructor(
        @Named("deviceStore") private val store: DataStore<Preferences>,
    ) {
        suspend fun quizGuideSeen(): Boolean = store.data.first()[QUIZ_GUIDE] == true

        suspend fun rememberQuizGuide() {
            store.edit { it[QUIZ_GUIDE] = true }
        }

        private companion object {
            val QUIZ_GUIDE = booleanPreferencesKey("guide.quiz")
        }
    }
