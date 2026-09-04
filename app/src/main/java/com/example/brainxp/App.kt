package com.example.brainxp

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.brainxp.core.permission.PermissionResumeObserver
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class App : Application() {
    @Inject
    lateinit var permissionResumeObserver: PermissionResumeObserver

    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(permissionResumeObserver)
    }
}
