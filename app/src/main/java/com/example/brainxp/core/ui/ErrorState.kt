package com.example.brainxp.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.brainxp.R
import com.example.brainxp.core.result.ApiError

@Composable
fun ErrorState(
    error: ApiError,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    val spacing = BrainXPTheme.spacing

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AlertNote(
            title = stringResource(error.titleRes()),
            body = error.bodyText(),
            modifier = Modifier.fillMaxWidth(),
        )

        if (onRetry != null && error.retryable) {
            OutlinedButton(
                onClick = onRetry,
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(OUTLINE_WIDTH, MaterialTheme.colorScheme.outline),
                modifier = Modifier.padding(top = spacing.lg),
            ) {
                Text(
                    text = stringResource(R.string.action_retry),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
fun AlertNote(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    val spacing = BrainXPTheme.spacing

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer,
        border = BorderStroke(NOTE_BORDER_WIDTH, MaterialTheme.colorScheme.error),
    ) {
        Row(
            modifier = Modifier.padding(spacing.md),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Surface(
                modifier = Modifier.size(ICON_SIZE),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.error,
            ) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "!",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onError,
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }
}

private fun ApiError.titleRes(): Int =
    when (this) {
        ApiError.Network -> R.string.error_network_title
        ApiError.Unauthorized -> R.string.error_unauthorized_title
        is ApiError.RateLimited -> R.string.error_rate_limited_title
        ApiError.ServerBusy -> R.string.error_server_busy_title
        is ApiError.Validation -> R.string.error_validation_title
        is ApiError.Unknown -> R.string.error_unknown_title
    }

@Composable
private fun ApiError.bodyText(): String =
    when (this) {
        ApiError.Network -> {
            stringResource(R.string.error_network_body)
        }

        ApiError.Unauthorized -> {
            stringResource(R.string.error_unauthorized_body)
        }

        is ApiError.RateLimited -> {
            retryAfterSeconds
                ?.let { stringResource(R.string.error_rate_limited_body_seconds, it) }
                ?: stringResource(R.string.error_rate_limited_body)
        }

        ApiError.ServerBusy -> {
            stringResource(R.string.error_server_busy_body)
        }

        is ApiError.Validation -> {
            field
                ?.let { stringResource(R.string.error_validation_body_field, it) }
                ?: message ?: stringResource(R.string.error_validation_body)
        }

        is ApiError.Unknown -> {
            code
                ?.let { stringResource(R.string.error_unknown_body_code, it) }
                ?: stringResource(R.string.error_unknown_body)
        }
    }

private val ICON_SIZE = 20.dp
private val NOTE_BORDER_WIDTH = 1.dp
private val OUTLINE_WIDTH = 1.5.dp

@Preview(name = "ErrorState network", showBackground = true)
@Composable
private fun ErrorStateNetworkPreview() {
    BrainXPTheme {
        ErrorState(error = ApiError.Network, onRetry = {})
    }
}

@Preview(name = "ErrorState validation", showBackground = true)
@Composable
private fun ErrorStateValidationPreview() {
    BrainXPTheme {
        ErrorState(error = ApiError.Validation(field = "questionCount", message = null))
    }
}

@Preview(name = "ErrorState dark", showBackground = true)
@Composable
private fun ErrorStateDarkPreview() {
    BrainXPTheme(darkTheme = true) {
        ErrorState(error = ApiError.RateLimited(retryAfterSeconds = 42), onRetry = {})
    }
}
