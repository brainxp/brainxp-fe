package com.example.brainxp.core.permission

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionResumeObserver
    @Inject
    constructor(
        private val provider: PermissionStateProvider,
    ) : DefaultLifecycleObserver {
        override fun onResume(owner: LifecycleOwner) {
            provider.refresh()
        }
    }
