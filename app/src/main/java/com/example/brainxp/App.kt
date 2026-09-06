package com.example.brainxp

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import com.example.brainxp.blocking.ProtectionController
import com.example.brainxp.core.permission.PermissionResumeObserver
import com.example.brainxp.core.upload.PreparationNotifier
import com.example.brainxp.data.repo.SessionRestorer
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class App :
    Application(),
    Configuration.Provider {
    @Inject
    lateinit var permissionResumeObserver: PermissionResumeObserver

    @Inject
    lateinit var protectionController: ProtectionController

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var sessionRestorer: SessionRestorer

    @Inject
    lateinit var preparationNotifier: PreparationNotifier

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        sessionRestorer.restore()
        ProcessLifecycleOwner.get().lifecycle.addObserver(permissionResumeObserver)
        protectionController.start()
        preparationNotifier.start()
    }
}
