package com.example.brainxp.feature

import com.example.brainxp.feature.questions.QuizQuestion
import com.example.brainxp.feature.questions.QuizUiState
import com.example.brainxp.feature.results.ReceiptRow
import com.example.brainxp.feature.results.ReceiptUiState

internal const val SAMPLE_QUESTION_COUNT = 6
internal const val SAMPLE_ESTIMATE_SECONDS = 1_140
internal const val SAMPLE_READY_QUESTIONS = 3
internal const val SAMPLE_MATERIAL_ID = "material-1"
internal const val SAMPLE_MATERIAL_NAME = "Bab 4 — Gerak Lurus.pdf"
internal const val SAMPLE_ASSESSED_LEVEL = "SD kelas 4"
internal const val SAMPLE_DECLARED_LEVEL = "SMA kelas 11"
internal const val SAMPLE_REJECT_REASON =
    "Isinya ringkasan satu paragraf tanpa istilah yang bisa diuji."

internal val SAMPLE_QUIZ =
    QuizUiState(
        title = "Bab 4 — Gerak Lurus",
        questions =
            listOf(
                QuizQuestion(
                    id = "q1",
                    stem = "Sebuah mobil bergerak 20 m/s lalu berhenti dalam 4 detik. Berapa perlambatannya?",
                    difficulty = "sedang",
                    factor = 1.2,
                    options = listOf("2 m/s²", "4 m/s²", "5 m/s²", "80 m/s²"),
                ),
                QuizQuestion(
                    id = "q2",
                    stem = "Benda jatuh bebas dari 45 m. Berapa lama sampai tanah? (g = 10 m/s²)",
                    difficulty = "mudah",
                    factor = 1.0,
                    options = listOf("1 detik", "2 detik", "3 detik", "4,5 detik"),
                ),
                QuizQuestion(
                    id = "q3",
                    stem = "Jelaskan bedanya kecepatan dan percepatan dengan contohmu sendiri.",
                    difficulty = "sulit",
                    factor = 2.88,
                    sourceExcerpt = "Percepatan adalah laju perubahan kecepatan terhadap waktu.",
                    rubricCriteria = 3,
                ),
            ),
    )

internal val SAMPLE_RECEIPT =
    ReceiptUiState(
        title = "Bab 4 — Gerak Lurus",
        correctCount = 4,
        questionCount = 6,
        baseRewardSeconds = 120,
        rows =
            listOf(
                ReceiptRow(1, "Soal 1", "mudah", 1.0, 120),
                ReceiptRow(2, "Soal 2", "sedang", 1.2, 144),
                ReceiptRow(
                    ordinal = 3,
                    label = "Soal 3",
                    difficulty = "sedang",
                    multiplier = 1.2,
                    rewardSeconds = 0,
                    voided = true,
                    voidReason = "salah",
                    explanation =
                        "Perlambatan dihitung dari selisih kecepatan dibagi waktu, bukan dikalikan.",
                ),
                ReceiptRow(4, "Soal 4", "sulit", 1.6, 192),
                ReceiptRow(
                    ordinal = 5,
                    label = "Soal 5",
                    difficulty = "sulit",
                    multiplier = 1.6,
                    rewardSeconds = 0,
                    voided = true,
                    voidReason = "di bawah ambang",
                    explanation = "Jawabannya belum menyebut satuan dan arah percepatannya.",
                ),
                ReceiptRow(6, "Esai", "sulit", 2.88, 346),
            ),
        subtotalSeconds = 802,
        levelFactor = 1.0,
        levelNote = "setara jenjangmu",
        noveltyFactor = 0.6,
        noveltyNote = "sudah pernah dipelajari",
        creditedSeconds = 481,
        balanceSeconds = 1_981,
        streakCurrent = 4,
        newBadges = listOf("Penulis"),
    )
