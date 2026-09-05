package com.example.brainxp.data.repo.fake

import com.example.brainxp.domain.model.Badge
import com.example.brainxp.domain.model.Material
import com.example.brainxp.domain.model.MaterialStatus
import com.example.brainxp.domain.model.MaterialType
import com.example.brainxp.domain.model.Progress
import com.example.brainxp.domain.model.Question
import com.example.brainxp.domain.model.RestrictedApp
import com.example.brainxp.domain.model.SessionMode

internal object FakeData {
    const val DAY_MILLIS = 86_400_000L

    val now: Long get() = System.currentTimeMillis()

    fun materials(): List<Material> =
        listOf(
            Material(
                id = "mat-1",
                title = "Bab 4 — Gerak Lurus",
                type = MaterialType.PHOTO,
                status = MaterialStatus.READY,
                createdAt = now - DAY_MILLIS,
                sessionCount = 3,
            ),
            Material(
                id = "mat-2",
                title = "Ringkasan Sel Hewan",
                type = MaterialType.IMAGE,
                status = MaterialStatus.READY,
                createdAt = now - 3 * DAY_MILLIS,
                sessionCount = 1,
            ),
            Material(
                id = "mat-3",
                title = "Slide Ekonomi — Inflasi",
                type = MaterialType.DOCUMENT,
                status = MaterialStatus.PROCESSING,
                createdAt = now - 30 * 60 * 1_000L,
                sessionCount = 0,
            ),
            Material(
                id = "mat-4",
                title = "Chapter 7 — Thermodynamics",
                type = MaterialType.DOCUMENT,
                status = MaterialStatus.READY,
                createdAt = now - 8 * DAY_MILLIS,
                sessionCount = 2,
            ),
        )

    fun questions(): List<Question> =
        listOf(
            Question.MultipleChoice(
                id = "q-1",
                conceptIds = listOf("kinematika"),
                stem = "Sebuah mobil bergerak 60 km/jam selama 2 jam. Berapa jarak tempuhnya?",
                options = listOf("30 km", "60 km", "120 km", "180 km"),
            ),
            Question.ShortAnswer(
                id = "q-3",
                conceptIds = listOf("percepatan"),
                stem = "Sebutkan satuan SI untuk percepatan.",
            ),
            Question.Unsupported(
                id = "q-4",
                conceptIds = listOf("kinematika"),
                rawType = "diagram_match",
            ),
        )

    fun restrictedApps(): List<RestrictedApp> =
        listOf(
            RestrictedApp("com.google.android.youtube", "YouTube", enabled = true),
            RestrictedApp("com.instagram.android", "Instagram", enabled = true),
            RestrictedApp("com.mobile.legends", "Mobile Legends", enabled = true),
            RestrictedApp("com.supercell.clashofclans", "Clash of Clans", enabled = false),
        )

    fun progress(streakCurrent: Int): Progress =
        Progress(
            streakCurrent = streakCurrent,
            streakLongest = LONGEST_STREAK,
            sessions = TOTAL_SESSIONS,
            correctTotal = TOTAL_CORRECT,
            essayPassed = TOTAL_ESSAYS,
            freezeTokens = SAMPLE_FREEZE_TOKENS,
            badges = badges(),
        )

    private fun badges(): List<Badge> =
        listOf(
            Badge("first-step", "Langkah pertama", "Selesaikan satu sesi", earned = true),
            Badge("week-streak", "Tujuh hari", "Belajar tujuh hari beruntun", earned = false),
            Badge("essayist", "Penulis", "Lolos ambang tiga esai", earned = true),
            Badge("hard-mode", "Soal sulit", "Benar sepuluh soal sulit", earned = false),
            Badge("night-owl", "Rajin malam", "Sesi setelah jam sembilan", earned = true),
            Badge("comeback", "Balik lagi", "Kembali setelah libur", earned = false),
        )

    private const val LONGEST_STREAK = 9
    private const val TOTAL_SESSIONS = 14
    private const val TOTAL_CORRECT = 96
    private const val TOTAL_ESSAYS = 3
    private const val SAMPLE_FREEZE_TOKENS = 1
}
