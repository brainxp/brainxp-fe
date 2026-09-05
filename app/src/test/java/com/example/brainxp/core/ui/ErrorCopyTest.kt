package com.example.brainxp.core.ui

import com.example.brainxp.core.result.ApiError
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

private val FORBIDDEN_IN_COPY =
    listOf(
        "kode %1\$d",
        "code %1\$d",
        "error_code",
        "HTTP",
        "Exception",
        "null",
    )

private fun stringsXml(): String {
    val roots = listOf("src/main/res/values/strings.xml", "app/src/main/res/values/strings.xml")
    return roots.map(::File).firstOrNull { it.exists() }?.readText() ?: ""
}

private fun copyFor(key: String): String {
    val match = Regex("""<string name="$key">(.*?)</string>""", RegexOption.DOT_MATCHES_ALL).find(stringsXml())
    return match?.groupValues?.get(1).orEmpty()
}

class ErrorCopyTest {
    private val branches =
        listOf(
            "network" to ApiError.Network,
            "unauthorized" to ApiError.Unauthorized,
            "rate_limited" to ApiError.RateLimited(null),
            "timeout" to ApiError.Timeout,
            "server_busy" to ApiError.ServerBusy,
            "validation" to ApiError.Validation(null, null),
            "unknown" to ApiError.Unknown(null, null),
        )

    @Test
    fun `every ApiError branch has a title and a body`() {
        branches.forEach { (key, _) ->
            assertTrue("missing error_${key}_title", copyFor("error_${key}_title").isNotBlank())
            assertTrue("missing error_${key}_body", copyFor("error_${key}_body").isNotBlank())
        }
    }

    @Test
    fun `the branch list covers the whole sealed hierarchy`() {
        val known = branches.map { it.second::class.simpleName }.toSet()
        val declared =
            setOf("Network", "Unauthorized", "RateLimited", "Timeout", "ServerBusy", "Validation", "Unknown")

        assertTrue("uncovered ApiError branches: ${declared - known}", declared.all { it in known })
    }

    @Test
    fun `no error body leaks a raw code or exception name`() {
        branches.forEach { (key, _) ->
            val body = copyFor("error_${key}_body")
            FORBIDDEN_IN_COPY.forEach { forbidden ->
                assertFalse(
                    "error_${key}_body leaks \"$forbidden\": $body",
                    body.contains(forbidden, ignoreCase = true),
                )
            }
        }
    }

    @Test
    fun `every error body tells the user what to do next`() {
        val actionWords = listOf("coba", "periksa", "tunggu", "masuk", "ganti", "kirim", "tutup", "buka")

        branches.forEach { (key, _) ->
            val body = copyFor("error_${key}_body").lowercase()
            assertTrue(
                "error_${key}_body has no next step: $body",
                actionWords.any { body.contains(it) },
            )
        }
    }

    @Test
    fun `the generic unknown copy is not the fallback for everything`() {
        val unknown = copyFor("error_unknown_body")
        val others = branches.filter { it.first != "unknown" }.map { copyFor("error_${it.first}_body") }

        assertTrue(
            "another branch reuses the unknown copy verbatim",
            others.none { it == unknown },
        )
    }
}
