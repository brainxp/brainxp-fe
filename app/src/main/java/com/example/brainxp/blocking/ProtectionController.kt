package com.example.brainxp.blocking

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.brainxp.data.repo.RestrictionRepository
import com.example.brainxp.di.AppScope
import com.example.brainxp.domain.protectionHeld
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProtectionController
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val restrictions: RestrictionRepository,
        @AppScope private val scope: CoroutineScope,
    ) {
        fun start() {
            scope.launch {
                restrictions
                    .observeRestricted()
                    .map { apps -> protectionHeld(apps.count { it.enabled }) }
                    .distinctUntilChanged()
                    .collect { enabled -> if (enabled) startService() else stopService() }
            }
        }

        private fun startService() {
            ContextCompat.startForegroundService(context, intent())
        }

        private fun stopService() {
            context.stopService(intent())
        }

        private fun intent() = Intent(context, BlockingService::class.java)
    }
