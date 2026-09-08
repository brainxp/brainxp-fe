package com.example.brainxp.core.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.brainxp.core.result.ApiError
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SharedComposablesTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun primaryButtonRendersAndClicks() {
        var clicks = 0
        compose.setContent {
            BrainXPTheme {
                PrimaryButton(text = "Earn time", onClick = { clicks++ })
            }
        }

        compose
            .onNodeWithText("Earn time")
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun primaryButtonIsNotClickableWhileLoading() {
        var clicks = 0
        compose.setContent {
            BrainXPTheme {
                PrimaryButton(text = "Submitting", onClick = { clicks++ }, loading = true)
            }
        }

        compose.onNodeWithText("Submitting").assertIsNotEnabled()

        assertEquals(0, clicks)
    }

    @Test
    fun primaryButtonRendersInDarkTheme() {
        compose.setContent {
            BrainXPTheme(darkTheme = true) {
                PrimaryButton(text = "Earn time", onClick = {})
            }
        }

        compose.onNodeWithText("Earn time").assertIsDisplayed()
    }

    @Test
    fun loadingStateShowsItsMessage() {
        compose.setContent {
            BrainXPTheme {
                LoadingState(message = "Menyiapkan pertanyaan")
            }
        }

        compose.onNodeWithText("Menyiapkan pertanyaan").assertIsDisplayed()
    }

    @Test
    fun loadingStateRendersEveryStep() {
        compose.setContent {
            BrainXPTheme(darkTheme = true) {
                LoadingState(
                    steps =
                        listOf(
                            LoadingStep("Mengunggah materi", LoadingStepState.Done),
                            LoadingStep("Membaca teks", LoadingStepState.Active),
                            LoadingStep("Menyusun pertanyaan", LoadingStepState.Pending),
                        ),
                )
            }
        }

        compose.onNodeWithText("Mengunggah materi").assertIsDisplayed()
        compose.onNodeWithText("Membaca teks").assertIsDisplayed()
        compose.onNodeWithText("Menyusun pertanyaan").assertIsDisplayed()
    }

    @Test
    fun emptyStateShowsTitleBodyAndAction() {
        var clicks = 0
        compose.setContent {
            BrainXPTheme {
                EmptyState(
                    title = "Belum ada materi",
                    body = "Tambahkan catatan untuk mulai.",
                    actionText = "Tambah materi",
                    onAction = { clicks++ },
                )
            }
        }

        compose.onNodeWithText("Belum ada materi").assertIsDisplayed()
        compose.onNodeWithText("Tambahkan catatan untuk mulai.").assertIsDisplayed()
        compose.onNodeWithText("Tambah materi").performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun emptyStateWithoutActionRendersInDarkTheme() {
        compose.setContent {
            BrainXPTheme(darkTheme = true) {
                EmptyState(title = "Belum cukup data")
            }
        }

        compose.onNodeWithText("Belum cukup data").assertIsDisplayed()
    }

    @Test
    fun errorStateShowsRetryForRetryableErrors() {
        var retries = 0
        compose.setContent {
            BrainXPTheme {
                ErrorState(error = ApiError.Network, onRetry = { retries++ })
            }
        }

        compose.onNodeWithText("Tidak ada koneksi").assertIsDisplayed()
        compose.onNodeWithText("Coba lagi").performClick()

        assertEquals(1, retries)
    }

    @Test
    fun errorStateHidesRetryForNonRetryableErrors() {
        compose.setContent {
            BrainXPTheme {
                ErrorState(
                    error = ApiError.Validation(field = "questionCount", message = null),
                    onRetry = {},
                )
            }
        }

        compose.onNodeWithText("Data tidak diterima").assertIsDisplayed()
        compose.onNodeWithText("Coba lagi").assertDoesNotExist()
    }

    @Test
    fun rateLimitedErrorSurfacesTheRetryAfterSeconds() {
        compose.setContent {
            BrainXPTheme(darkTheme = true) {
                ErrorState(error = ApiError.RateLimited(retryAfterSeconds = 42), onRetry = {})
            }
        }

        compose.onNodeWithText("Coba lagi dalam 42 detik.").assertIsDisplayed()
    }

    @Test
    fun unknownErrorFallsBackToPlainCopyWithoutTheCode() {
        compose.setContent {
            BrainXPTheme {
                ErrorState(error = ApiError.Unknown(code = 418, message = null))
            }
        }

        compose.onNodeWithText("Gagal diproses").assertIsDisplayed()
        compose.onNodeWithText("Coba lagi sekarang. Kalau tetap gagal, tutup lalu buka lagi aplikasinya.").assertIsDisplayed()
    }
}
