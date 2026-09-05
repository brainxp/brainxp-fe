package com.example.brainxp

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.example.brainxp.core.capture.CameraSession
import com.example.brainxp.core.ui.PlaceholderAction
import com.example.brainxp.core.ui.PlaceholderScreen
import com.example.brainxp.feature.SAMPLE_ASSESSED_LEVEL
import com.example.brainxp.feature.SAMPLE_DECLARED_LEVEL
import com.example.brainxp.feature.SAMPLE_ESTIMATE_SECONDS
import com.example.brainxp.feature.SAMPLE_MATERIAL_ID
import com.example.brainxp.feature.SAMPLE_MATERIAL_NAME
import com.example.brainxp.feature.SAMPLE_QUESTION_COUNT
import com.example.brainxp.feature.SAMPLE_QUIZ
import com.example.brainxp.feature.SAMPLE_READY_QUESTIONS
import com.example.brainxp.feature.SAMPLE_RECEIPT
import com.example.brainxp.feature.SAMPLE_REJECT_REASON
import com.example.brainxp.feature.apps.AppPickerRoute
import com.example.brainxp.feature.capture.CameraCaptureScreen
import com.example.brainxp.feature.capture.CaptureMethod
import com.example.brainxp.feature.capture.CaptureViewModel
import com.example.brainxp.feature.capture.PickSourceScreen
import com.example.brainxp.feature.capture.PickSourceViewModel
import com.example.brainxp.feature.capture.PreparingScreen
import com.example.brainxp.feature.capture.PreparingStage
import com.example.brainxp.feature.capture.PreparingViewModel
import com.example.brainxp.feature.capture.RejectedScreen
import com.example.brainxp.feature.debug.DebugUnlockPanel
import com.example.brainxp.feature.family.BalanceAdjustScreen
import com.example.brainxp.feature.family.ChildReportScreen
import com.example.brainxp.feature.family.FamilyHomeScreen
import com.example.brainxp.feature.family.NewChildScreen
import com.example.brainxp.feature.family.PAIRING_CODE_LENGTH
import com.example.brainxp.feature.family.PairDeviceScreen
import com.example.brainxp.feature.family.PairingCodeScreen
import com.example.brainxp.feature.family.PolicyEditorScreen
import com.example.brainxp.feature.family.PolicyEvent
import com.example.brainxp.feature.family.SAMPLE_FAMILY
import com.example.brainxp.feature.family.SAMPLE_PAIRING_CODE
import com.example.brainxp.feature.family.SAMPLE_POLICY
import com.example.brainxp.feature.family.SAMPLE_REPORT
import com.example.brainxp.feature.family.stepped
import com.example.brainxp.feature.home.HomeEffect
import com.example.brainxp.feature.home.HomeScreen
import com.example.brainxp.feature.home.HomeViewModel
import com.example.brainxp.feature.library.LibraryEffect
import com.example.brainxp.feature.library.LibraryScreen
import com.example.brainxp.feature.library.LibraryViewModel
import com.example.brainxp.feature.onboarding.DeviceRole
import com.example.brainxp.feature.onboarding.LevelScreen
import com.example.brainxp.feature.onboarding.LevelViewModel
import com.example.brainxp.feature.onboarding.PickModeScreen
import com.example.brainxp.feature.onboarding.PickRoleScreen
import com.example.brainxp.feature.onboarding.SetupMode
import com.example.brainxp.feature.onboarding.SignInScreen
import com.example.brainxp.feature.onboarding.SignInViewModel
import com.example.brainxp.feature.onboarding.WelcomeScreen
import com.example.brainxp.feature.onboarding.permission.PermissionSetupRoute
import com.example.brainxp.feature.progress.ProgressScreen
import com.example.brainxp.feature.progress.ProgressViewModel
import com.example.brainxp.feature.questions.QuizEvent
import com.example.brainxp.feature.questions.QuizScreen
import com.example.brainxp.feature.questions.recordAnswer
import com.example.brainxp.feature.results.ReceiptScreen
import kotlinx.coroutines.launch

internal fun EntryProviderScope<NavKey>.onboardingEntries(backStack: NavBackStack<NavKey>) {
    entry<OnboardingRoute.Welcome> {
        WelcomeScreen(onStart = { backStack.add(OnboardingRoute.ModeSelect) })
    }
    entry<OnboardingRoute.ModeSelect> {
        PickModeScreen(
            onPick = { mode ->
                when (mode) {
                    SetupMode.FAMILY -> backStack.add(OnboardingRoute.RoleSelect)
                    SetupMode.PERSONAL -> backStack.add(OnboardingRoute.SignIn(family = false))
                }
            },
        )
    }
    entry<OnboardingRoute.RoleSelect> {
        PickRoleScreen(
            onBack = { backStack.popOrIgnore() },
            onPick = { role ->
                when (role) {
                    DeviceRole.PARENT -> backStack.add(OnboardingRoute.SignIn(family = true))
                    DeviceRole.CHILD -> backStack.add(OnboardingRoute.PairDevice)
                }
            },
        )
    }
    entry<OnboardingRoute.SignIn> { key ->
        val viewModel: SignInViewModel = hiltViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()

        LaunchedEffect(state.signedIn) {
            if (state.signedIn) backStack.add(OnboardingRoute.Level)
        }

        SignInScreen(
            mode = if (key.family) SetupMode.FAMILY else SetupMode.PERSONAL,
            onBack = { backStack.popOrIgnore() },
            onSubmit = viewModel::submit,
            busy = state.busy,
            error = state.error,
        )
    }
    entry<OnboardingRoute.PairDevice> {
        var digits by remember { mutableStateOf("") }

        PairDeviceScreen(
            digits = digits,
            onBack = { backStack.popOrIgnore() },
            onKey = { key ->
                if (digits.length < PAIRING_CODE_LENGTH) {
                    digits += key
                    if (digits.length == PAIRING_CODE_LENGTH) {
                        backStack.add(OnboardingRoute.PermissionSetup)
                    }
                }
            },
            onDelete = { digits = digits.dropLast(1) },
        )
    }
}

internal fun EntryProviderScope<NavKey>.onboardingTailEntries(
    backStack: NavBackStack<NavKey>,
    onSetupComplete: () -> Unit,
) {
    entry<OnboardingRoute.Level> {
        val viewModel: LevelViewModel = hiltViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()

        LaunchedEffect(state.saved) {
            if (state.saved) backStack.add(OnboardingRoute.PermissionSetup)
        }

        LevelScreen(
            onSubmit = viewModel::submit,
            onBack = { backStack.popOrIgnore() },
            busy = state.busy,
            error = state.error,
        )
    }
    entry<OnboardingRoute.PermissionSetup> {
        PermissionSetupRoute(
            onDone = { backStack.add(OnboardingRoute.SetupDone) },
        )
    }
    entry<OnboardingRoute.SetupDone> {
        Placeholder("Selesai", "OnboardingRoute.SetupDone") {
            listOf(PlaceholderAction("Finish setup", onSetupComplete))
        }
    }
}

internal data class DebugActions(
    val grantUnlock: (Int) -> Unit,
    val setWarningLead: (Int) -> Unit,
    val endUnlock: () -> Unit,
)

internal fun EntryProviderScope<NavKey>.dailyEntries(
    backStack: NavBackStack<NavKey>,
    onResetSetup: () -> Unit,
    onToggleProtection: () -> Unit,
) {
    entry<MainRoute.Home> {
        val viewModel: HomeViewModel = hiltViewModel()
        val homeState by viewModel.state.collectAsStateWithLifecycle()
        val context = LocalContext.current

        LaunchedEffect(viewModel) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    HomeEffect.OpenAddMaterial -> backStack.add(MainRoute.Capture)
                    HomeEffect.OpenPermissionSetup -> backStack.add(MainRoute.PermissionSetup)
                    HomeEffect.OpenLibrary -> backStack.add(MainRoute.MaterialList)
                    HomeEffect.OpenProgress -> backStack.add(MainRoute.Progress)
                    is HomeEffect.LaunchApp -> launchApp(context, effect.packageName)
                }
            }
        }

        Column {
            if (BuildConfig.DEBUG) {
                TextButton(onClick = { backStack.add(MainRoute.DebugMenu) }) {
                    Text("Debug menu")
                }
            }
            HomeScreen(
                state = homeState,
                onEvent = viewModel::onEvent,
                modifier = Modifier.weight(1f),
            )
        }
    }
    entry<MainRoute.AppPicker> { AppPickerRoute() }
    entry<MainRoute.History> { Placeholder("History", "MainRoute.History") }
    entry<MainRoute.Progress> {
        val viewModel: ProgressViewModel = hiltViewModel()
        val progressState by viewModel.state.collectAsStateWithLifecycle()

        ProgressScreen(
            state = progressState,
            onRetry = viewModel::retry,
            onBack = { backStack.popOrIgnore() },
        )
    }
    entry<MainRoute.ActivityLog> { Placeholder("Activity log", "MainRoute.ActivityLog") }
    entry<MainRoute.Settings> {
        Placeholder("Settings", "MainRoute.Settings") {
            listOf(
                action("Restricted apps", backStack, MainRoute.AppPicker),
                action("Permissions", backStack, MainRoute.PermissionSetup),
                PlaceholderAction("Toggle protection", onToggleProtection),
                PlaceholderAction("Re-run setup", onResetSetup),
            )
        }
    }
    entry<MainRoute.PermissionSetup> {
        PermissionSetupRoute(
            onDone = { backStack.popOrIgnore() },
            reentrant = true,
        )
    }
}

internal fun EntryProviderScope<NavKey>.debugEntries(
    backStack: NavBackStack<NavKey>,
    onResetSetup: () -> Unit,
    onToggleProtection: () -> Unit,
    debug: DebugActions,
) {
    entry<MainRoute.DebugMenu> {
        DebugDestinations(backStack, onResetSetup, onToggleProtection)
    }
    entry<MainRoute.DebugUnlock> {
        DebugUnlockPanel(
            onGrant = debug.grantUnlock,
            onSetWarningLead = debug.setWarningLead,
            onEndUnlock = debug.endUnlock,
        )
    }
}

internal fun EntryProviderScope<NavKey>.cameraEntries(backStack: NavBackStack<NavKey>) {
    entry<MainRoute.CameraCapture> {
        val viewModel: CaptureViewModel = hiltViewModel()
        val captureState by viewModel.state.collectAsStateWithLifecycle()
        val context = LocalContext.current
        val owner = LocalLifecycleOwner.current
        val session = remember { CameraSession(context) }
        val scope = rememberCoroutineScope()

        CameraCaptureScreen(
            state = captureState,
            onBack = { backStack.popOrIgnore() },
            bindCamera = { view -> session.bind(owner, view) },
            onShutter = {
                if (!captureState.capturing) {
                    val target = viewModel.newPageFile()
                    viewModel.beginCapture()
                    scope.launch {
                        session
                            .takePicture(target)
                            .onSuccess { viewModel.captured(it) }
                            .onFailure { viewModel.captureFailed(target) }
                    }
                }
            },
            onDelete = viewModel::delete,
            onMove = viewModel::move,
            onContinue = {
                viewModel.uploadAll()
                backStack.popOrIgnore()
            },
        )
    }
}

private const val PENDING_MATERIAL = "pending"

internal fun EntryProviderScope<NavKey>.captureEntries(backStack: NavBackStack<NavKey>) {
    entry<MainRoute.Capture> {
        val picker: PickSourceViewModel = hiltViewModel()
        val rejection by picker.rejection.collectAsStateWithLifecycle()

        PickSourceScreen(
            rejection = rejection?.let { stringResource(it) },
            onPicked = { uri -> picker.accept(uri) { backStack.add(MainRoute.Preparing(PENDING_MATERIAL)) } },
            questionCount = SAMPLE_QUESTION_COUNT,
            estimatedRewardSeconds = SAMPLE_ESTIMATE_SECONDS,
            onBack = { backStack.popOrIgnore() },
            onPick = { method ->
                when (method) {
                    CaptureMethod.PHOTO -> backStack.add(MainRoute.CameraCapture)
                    CaptureMethod.DOCUMENT -> backStack.add(MainRoute.Preparing(SAMPLE_MATERIAL_ID))
                }
            },
        )
    }
    entry<MainRoute.Preparing> {
        val viewModel: PreparingViewModel = hiltViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()

        LaunchedEffect(state.rejected) {
            if (state.rejected) {
                viewModel.done()
                backStack.add(MainRoute.Rejected(state.materialId ?: PENDING_MATERIAL))
            }
        }

        PreparingScreen(
            materialName = state.materialName,
            stage = state.stage,
            readyQuestions = state.readyQuestions,
            onStart = {
                val id = state.materialId ?: return@PreparingScreen
                viewModel.done()
                backStack.add(MainRoute.Questions(id))
            },
        )
    }
    entry<MainRoute.Rejected> {
        RejectedScreen(
            assessedLevel = SAMPLE_ASSESSED_LEVEL,
            declaredLevel = SAMPLE_DECLARED_LEVEL,
            reason = SAMPLE_REJECT_REASON,
            onBack = { backStack.popOrIgnore() },
            onRetry = { backStack.popOrIgnore() },
        )
    }
}

internal fun EntryProviderScope<NavKey>.learningEntries(backStack: NavBackStack<NavKey>) {
    entry<MainRoute.MaterialList> {
        val viewModel: LibraryViewModel = hiltViewModel()
        val libraryState by viewModel.state.collectAsStateWithLifecycle()
        val context = LocalContext.current

        LaunchedEffect(viewModel) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    is LibraryEffect.OpenMaterial -> {
                        backStack.add(MainRoute.MaterialDetail(effect.materialId))
                    }

                    is LibraryEffect.RemoveFailed -> {
                        Toast
                            .makeText(context, R.string.library_remove_failed, Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
        }

        LibraryScreen(
            state = libraryState,
            onEvent = viewModel::onEvent,
            onBack = { backStack.popOrIgnore() },
        )
    }
    entry<MainRoute.MaterialDetail> { key ->
        Placeholder("Material detail", "materialId = ${key.materialId}") {
            listOf(action("Start session", backStack, MainRoute.Questions("session-1")))
        }
    }
    entry<MainRoute.Questions> { key ->
        var quiz by remember { mutableStateOf(SAMPLE_QUIZ) }

        QuizScreen(
            state = quiz,
            onBack = { backStack.popOrIgnore() },
            onEvent = { event ->
                when (event) {
                    is QuizEvent.Jump -> quiz = quiz.copy(index = event.index, chosen = null, draft = "")
                    is QuizEvent.Choose -> quiz = quiz.copy(chosen = event.option)
                    is QuizEvent.Draft -> quiz = quiz.copy(draft = event.text)
                    QuizEvent.Save -> quiz = quiz.recordAnswer()
                    QuizEvent.Submit -> backStack.add(MainRoute.Results(key.sessionId))
                }
            },
        )
    }
    entry<MainRoute.Results> {
        ReceiptScreen(
            state = SAMPLE_RECEIPT,
            onHome = { backStack.popOrIgnore() },
            onLibrary = { backStack.add(MainRoute.MaterialList) },
        )
    }
}

@Composable
private fun Placeholder(
    name: String,
    detail: String,
    actions: () -> List<PlaceholderAction> = { emptyList() },
) {
    PlaceholderScreen(name = name, detail = detail, actions = actions())
}

private fun action(
    label: String,
    backStack: NavBackStack<NavKey>,
    target: NavKey,
) = PlaceholderAction(label) { backStack.add(target) }

@Composable
private fun DebugDestinations(
    backStack: NavBackStack<NavKey>,
    onResetSetup: () -> Unit,
    onToggleProtection: () -> Unit,
) {
    PlaceholderScreen(
        name = "Debug",
        detail = "Semua tujuan navigasi, hanya di build debug",
        actions =
            listOf(
                action("Restricted apps", backStack, MainRoute.AppPicker),
                action("Permissions", backStack, MainRoute.PermissionSetup),
                action("Materials", backStack, MainRoute.MaterialList),
                action("Capture", backStack, MainRoute.Capture),
                action("Camera", backStack, MainRoute.CameraCapture),
                action("Rejected", backStack, MainRoute.Rejected(SAMPLE_MATERIAL_ID)),
                action("Receipt", backStack, MainRoute.Results("session-1")),
                action("History", backStack, MainRoute.History),
                action("Progress", backStack, MainRoute.Progress),
                action("Activity log", backStack, MainRoute.ActivityLog),
                action("Family", backStack, MainRoute.FamilyHome),
                action("New child", backStack, MainRoute.FamilyNewChild),
                action("Child policy", backStack, MainRoute.FamilyChildPolicy("child-1")),
                action("Pairing code", backStack, MainRoute.FamilyPairing("child-1")),
                action("Balance adjust", backStack, MainRoute.FamilyBalance("child-1")),
                action("Settings", backStack, MainRoute.Settings),
                action("Sesi uji", backStack, MainRoute.DebugUnlock),
                PlaceholderAction("Toggle protection", onToggleProtection),
                PlaceholderAction("Re-run setup", onResetSetup),
            ),
    )
}

private fun launchApp(
    context: Context,
    packageName: String,
) {
    val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}
