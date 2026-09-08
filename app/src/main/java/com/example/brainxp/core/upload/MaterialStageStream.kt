package com.example.brainxp.core.upload

import com.example.brainxp.core.network.MaterialApi
import com.example.brainxp.core.network.StageEventDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

enum class PreparingStage {
    READING,
    VALIDATING,
    PARTIAL,
    READY,
    REJECTED,
}

data class StageUpdate(
    val stage: PreparingStage,
    val ready: Int,
    val total: Int,
) {
    val settled: Boolean get() = stage == PreparingStage.READY || stage == PreparingStage.REJECTED
}

@Singleton
class MaterialStageStream
    @Inject
    constructor(
        private val api: MaterialApi,
        private val json: Json,
    ) {
        fun follow(materialId: String): Flow<StageUpdate> =
            flow {
                api.stream(materialId).use { body ->
                    val reader = body.source()
                    var settled = false
                    while (!settled && !reader.exhausted()) {
                        val update = parse(reader.readUtf8LineStrict())
                        if (update != null) {
                            emit(update)
                            settled = update.settled
                        }
                    }
                }
            }.flowOn(Dispatchers.IO)

        private fun parse(line: String): StageUpdate? {
            val payload = line.removePrefix(DATA_PREFIX).takeIf { it != line }
            val event = payload?.let { runCatching { json.decodeFromString<StageEventDto>(it) }.getOrNull() }
            return event?.let { sent ->
                stageOf(sent.stage)?.let { stage ->
                    StageUpdate(stage = stage, ready = sent.ready ?: 0, total = sent.total ?: 0)
                }
            }
        }

        private fun stageOf(raw: String): PreparingStage? =
            when (raw) {
                "reading" -> PreparingStage.READING
                "validating" -> PreparingStage.VALIDATING
                "partial" -> PreparingStage.PARTIAL
                "ready" -> PreparingStage.READY
                "rejected", "failed" -> PreparingStage.REJECTED
                else -> null
            }

        private companion object {
            const val DATA_PREFIX = "data: "
        }
    }
