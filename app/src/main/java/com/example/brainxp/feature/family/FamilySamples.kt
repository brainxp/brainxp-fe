package com.example.brainxp.feature.family

import com.example.brainxp.domain.model.AcademicLevel
import com.example.brainxp.domain.model.UploadMethod

internal val SAMPLE_FAMILY =
    FamilyHomeUiState(
        children =
            listOf(
                FamilyMember(
                    id = "child-1",
                    name = "Rani",
                    level = AcademicLevel.SMP,
                    balanceSeconds = 1_500,
                    streakDays = 4,
                    remainingCapSeconds = 3_600,
                ),
                FamilyMember(
                    id = "child-2",
                    name = "Bima",
                    level = AcademicLevel.SD,
                    balanceSeconds = 0,
                    streakDays = 0,
                    remainingCapSeconds = 5_400,
                ),
            ),
        self =
            FamilyMember(
                id = "self",
                name = "Sita",
                level = AcademicLevel.SMA,
                balanceSeconds = 2_400,
                streakDays = 9,
                remainingCapSeconds = 1_800,
            ),
    )

internal val SAMPLE_POLICY =
    PolicyUiState(
        subjectName = "Rani",
        questionsPerSession = 6,
        essayCount = 2,
        dailyCapMinutes = listOf(60, 60, 60, 60, 90, 120, 120),
        dailyGrantMinutes = listOf(0, 0, 0, 0, 0, 30, 30),
        idleDaysAllowed = 2,
        dayResetHour = 4,
        baseRewardSeconds = 60,
        uploadMethods = UploadMethod.entries.toSet(),
        apps =
            listOf(
                LockedAppEntry("com.mobile.legends", "Mobile Legends", locked = true),
                LockedAppEntry("com.instagram.android", "Instagram", locked = true),
                LockedAppEntry("com.zhiliaoapp.musically", "TikTok", locked = false),
                LockedAppEntry("com.google.android.youtube", "YouTube", locked = true),
            ),
    )

internal val SAMPLE_PAIRING_CODE =
    PairingCodeUiState(
        subjectName = "Rani",
        code = "418702",
        secondsLeft = 552,
    )

internal val SAMPLE_REPORT =
    ChildReportUiState(
        subjectName = "Rani",
        alert = null,
        earnedPerDay = listOf(0, 420, 780, 300, 0, 960, 540),
        balanceSeconds = 1_500,
        materialsStudied = 7,
        correctTotal = 52,
        essayPassed = 3,
        recent =
            listOf(
                LedgerRow("Sesi belajar", "Bab 4 — Gerak Lurus", 481),
                LedgerRow("Waktu bermain", "Mobile Legends", -900),
                LedgerRow("Ditambah orang tua", "Ditukar hadiah: sepeda", 1_800),
                LedgerRow("Sesi belajar", "Ringkasan Sel Hewan", 322),
            ),
    )
