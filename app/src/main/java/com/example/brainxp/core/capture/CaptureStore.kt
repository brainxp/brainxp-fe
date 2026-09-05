package com.example.brainxp.core.capture

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class CapturedPage(
    val id: String,
    val path: String,
)

@Singleton
class CaptureStore
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val mutablePages = MutableStateFlow<List<CapturedPage>>(emptyList())
        val pages: StateFlow<List<CapturedPage>> = mutablePages.asStateFlow()

        private val root: File
            get() = File(context.cacheDir, DIRECTORY).apply { mkdirs() }

        fun newPageFile(): File = File(root, "${UUID.randomUUID()}.jpg")

        fun add(file: File) {
            mutablePages.value = mutablePages.value + CapturedPage(file.name, file.absolutePath)
        }

        fun remove(id: String) {
            val page = mutablePages.value.firstOrNull { it.id == id } ?: return
            deleteFile(page.path)
            mutablePages.value = mutablePages.value.filterNot { it.id == id }
        }

        fun reorder(pages: List<CapturedPage>) {
            mutablePages.value = pages
        }

        fun deleteFile(path: String) {
            File(path).takeIf { it.exists() && it.parentFile == root }?.delete()
        }

        fun clear() {
            root.listFiles()?.forEach { it.delete() }
            mutablePages.value = emptyList()
        }

        private companion object {
            const val DIRECTORY = "captures"
        }
    }
