package com.example.brainxp.data.repo.fake

import com.example.brainxp.domain.model.ActivityEvent
import com.example.brainxp.domain.model.ActivityKind
import com.example.brainxp.domain.model.Badge
import com.example.brainxp.domain.model.ChildConfig
import com.example.brainxp.domain.model.FamilyChild
import com.example.brainxp.domain.model.GuardianStatus
import com.example.brainxp.domain.model.Material
import com.example.brainxp.domain.model.MaterialStatus
import com.example.brainxp.domain.model.MaterialType
import com.example.brainxp.domain.model.Progress
import com.example.brainxp.domain.model.Question
import com.example.brainxp.domain.model.RestrictedApp
import com.example.brainxp.domain.model.SessionMode

internal object FakeData {
    const val DAY_MILLIS = 86_400_000L
    private const val MINUTE_MILLIS = 60_000L
    private const val HOUR_MILLIS = 3_600_000L

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

    fun activityLog(): List<ActivityEvent> =
        listOf(
            ActivityEvent(ActivityKind.UNLOCK_ENDED, now - HOUR_MILLIS),
            ActivityEvent(
                ActivityKind.UNLOCK_STARTED,
                now - UNLOCK_STARTED_AGO,
                mapOf("minutes" to "15"),
            ),
            ActivityEvent(
                ActivityKind.REWARD_EARNED,
                now - REWARD_EARNED_AGO,
                mapOf("minutes" to "15"),
            ),
            ActivityEvent(
                ActivityKind.SESSION_COMPLETED,
                now - SESSION_DONE_AGO,
                mapOf("score" to "0.8"),
            ),
            ActivityEvent(ActivityKind.MATERIAL_ADDED, now - DAY_MILLIS),
            ActivityEvent(
                ActivityKind.PROTECTION_DEGRADED,
                now - 2 * DAY_MILLIS,
                mapOf("permission" to "usage_access"),
            ),
        )

    fun children(): List<FamilyChild> =
        listOf(
            FamilyChild("child-1", "Rani", GuardianStatus.OK, RANI_MINUTES, RANI_SESSIONS, RANI_ACCURACY),
            FamilyChild("child-2", "Bima", GuardianStatus.DEGRADED, 0, BIMA_SESSIONS, BIMA_ACCURACY),
            FamilyChild("child-3", "Sita", GuardianStatus.UNKNOWN, SITA_MINUTES, 0, null),
        )

    fun defaultChildConfig() =
        ChildConfig(
            restrictedPackages = listOf("com.google.android.youtube", "com.mobile.legends"),
            dailyCapMinutes = DEFAULT_DAILY_CAP_MINUTES,
            rewardPerSessionMinutes = DEFAULT_REWARD_MINUTES,
        )

    private const val UNLOCK_STARTED_AGO = 90 * MINUTE_MILLIS
    private const val REWARD_EARNED_AGO = 95 * MINUTE_MILLIS

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
    private const val SESSION_DONE_AGO = 96 * MINUTE_MILLIS
    private const val RANI_MINUTES = 25
    private const val RANI_SESSIONS = 6
    private const val RANI_ACCURACY = 0.78
    private const val BIMA_SESSIONS = 2
    private const val BIMA_ACCURACY = 0.51
    private const val SITA_MINUTES = 40
    private const val DEFAULT_DAILY_CAP_MINUTES = 90
    private const val DEFAULT_REWARD_MINUTES = 15
}
