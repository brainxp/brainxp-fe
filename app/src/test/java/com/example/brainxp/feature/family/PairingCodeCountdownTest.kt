package com.example.brainxp.feature.family

import com.example.brainxp.core.result.AppResult
import com.example.brainxp.data.repo.FamilyRepository
import com.example.brainxp.domain.model.AcademicLevel
import com.example.brainxp.domain.model.DeviceBinding
import com.example.brainxp.domain.model.FamilyChild
import com.example.brainxp.domain.model.GuardianEvent
import com.example.brainxp.domain.model.GuardianStatus
import com.example.brainxp.domain.model.PairingCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

private class StubFamily(
    private val secondsAhead: Long,
) : FamilyRepository {
    override suspend fun children(): AppResult<List<FamilyChild>> =
        AppResult.Success(listOf(FamilyChild(childId = "child-1", name = "Raka", level = AcademicLevel.SMP)))

    override suspend fun createChild(
        name: String,
        level: AcademicLevel,
        language: String,
    ): AppResult<FamilyChild> = error("not used")

    override suspend fun createSelfSubject(level: AcademicLevel): AppResult<FamilyChild> = error("not used")

    override suspend fun removeChild(childId: String): AppResult<Unit> = error("not used")

    override suspend fun releaseDevice(childId: String): AppResult<Unit> = error("not used")

    override suspend fun pairingCode(childId: String): AppResult<PairingCode> =
        AppResult.Success(
            PairingCode(
                code = "190196",
                childId = childId,
                expiresAt = Instant.now().plusSeconds(secondsAhead).toString(),
                attemptsAllowed = 5,
            ),
        )

    override suspend fun pair(code: String): AppResult<Unit> = error("not used")

    override suspend fun checkBinding(): AppResult<DeviceBinding> = error("not used")

    override suspend fun reportHealth(
        status: GuardianStatus,
        events: List<GuardianEvent>,
    ): AppResult<Unit> = error("not used")
}

@OptIn(ExperimentalCoroutinesApi::class)
class PairingCodeCountdownTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun takeOverTheMainThread() = Dispatchers.setMain(dispatcher)

    @After
    fun giveTheMainThreadBack() = Dispatchers.resetMain()

    @Test
    fun `the code arrives with about as long left as the server gave it`() =
        runTest(dispatcher) {
            val viewModel = PairingCodeViewModel(StubFamily(secondsAhead = 600))

            viewModel.load("child-1")
            runCurrent()

            val left =
                viewModel.state.value.code
                    ?.secondsLeft ?: 0
            assertTrue("$left", left in 595..600)
        }

    @Test
    fun `the time left keeps falling while the code is on screen`() =
        runTest(dispatcher) {
            val viewModel = PairingCodeViewModel(StubFamily(secondsAhead = 600))

            viewModel.load("child-1")
            runCurrent()
            val started =
                viewModel.state.value.code
                    ?.secondsLeft ?: 0

            advanceTimeBy(5_000)
            runCurrent()

            assertEquals(
                started - 5,
                viewModel.state.value.code
                    ?.secondsLeft,
            )
        }

    @Test
    fun `the countdown stops at zero instead of going negative`() =
        runTest(dispatcher) {
            val viewModel = PairingCodeViewModel(StubFamily(secondsAhead = 3))

            viewModel.load("child-1")
            runCurrent()
            advanceTimeBy(30_000)
            runCurrent()

            assertEquals(
                0,
                viewModel.state.value.code
                    ?.secondsLeft,
            )
        }
}
