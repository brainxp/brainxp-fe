package com.example.brainxp.feature.health

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.brainxp.R
import com.example.brainxp.blocking.ProtectionStatus
import com.example.brainxp.core.ui.RowGroup

@Composable
fun ProtectionRow(modifier: Modifier = Modifier) {
    val viewModel: ProtectionViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    RowGroup(modifier = modifier) {
        item(
            title = stringResource(R.string.settings_protection),
            subtitle =
                stringResource(
                    when (state.status) {
                        ProtectionStatus.ACTIVE -> R.string.settings_protection_on
                        ProtectionStatus.DEGRADED -> R.string.settings_protection_degraded
                        ProtectionStatus.OFF -> R.string.settings_protection_off
                    },
                ),
        )
    }
}
