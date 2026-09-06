package com.example.brainxp

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import com.example.brainxp.core.ui.ErrorState
import com.example.brainxp.core.ui.LoadingState
import com.example.brainxp.core.ui.ParentPinDialog
import com.example.brainxp.core.ui.levelLabel
import com.example.brainxp.core.upload.PreparingStage
import com.example.brainxp.domain.model.AcademicLevel
import com.example.brainxp.domain.model.DeviceRole
import com.example.brainxp.feature.SAMPLE_ESTIMATE_SECONDS
import com.example.brainxp.feature.SAMPLE_MATERIAL_ID
import com.example.brainxp.feature.SAMPLE_QUESTION_COUNT
import com.example.brainxp.feature.activity.ActivityLogScreen
import com.example.brainxp.feature.activity.ActivityLogViewModel
import com.example.brainxp.feature.apps.AppPickerRoute
import com.example.brainxp.feature.capture.CameraCaptureScreen
import com.example.brainxp.feature.capture.CaptureMethod
import com.example.brainxp.feature.capture.CaptureViewModel
import com.example.brainxp.feature.capture.PickSourceScreen
import com.example.brainxp.feature.capture.PickSourceViewModel
import com.example.brainxp.feature.capture.PreparationFooter
import com.example.brainxp.feature.capture.PreparingScreen
import com.example.brainxp.feature.capture.PreparingViewModel
import com.example.brainxp.feature.capture.RejectedScreen
import com.example.brainxp.feature.capture.RejectedViewModel
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
import com.example.brainxp.feature.health.ProtectionRow
import com.example.brainxp.feature.history.HistoryScreen
import com.example.brainxp.feature.history.HistoryViewModel
import com.example.brainxp.feature.home.HomeEffect
import com.example.brainxp.feature.home.HomeScreen
import com.example.brainxp.feature.home.HomeViewModel
import com.example.brainxp.feature.legal.DeleteAccountScreen
import com.example.brainxp.feature.legal.DeleteAccountViewModel
import com.example.brainxp.feature.legal.PrivacyPolicyScreen
import com.example.brainxp.feature.library.LibraryEffect
import com.example.brainxp.feature.library.LibraryScreen
import com.example.brainxp.feature.library.LibraryViewModel
import com.example.brainxp.feature.library.MaterialDetailScreen
import com.example.brainxp.feature.library.MaterialDetailViewModel
import com.example.brainxp.feature.onboarding.LevelScreen
import com.example.brainxp.feature.onboarding.LevelViewModel
import com.example.brainxp.feature.onboarding.PairDeviceViewModel
import com.example.brainxp.feature.onboarding.PickModeScreen
import com.example.brainxp.feature.onboarding.PickRoleScreen
import com.example.brainxp.feature.onboarding.SetParentPinScreen
import com.example.brainxp.feature.onboarding.SetParentPinViewModel
import com.example.brainxp.feature.onboarding.SetupDoneScreen
import com.example.brainxp.feature.onboarding.SetupMode
import com.example.brainxp.feature.onboarding.SignInScreen
import com.example.brainxp.feature.onboarding.SignInViewModel
import com.example.brainxp.feature.onboarding.WelcomeScreen
import com.example.brainxp.feature.onboarding.permission.PermissionSetupRoute
import com.example.brainxp.feature.progress.ProgressScreen
import com.example.brainxp.feature.progress.ProgressViewModel
import com.example.brainxp.feature.questions.QuizEvent
import com.example.brainxp.feature.questions.QuizScreen
import com.example.brainxp.feature.questions.QuizTourScreen
import com.example.brainxp.feature.questions.QuizViewModel
import com.example.brainxp.feature.results.ReceiptScreen
import com.example.brainxp.feature.results.ReceiptViewModel
import com.example.brainxp.feature.settings.SettingsScreen
import com.example.brainxp.feature.settings.SettingsViewModel
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
            if (!state.signedIn) return@LaunchedEffect
            viewModel.consumeSignIn()
            if (key.family) {
                backStack.add(OnboardingRoute.PermissionSetup)
            } else {
                backStack.add(OnboardingRoute.Level)
            }
        }

        SignInScreen(
            mode = if (key.family) SetupMode.FAMILY else SetupMode.PERSONAL,
            onBack = { backStack.popOrIgnore() },
            onSubmit = viewModel::submit,
            onPrivacyPolicy = { backStack.add(OnboardingRoute.PrivacyPolicy) },
            busy = state.busy,
            error = state.error,
        )
    }
    entry<OnboardingRoute.PrivacyPolicy> {
        PrivacyPolicyScreen(onBack = { backStack.popOrIgnore() })
    }
    entry<OnboardingRoute.PairDevice> { PairDeviceEntry(backStack) }
    entry<OnboardingRoute.SetParentPin> { SetParentPinEntry(backStack) }
}

internal fun EntryProviderScope<NavKey>.onboardingTailEntries(
    backStack: NavBackStack<NavKey>,
    onSetupComplete: () -> Unit,
) {
    entry<OnboardingRoute.Level> {
        val viewModel: LevelViewModel = hiltViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()

        LaunchedEffect(state.saved) {
            if (!state.saved) return@LaunchedEffect
            viewModel.consumeSaved()
            backStack.add(OnboardingRoute.PermissionSetup)
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
        SetupDoneScreen(onStart = onSetupComplete)
    }
}

private fun openHome(
    effect: HomeEffect,
    backStack: NavBackStack<NavKey>,
    context: android.content.Context,
) {
    when (effect) {
        HomeEffect.OpenAddMaterial -> backStack.add(MainRoute.Capture)
        HomeEffect.OpenPermissionSetup -> backStack.add(MainRoute.PermissionSetup)
        HomeEffect.OpenLibrary -> backStack.add(MainRoute.MaterialList)
        HomeEffect.OpenProgress -> backStack.add(MainRoute.Progress)
        HomeEffect.OpenSettings -> backStack.add(MainRoute.Settings)
        HomeEffect.OpenHistory -> backStack.add(MainRoute.History)
        HomeEffect.OpenActivity -> backStack.add(MainRoute.ActivityLog)
        HomeEffect.OpenApps -> backStack.add(MainRoute.AppPicker)
        HomeEffect.OpenPreparing -> backStack.add(MainRoute.Preparing(PENDING_MATERIAL))
        is HomeEffect.OpenQuestions -> backStack.add(MainRoute.Questions(effect.materialId))
        is HomeEffect.LaunchApp -> launchApp(context, effect.packageName)
    }
}

internal fun EntryProviderScope<NavKey>.dailyEntries(backStack: NavBackStack<NavKey>) {
    entry<MainRoute.Home> {
        val viewModel: HomeViewModel = hiltViewModel()
        val homeState by viewModel.state.collectAsStateWithLifecycle()
        val context = LocalContext.current

        LaunchedEffect(viewModel) {
            viewModel.effects.collect { effect -> openHome(effect, backStack, context) }
        }

        Column {
            HomeScreen(
                state = homeState,
                onEvent = viewModel::onEvent,
                modifier = Modifier.weight(1f),
            )
        }

        if (homeState.pinRequired) {
            ParentPinDialog(
                title = stringResource(R.string.pin_title_protection),
                onSubmit = viewModel::submitPin,
                onDismiss = viewModel::dismissPin,
                wrong = homeState.pinWrong,
            )
        }
    }
    entry<MainRoute.AppPicker> { AppPickerRoute(onBack = { backStack.popOrIgnore() }) }
    entry<MainRoute.History> { HistoryEntry(backStack) }
    entry<MainRoute.Progress> {
        val viewModel: ProgressViewModel = hiltViewModel()
        val progressState by viewModel.state.collectAsStateWithLifecycle()

        ProgressScreen(
            state = progressState,
            onRetry = viewModel::retry,
            onBack = { backStack.popOrIgnore() },
        )
    }
    entry<MainRoute.ActivityLog> { ActivityLogEntry(backStack) }
    entry<MainRoute.Settings> { SettingsEntry(backStack) }
    entry<MainRoute.PrivacyPolicy> {
        PrivacyPolicyScreen(onBack = { backStack.popOrIgnore() })
    }
    entry<MainRoute.DeleteAccount> { DeleteAccountEntry(backStack) }
    entry<MainRoute.PermissionSetup> {
        PermissionSetupRoute(
            onDone = { backStack.popOrIgnore() },
            reentrant = true,
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
        val methods by picker.uploadMethods.collectAsStateWithLifecycle()

        PickSourceScreen(
            rejection = rejection?.let { stringResource(it) },
            methods = methods,
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

        val start = {
            state.materialId?.let { id ->
                viewModel.done()
                backStack.popOrIgnore()
                backStack.add(MainRoute.Questions(id))
            }
            Unit
        }

        if (state.guide) {
            QuizTourScreen(
                onDone = { viewModel.showGuide(false) },
                onSkip = { viewModel.showGuide(false) },
                ready = state.ready,
                onStart = start,
                footer = { PreparationFooter(state = state, onStart = start) },
            )
        } else {
            PreparingScreen(
                state = state,
                onRetry = viewModel::retry,
                onLeave = { backStack.popOrIgnore() },
                onStart = start,
            )
        }
    }
    entry<MainRoute.Rejected> { key -> RejectedEntry(key, backStack) }
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
    entry<MainRoute.MaterialDetail> { key -> MaterialDetailEntry(key, backStack) }
    entry<MainRoute.Questions> { key -> QuestionsEntry(key, backStack) }
    entry<MainRoute.Results> { key -> ReceiptEntry(key, backStack) }
}

private fun launchApp(
    context: Context,
    packageName: String,
) {
    val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

@Composable
private fun QuestionsEntry(
    key: MainRoute.Questions,
    backStack: NavBackStack<NavKey>,
) {
    val viewModel: QuizViewModel = hiltViewModel()
    val load by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(key.materialId) { viewModel.start(key.materialId) }

    val quiz = load.quiz
    when {
        load.error != null -> {
            ErrorState(error = load.error!!, onRetry = { viewModel.start(key.materialId, restart = true) })
        }

        quiz == null -> {
            LoadingState()
        }

        load.tour -> {
            QuizTourScreen(
                onDone = { viewModel.showGuide(false) },
                onSkip = { viewModel.showGuide(false) },
            )
        }

        else -> {
            QuizScreen(
                state = quiz,
                onBack = { backStack.popOrIgnore() },
                onGuide = { viewModel.showGuide(true) },
                onEvent = { event ->
                    viewModel.onEvent(event)
                    if (event == QuizEvent.Submit) {
                        load.sessionId?.let { sessionId ->
                            backStack.popOrIgnore()
                            backStack.add(MainRoute.Results(sessionId))
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun RejectedEntry(
    key: MainRoute.Rejected,
    backStack: NavBackStack<NavKey>,
) {
    val viewModel: RejectedViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(key.materialId) { viewModel.load(key.materialId) }

    when {
        state.error != null -> {
            ErrorState(error = state.error!!, onRetry = viewModel::retry)
        }

        state.loading -> {
            LoadingState()
        }

        else -> {
            RejectedScreen(
                assessedLevel = levelText(state.assessedLevel),
                declaredLevel = levelText(state.declaredLevel),
                reason = reasonText(state.reasonCode),
                onBack = { backStack.popOrIgnore() },
                onRetry = { backStack.popOrIgnore() },
            )
        }
    }
}

@Composable
private fun levelText(wire: String?): String =
    AcademicLevel.fromWire(wire)?.let { levelLabel(it) } ?: stringResource(R.string.reject_level_unknown)

@Composable
private fun reasonText(code: String?): String =
    when {
        code == null -> stringResource(R.string.reject_reason_unknown)
        code == "level_too_low" -> stringResource(R.string.reject_reason_level_too_low)
        code == "too_thin" || code == "too_short" -> stringResource(R.string.reject_reason_too_thin)
        code == "unreadable" -> stringResource(R.string.reject_reason_unreadable)
        code.contains(' ') -> code
        else -> stringResource(R.string.reject_reason_unknown)
    }

@Composable
private fun MaterialDetailEntry(
    key: MainRoute.MaterialDetail,
    backStack: NavBackStack<NavKey>,
) {
    val viewModel: MaterialDetailViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(key.materialId) { viewModel.load(key.materialId) }
    LaunchedEffect(state.deleted) { if (state.deleted) backStack.popOrIgnore() }

    val material = state.material
    when {
        state.error != null && material == null -> {
            ErrorState(error = state.error!!, onRetry = viewModel::retry)
        }

        material == null -> {
            LoadingState()
        }

        else -> {
            MaterialDetailScreen(
                material = material,
                onStart = { backStack.add(MainRoute.Questions(key.materialId)) },
                onDelete = viewModel::delete,
                onBack = { backStack.popOrIgnore() },
                deleting = state.deleting,
            )
        }
    }
}

@Composable
private fun ReceiptEntry(
    key: MainRoute.Results,
    backStack: NavBackStack<NavKey>,
) {
    val viewModel: ReceiptViewModel = hiltViewModel()
    val load by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(key.sessionId) { viewModel.submit(key.sessionId) }

    val receipt = load.receipt
    when {
        load.error != null && receipt == null -> {
            ErrorState(error = load.error!!, onRetry = viewModel::retry)
        }

        receipt == null -> {
            LoadingState()
        }

        else -> {
            ReceiptScreen(
                state = receipt,
                onHome = { backStack.popOrIgnore() },
                onLibrary = { backStack.add(MainRoute.MaterialList) },
            )
        }
    }
}

@Composable
private fun HistoryEntry(backStack: NavBackStack<NavKey>) {
    val viewModel: HistoryViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    when {
        state.error != null -> ErrorState(error = state.error!!, onRetry = viewModel::retry)
        state.loading -> LoadingState()
        else -> HistoryScreen(entries = state.entries, onBack = { backStack.popOrIgnore() })
    }
}

@Composable
private fun ActivityLogEntry(backStack: NavBackStack<NavKey>) {
    val viewModel: ActivityLogViewModel = hiltViewModel()
    val events by viewModel.events.collectAsStateWithLifecycle()

    ActivityLogScreen(events = events, onBack = { backStack.popOrIgnore() })
}

@Composable
private fun SettingsEntry(backStack: NavBackStack<NavKey>) {
    val viewModel: SettingsViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.signedOut) {
        if (state.signedOut) backStack.popOrIgnore()
    }

    val policy = state.policy
    when {
        state.error != null && policy == null -> {
            ErrorState(error = state.error!!, onRetry = viewModel::retry)
        }

        policy == null -> {
            LoadingState()
        }

        else -> {
            SettingsScreen(
                policy = policy,
                onEdit = viewModel::edit,
                onApps = { backStack.add(MainRoute.AppPicker) },
                onPermissions = { backStack.add(MainRoute.PermissionSetup) },
                onPrivacyPolicy = { backStack.add(MainRoute.PrivacyPolicy) },
                onDeleteAccount = { backStack.add(MainRoute.DeleteAccount) },
                onSignOut = viewModel::signOut,
                onBack = { backStack.popOrIgnore() },
                saving = state.saving,
                notice = state.notice,
                protection = { ProtectionRow() },
            )
        }
    }

    if (state.pinRequired) {
        ParentPinDialog(
            title = stringResource(R.string.pin_title_mode),
            onSubmit = viewModel::submitPin,
            onDismiss = viewModel::dismissPin,
            wrong = state.pinWrong,
        )
    }
}

@Composable
private fun DeleteAccountEntry(backStack: NavBackStack<NavKey>) {
    val viewModel: DeleteAccountViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var noMailApp by remember { mutableStateOf(false) }
    val address = stringResource(R.string.privacy_contact_email)
    val subject = stringResource(R.string.delete_account_mail_subject)
    val body = stringResource(R.string.delete_account_mail_body, state.subjectId.orEmpty())

    DeleteAccountScreen(
        onSendRequest = {
            noMailApp = !launchMailRequest(context, address, subject, body)
        },
        onBack = { backStack.popOrIgnore() },
        onPrivacyPolicy = { backStack.add(MainRoute.PrivacyPolicy) },
        noMailApp = noMailApp,
    )
}

@Suppress("SwallowedException")
private fun launchMailRequest(
    context: Context,
    address: String,
    subject: String,
    body: String,
): Boolean {
    val intent =
        Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$address")
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
    return try {
        context.startActivity(intent)
        true
    } catch (ignored: ActivityNotFoundException) {
        false
    }
}

@Composable
private fun PairDeviceEntry(backStack: NavBackStack<NavKey>) {
    val viewModel: PairDeviceViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.paired) {
        if (!state.paired) return@LaunchedEffect
        viewModel.consumePaired()
        backStack.add(OnboardingRoute.SetParentPin)
    }

    PairDeviceScreen(
        digits = state.digits,
        onBack = { backStack.popOrIgnore() },
        onKey = viewModel::press,
        onDelete = viewModel::backspace,
        busy = state.busy,
        error = state.error?.let { stringResource(R.string.pair_failed) },
    )
}

@Composable
private fun SetParentPinEntry(backStack: NavBackStack<NavKey>) {
    val viewModel: SetParentPinViewModel = hiltViewModel()
    val saved by viewModel.saved.collectAsStateWithLifecycle()

    LaunchedEffect(saved) {
        if (!saved) return@LaunchedEffect
        viewModel.consumeSaved()
        backStack.add(OnboardingRoute.PermissionSetup)
    }

    SetParentPinScreen(
        onSet = viewModel::set,
        onSkip = { backStack.add(OnboardingRoute.PermissionSetup) },
    )
}
