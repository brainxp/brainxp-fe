package com.example.brainxp.data.repo

import com.example.brainxp.data.db.OcrDraftDao
import com.example.brainxp.data.db.OcrDraftEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class DraftPage(
    val pageIndex: Int,
    val text: String,
)

interface OcrDraftRepository {
    suspend fun load(draftId: String): List<DraftPage>

    fun observe(draftId: String): Flow<List<DraftPage>>

    suspend fun savePage(
        draftId: String,
        page: DraftPage,
        now: Long,
    )

    suspend fun charCount(draftId: String): Int

    suspend fun discard(draftId: String)
}

@Singleton
class RoomOcrDraftRepository
    @Inject
    constructor(
        private val dao: OcrDraftDao,
    ) : OcrDraftRepository {
        override suspend fun load(draftId: String): List<DraftPage> = dao.findForDraft(draftId).map { DraftPage(it.pageIndex, it.text) }

        override fun observe(draftId: String): Flow<List<DraftPage>> =
            dao.observeForDraft(draftId).map { rows ->
                rows.map { DraftPage(it.pageIndex, it.text) }
            }

        override suspend fun savePage(
            draftId: String,
            page: DraftPage,
            now: Long,
        ) {
            dao.upsert(
                OcrDraftEntity(
                    draftId = draftId,
                    pageIndex = page.pageIndex,
                    text = page.text,
                    updatedAt = now,
                ),
            )
        }

        override suspend fun charCount(draftId: String): Int = dao.charCountForDraft(draftId) ?: 0

        override suspend fun discard(draftId: String) {
            dao.deleteDraft(draftId)
        }
    }
