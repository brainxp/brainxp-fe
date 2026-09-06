package com.example.brainxp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LocaleParityTest {
    private val untranslated: Set<String>
        get() =
            UNTRANSLATABLE_PATTERN
                .findAll(resource("values").readText())
                .map { it.groupValues[1] }
                .toSet()

    private fun resource(locale: String): File {
        val candidates =
            listOf(
                File("src/main/res/$locale/strings.xml"),
                File("app/src/main/res/$locale/strings.xml"),
            )
        return candidates.firstOrNull { it.isFile }
            ?: error("strings.xml for $locale not found from ${File(".").absolutePath}")
    }

    private fun keys(locale: String): Set<String> =
        NAME_PATTERN
            .findAll(resource(locale).readText())
            .map { it.groupValues[1] }
            .toSet()

    @Test
    fun englishCoversEveryTranslatableString() {
        val missing = keys("values") - keys("values-en") - untranslated

        assertEquals(emptySet<String>(), missing)
    }

    @Test
    fun englishDefinesNothingTheDefaultLocaleLacks() {
        val orphaned = keys("values-en") - keys("values")

        assertEquals(emptySet<String>(), orphaned)
    }

    @Test
    fun theBrandNameIsNotTranslated() {
        assertTrue(untranslated.none { it in keys("values-en") })
    }

    @Test
    fun everyFormatArgumentSurvivesTranslation() {
        val base = arguments("values")
        val english = arguments("values-en")

        val diverged =
            base.keys
                .intersect(english.keys)
                .filter { base[it] != english[it] }

        assertEquals(emptyList<String>(), diverged)
    }

    private fun arguments(locale: String): Map<String, Set<String>> =
        ENTRY_PATTERN
            .findAll(resource(locale).readText())
            .associate { match ->
                match.groupValues[1] to
                    ARGUMENT_PATTERN
                        .findAll(match.groupValues[2])
                        .map { it.value }
                        .toSet()
            }

    private companion object {
        val UNTRANSLATABLE_PATTERN = Regex("""<string name="([^"]+)"[^>]*translatable="false"""")
        val NAME_PATTERN = Regex("""<(?:string|plurals) name="([^"]+)"""")
        val ENTRY_PATTERN = Regex("""<string name="([^"]+)"[^>]*>(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
        val ARGUMENT_PATTERN = Regex("""%\d+\$[a-z]""")
    }
}
