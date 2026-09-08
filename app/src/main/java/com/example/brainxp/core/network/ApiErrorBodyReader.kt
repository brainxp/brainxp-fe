package com.example.brainxp.core.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiErrorBodyReader
    @Inject
    constructor(
        private val json: Json,
    ) {
        fun field(body: String?): String? {
            val root = body?.let { objectOrNull(it) } ?: return null
            val detail = firstDetail(root)
            return stringOf(root[KEY_FIELD])
                ?: detail?.let { stringOf(it[KEY_FIELD]) }
                ?: detail?.let { lastLocationSegment(it) }
        }

        fun message(body: String?): String? {
            if (body.isNullOrBlank()) return null
            val root = objectOrNull(body) ?: return body.take(MAX_MESSAGE_LENGTH)
            return stringOf(root[KEY_MESSAGE])
                ?: stringOf(root[KEY_DETAIL])
                ?: detailObject(root)?.let { stringOf(it[KEY_MESSAGE]) }
                ?: firstDetail(root)?.let { stringOf(it[KEY_MSG]) }
        }

        private fun objectOrNull(raw: String): JsonObject? =
            runCatching {
                json.parseToJsonElement(raw).jsonObject
            }.getOrNull()

        private fun detailObject(root: JsonObject): JsonObject? = runCatching { root[KEY_DETAIL]?.jsonObject }.getOrNull()

        private fun firstDetail(root: JsonObject): JsonObject? =
            runCatching { root[KEY_DETAIL]?.jsonArray?.firstOrNull()?.jsonObject }.getOrNull()

        private fun lastLocationSegment(detail: JsonObject): String? =
            runCatching {
                detail[KEY_LOCATION]?.jsonArray?.mapNotNull { stringOf(it) }?.lastOrNull()
            }.getOrNull()

        private fun stringOf(element: JsonElement?): String? =
            (element as? JsonPrimitive)
                ?.takeIf { it.isString }
                ?.content
                ?.takeIf { it.isNotBlank() }

        private companion object {
            const val KEY_FIELD = "field"
            const val KEY_MESSAGE = "message"
            const val KEY_DETAIL = "detail"
            const val KEY_LOCATION = "loc"
            const val KEY_MSG = "msg"
            const val MAX_MESSAGE_LENGTH = 200
        }
    }
