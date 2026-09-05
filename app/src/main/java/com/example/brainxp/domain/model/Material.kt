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

data class Material(
    val id: String,
    val title: String,
    val type: MaterialType,
    val status: MaterialStatus,
    val charCount: Int,
    val createdAt: Long,
    val sessionCount: Int,
    val questionCount: Int = 0,
)

data class MaterialPage(
    val items: List<Material>,
    val nextCursor: String?,
)

data class MaterialDetail(
    val material: Material,
    val sessions: List<SessionSummary>,
)

data class SessionSummary(
    val sessionId: String,
    val mode: SessionMode,
    val createdAt: Long,
    val score: Double?,
    val rewardSeconds: Int?,
)
