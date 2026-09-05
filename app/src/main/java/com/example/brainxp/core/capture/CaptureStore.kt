package com.example.brainxp.core.capture

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CaptureStore
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val root: File
            get() = File(context.cacheDir, DIRECTORY).apply { mkdirs() }

        fun newPageFile(): File = File(root, "${UUID.randomUUID()}.jpg")

        fun delete(path: String) {
            File(path).takeIf { it.exists() && it.parentFile == root }?.delete()
        }

        fun clear() {
            root.listFiles()?.forEach { it.delete() }
        }

        private companion object {
            const val DIRECTORY = "captures"
        }
    }
