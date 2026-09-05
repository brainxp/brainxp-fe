package com.example.brainxp.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.brainxp.R
import com.example.brainxp.domain.model.AcademicLevel

@Composable
fun levelLabel(level: AcademicLevel): String =
    stringResource(
        when (level) {
            AcademicLevel.SD -> R.string.level_sd
            AcademicLevel.SMP -> R.string.level_smp
            AcademicLevel.SMA -> R.string.level_sma
            AcademicLevel.KULIAH -> R.string.level_kuliah
            AcademicLevel.UMUM -> R.string.level_umum
        },
    )
