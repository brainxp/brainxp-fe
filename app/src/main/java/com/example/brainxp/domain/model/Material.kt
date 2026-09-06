package com.example.brainxp.domain.model

enum class MaterialType {
    PHOTO,
    IMAGE,
    DOCUMENT,
    TEXT,
}

enum class MaterialStatus {
    UPLOADING,
    PROCESSING,
    READY,
    FAILED,
}

data class UnfinishedSession(
    val sessionId: String,
    val answered: Int,
    val total: Int,
)

data class Material(
    val id: String,
    val title: String,
    val type: MaterialType,
    val status: MaterialStatus,
    val createdAt: Long,
    val sessionCount: Int,
    val questionCount: Int = 0,
    val assessedLevel: String? = null,
    val declaredLevel: String? = null,
    val gateReason: String? = null,
    val topicSummary: String? = null,
    val unfinished: UnfinishedSession? = null,
)

data class MaterialPage(
    val items: List<Material>,
    val nextCursor: String?,
)
