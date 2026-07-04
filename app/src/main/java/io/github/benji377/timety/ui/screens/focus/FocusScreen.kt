package io.github.benji377.timety.ui.screens.focus

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Task
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.focus.FocusModeType
import io.github.benji377.timety.data.model.focus.FocusTargetType
import io.github.benji377.timety.data.model.focus.PhaseType
import io.github.benji377.timety.data.model.focus.SessionPhaseEntity
import io.github.benji377.timety.services.FocusTimerManager
import io.github.benji377.timety.services.FocusTimerService
import io.github.benji377.timety.ui.components.common.TextInputDialog
import io.github.benji377.timety.ui.components.focus.DistractionBottomSheet
import io.github.benji377.timety.ui.components.focus.InteractiveGauge
import io.github.benji377.timety.ui.components.focus.ModeTimeline
import io.github.benji377.timety.ui.components.focus.TargetSelectorBottomSheet
import io.github.benji377.timety.ui.components.focus.TimeMachineDialog
import io.github.benji377.timety.ui.components.focus.localizedFocusModeName
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.ErrorColor
import io.github.benji377.timety.ui.theme.FocusColor
import io.github.benji377.timety.ui.theme.HabitColor
import io.github.benji377.timety.ui.theme.TaskColor
import io.github.benji377.timety.ui.theme.WarningColor
import io.github.benji377.timety.ui.viewmodel.AppViewModelProvider
import io.github.benji377.timety.ui.viewmodel.FocusViewModel
import io.github.benji377.timety.ui.viewmodel.HabitViewModel
import io.github.benji377.timety.ui.viewmodel.SettingsViewModel
import io.github.benji377.timety.ui.viewmodel.TaskViewModel
import io.github.benji377.timety.util.datetime.AppDateFormatUtils
import io.github.benji377.timety.util.habit.HabitUtils
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.roundToInt

/** Roughly a day; used as a stand-in "infinite" duration so Stopwatch mode can count up under
 * the current [FocusTimerManager], which only knows how to count down from a fixed total. */
private fun secondsForPhase(
    phase: io.github.benji377.timety.data.model.focus.SessionPhaseEntity,
    flexibleMinutes: Int
): Int = when {
    phase.durationMinutes == 0 -> 0 // Stopwatch
    phase.durationMinutes == -1 -> flexibleMinutes * 60
    else -> phase.durationMinutes * 60
}

/**
 * The main focus timer screen: mode selector, interactive gauge, phase timeline, and transport
 * controls. Mirrors `screens/focus/focus_screen.dart`.
 *
 * NOTE (timer-engine scope boundary - see report): [FocusTimerManager]/[FocusTimerService] are
 * out of scope for this port (a separate phase) and only support a single countdown phase at a
 * time. This screen works within that: it drives `setMode`/start/pause/stop using the engine's
 * existing public API to approximate Flutter's stopwatch (counts up, here via a 24h countdown +
 * elapsed-time display), flexible (drag-to-set duration before starting), and multi-phase
 * (focus/rest sequence with a "continue" prompt) modes, and detects phase completion generically
 * by observing `FocusTimerManager.timerState` (not just the focus-only `sessionCompleteEvent`) so
 * rest-phase boundaries also prompt "continue". True parity (one logged session per whole
 * multi-phase run, dynamic target auto-complete, habit/task XP wiring on finish) needs engine
 * changes and should land with the timer-service phase.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusScreen(
    onNavigateToModes: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    focusViewModel: FocusViewModel = viewModel(factory = AppViewModelProvider.Factory),
    settingsViewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory),
    taskViewModel: TaskViewModel = viewModel(factory = AppViewModelProvider.Factory),
    habitViewModel: HabitViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val timerState by FocusTimerManager.timerState.collectAsState()
    val isRunning = timerState.isRunning
    val isPaused = timerState.isPaused

    androidx.compose.runtime.LaunchedEffect(focusViewModel) {
        focusViewModel.autoCompleteTaskEvent.collect { taskId ->
            taskViewModel.markTaskCompleted(taskId)
        }
    }

    val allModes by focusViewModel.allModes.collectAsState()
    val modeIndex by focusViewModel.currentModeIndex.collectAsState()
    val activeMode = allModes.getOrNull(modeIndex)
    var activePhases by remember { mutableStateOf<List<SessionPhaseEntity>>(emptyList()) }
    val phaseIndex by focusViewModel.currentPhaseIndex.collectAsState()
    val awaitingContinue by focusViewModel.awaitingContinue.collectAsState()

    val selectedTarget by focusViewModel.selectedTarget.collectAsState()
    val allTags by focusViewModel.allTags.collectAsState()
    val allSessions by focusViewModel.allSessions.collectAsState()
    val tasks by taskViewModel.allTasks.collectAsState()
    val habitsWithCompletions by habitViewModel.habitsWithCompletions.collectAsState()
    val dailyGoalMins by settingsViewModel.dailyGoalMins.collectAsState()

    var flexibleMinutes by remember { mutableStateOf(25) }

    var showTargetSelection by remember { mutableStateOf(false) }
    var showTimeMachine by remember { mutableStateOf(false) }
    var showDistraction by remember { mutableStateOf(false) }
    var showCreateTagDialog by remember { mutableStateOf(false) }
    var showStopConfirmation by remember { mutableStateOf(false) }
    var showResetConfirmation by remember { mutableStateOf(false) }
    var stopWasRunning by remember { mutableStateOf(false) }
    var resetWasRunning by remember { mutableStateOf(false) }

    fun sendAction(action: String) {
        val intent = Intent(context, FocusTimerService::class.java).apply { this.action = action }
        ContextCompat.startForegroundService(context, intent)
    }

    // Load the active mode's phases and reset the phase cursor whenever the mode changes.
    LaunchedEffect(activeMode?.id) {
        focusViewModel.resetPhaseIndex()
        val mode = activeMode
        if (mode != null) {
            focusViewModel.getPhasesForMode(mode.id).collect { phases ->
                activePhases = phases.sortedBy { it.orderIndex }
            }
        } else {
            activePhases = emptyList()
        }
    }

    fun continueToNextPhase() {
        val nextIndex = phaseIndex + 1
        val mode = activeMode
        if (mode == null || nextIndex >= activePhases.size) {
            sendAction(FocusTimerService.ACTION_STOP)
            focusViewModel.setAwaitingContinue(false)
            focusViewModel.resetPhaseIndex()
            focusViewModel.completeSessionAndLog()
            return
        }
        val phase = activePhases[nextIndex]
        focusViewModel.setCurrentPhaseIndex(nextIndex)
        focusViewModel.setAwaitingContinue(false)
        FocusTimerManager.setMode(
            name = mode.name,
            totalPhaseSeconds = secondsForPhase(phase, flexibleMinutes),
            isRestPhase = phase.type == PhaseType.REST,
        )
        sendAction(FocusTimerService.ACTION_START)
    }

    fun startFromScratch() {
        val mode = activeMode ?: return
        val phase = activePhases.getOrNull(phaseIndex) ?: return
        FocusTimerManager.setMode(
            name = mode.name,
            totalPhaseSeconds = secondsForPhase(phase, flexibleMinutes),
            isRestPhase = phase.type == PhaseType.REST,
        )
        sendAction(FocusTimerService.ACTION_START)
    }

    // --- Target lock check (mirrors FocusProvider.selectedTargetIsLocked) ---
    val targetLocked = remember(selectedTarget, habitsWithCompletions) {
        val target = selectedTarget
        if (target?.type != FocusTargetType.HABIT) {
            false
        } else {
            val hwc = habitsWithCompletions.find { it.habit.id == target.id }
            val stackName = hwc?.habit?.stackName?.trim()
            if (hwc == null || stackName.isNullOrEmpty()) {
                false
            } else {
                val stackHabits = habitsWithCompletions
                    .filter { it.habit.stackName?.trim() == stackName }
                    .sortedBy { it.habit.stackOrder ?: 99 }
                val index = stackHabits.indexOfFirst { it.habit.id == target.id }
                if (index <= 0) {
                    false
                } else {
                    val today = LocalDate.now()
                    val isCurrentDone = HabitUtils.isCompletedOn(stackHabits[index], today)
                    val isPrevDone = HabitUtils.isCompletedOn(stackHabits[index - 1], today)
                    HabitUtils.isHabitLocked(index, isCurrentDone, isPrevDone)
                }
            }
        }
    }

    // --- Gauge display state (mirrors the big `build()` computed-vars block in focus_screen.dart) ---
    val focusLabelDefault = stringResource(R.string.focusLabelDefault).uppercase()
    val focusLabelRest = stringResource(R.string.focusLabelRest).uppercase()
    val focusLabelSetTime = stringResource(R.string.focusLabelSetTime).uppercase()
    val focusLabelStopwatch = stringResource(R.string.focusLabelStopwatch).uppercase()

    val isFlexibleMode = activeMode?.type == FocusModeType.FLEXIBLE
    val canDrag = isFlexibleMode && !isRunning && !isPaused
    val isStopwatchPhaseType = activeMode?.type == FocusModeType.STOPWATCH
    val currentPhase = activePhases.getOrNull(phaseIndex)

    var gaugeProgress = 1f
    var isStopwatchAnim = false
    var gaugeLabel = focusLabelDefault
    var centerText = "25:00"
    var isResting = false

    if (activeMode != null && currentPhase != null) {
        when {
            canDrag -> {
                gaugeProgress = flexibleMinutes / 120f
                gaugeLabel = focusLabelSetTime
                centerText = AppDateFormatUtils.formatDuration(flexibleMinutes * 60)
            }

            !isStopwatchPhaseType -> {
                if (isRunning || isPaused) {
                    val total =
                        if (timerState.totalPhaseSeconds > 0) timerState.totalPhaseSeconds else 1
                    gaugeProgress = (timerState.secondsRemaining.toFloat() / total).coerceIn(0f, 1f)
                    centerText = timerState.centerText
                    isResting = timerState.isRestPhase
                    gaugeLabel = if (timerState.isRestPhase) focusLabelRest else focusLabelDefault
                } else {
                    val totalPhaseSeconds =
                        if (currentPhase.durationMinutes > 0) currentPhase.durationMinutes * 60 else 25 * 60
                    gaugeProgress = 1f
                    centerText = AppDateFormatUtils.formatDuration(totalPhaseSeconds)
                    isResting = currentPhase.type == PhaseType.REST
                    gaugeLabel = if (isResting) focusLabelRest else focusLabelDefault
                }
            }

            else -> {
                isStopwatchAnim = isRunning
                gaugeProgress = 0f
                gaugeLabel = focusLabelStopwatch
                centerText = if (isRunning || isPaused) {
                    timerState.centerText
                } else {
                    "00:00"
                }
            }
        }
    }

    val gaugeColor = if (isResting) WarningColor else FocusColor
    val bottomTextIcon = when (selectedTarget?.type) {
        FocusTargetType.TASK -> Icons.Filled.Task
        FocusTargetType.HABIT -> Icons.Filled.Alarm
        else -> null
    }

    val focusMinsToday = remember(allSessions) { focusViewModel.getMinutesFocusedToday() }

    val activeModeName =
        if (activeMode != null) localizedFocusModeName(activeMode) else stringResource(R.string.focusModeSelect)
    val habitLockedMsg = stringResource(R.string.focusSnackbarHabitLocked)

    fun cycleMode(direction: Int) {
        if (isRunning || isPaused || allModes.isEmpty()) return
        val currentIndex =
            allModes.indexOfFirst { it.id == activeMode?.id }.let { if (it < 0) 0 else it }
        var nextIndex = (currentIndex + direction) % allModes.size
        if (nextIndex < 0) nextIndex += allModes.size
        focusViewModel.setCurrentModeIndex(nextIndex)
    }

    fun requestStop() {
        if (!isRunning && !awaitingContinue) return
        stopWasRunning = isRunning
        if (isRunning) sendAction(FocusTimerService.ACTION_PAUSE)
        showStopConfirmation = true
    }

    fun requestReset() {
        if (!isRunning && !isPaused && !awaitingContinue) return
        resetWasRunning = isRunning
        if (isRunning) sendAction(FocusTimerService.ACTION_PAUSE)
        showResetConfirmation = true
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.focusTitle), fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                actions = {
                    IconButton(onClick = onNavigateToModes) {
                        Icon(Icons.Filled.DashboardCustomize, contentDescription = null)
                    }
                    Spacer(modifier = Modifier.width(AppTheme.spaceSmall))
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // --- FOCUS MODE SELECTOR ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(onClick = { cycleMode(-1) }, enabled = !isRunning && !isPaused) {
                    Icon(
                        Icons.Filled.ArrowBackIosNew,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color.Gray
                    )
                }
                Box(
                    modifier = Modifier
                        .width(180.dp)
                        .clip(AppTheme.brMedium)
                        .clickable(enabled = !isRunning && !isPaused, onClick = onNavigateToModes),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = activeModeName.uppercase(),
                        textAlign = TextAlign.Center,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = AppTheme.lsWide,
                        color = if (isRunning || isPaused) MaterialTheme.colorScheme.onSurfaceVariant else FocusColor,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
                IconButton(onClick = { cycleMode(1) }, enabled = !isRunning && !isPaused) {
                    Icon(
                        Icons.Filled.ArrowForwardIos,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // --- DAILY GOAL BADGE ---
            Box(
                modifier = Modifier
                    .padding(bottom = AppTheme.spaceMedium)
                    .clip(RoundedCornerShape(20.dp))
                    .background(FocusColor.copy(alpha = 0.12f))
                    .border(1.dp, FocusColor.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                    .clickable(onClick = onNavigateToSettings)
                    .padding(horizontal = AppTheme.spaceLarge, vertical = AppTheme.spaceSmall),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.TrackChanges,
                        contentDescription = null,
                        tint = FocusColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(AppTheme.spaceSmall))
                    Text(
                        text = "$focusMinsToday / $dailyGoalMins m",
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = FocusColor,
                    )
                }
            }

            // --- INTERACTIVE TIMER GAUGE ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
                contentAlignment = Alignment.Center
            ) {
                InteractiveGauge(
                    progress = gaugeProgress,
                    isStopwatch = isStopwatchAnim,
                    color = gaugeColor,
                    labelColor = gaugeColor,
                    centerTextColor = gaugeColor,
                    centerText = centerText,
                    bottomText = selectedTarget?.label ?: stringResource(R.string.focusTargetEmpty),
                    bottomTextColor = selectedTarget?.color ?: FocusColor,
                    bottomTextIcon = bottomTextIcon,
                    onBottomTextTapped = if (isRunning || isPaused) null else {
                        { showTargetSelection = true }
                    },
                    isInteractive = canDrag,
                    label = gaugeLabel,
                    onChanged = { newProgress ->
                        if (canDrag) {
                            var newMins = (newProgress * 120).roundToInt()
                            if (newMins < 1) newMins = 1
                            flexibleMinutes = newMins
                        }
                    },
                )

                FilledTonalIconButton(
                    onClick = { showTimeMachine = true },
                    enabled = !isRunning && !isPaused,
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.TopStart),
                ) {
                    Icon(
                        Icons.Filled.History,
                        contentDescription = stringResource(R.string.commonTooltipTimeMachine),
                        modifier = Modifier.size(28.dp)
                    )
                }

                FilledTonalIconButton(
                    onClick = { showDistraction = true },
                    enabled = isRunning || isPaused,
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.TopEnd),
                ) {
                    Icon(
                        Icons.Filled.WarningAmber,
                        contentDescription = stringResource(R.string.commonTooltipDistractions),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // --- PHASE TIMELINE ---
            ModeTimeline(
                phases = activePhases,
                currentPhaseIndex = phaseIndex,
                isRunning = isRunning || isPaused,
                awaitingContinue = awaitingContinue,
            )

            Spacer(modifier = Modifier.weight(1f))

            // --- PLAY/PAUSE CONTROLS ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                val secondaryEnabled = isRunning || isPaused || awaitingContinue
                IconButton(
                    onClick = { requestReset() },
                    modifier = Modifier.size(64.dp),
                    enabled = secondaryEnabled
                ) {
                    Icon(
                        Icons.Filled.RestartAlt,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = if (secondaryEnabled) Color.Gray else Color.Transparent,
                    )
                }

                Spacer(modifier = Modifier.width(32.dp))

                Button(
                    onClick = {
                        when {
                            isRunning -> requestStop()
                            awaitingContinue -> continueToNextPhase()
                            targetLocked -> scope.launch {
                                snackbarHostState.showSnackbar(
                                    habitLockedMsg
                                )
                            }

                            else -> startFromScratch()
                        }
                    },
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRunning) ErrorColor else FocusColor,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(0.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                ) {
                    Icon(
                        when {
                            isRunning -> Icons.Filled.Stop
                            awaitingContinue -> Icons.Filled.FastForward
                            else -> Icons.Filled.PlayArrow
                        },
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                    )
                }

                Spacer(modifier = Modifier.width(32.dp))

                IconButton(
                    onClick = {
                        when {
                            awaitingContinue -> requestStop()
                            isPaused -> startFromScratch()
                            else -> sendAction(FocusTimerService.ACTION_PAUSE)
                        }
                    },
                    modifier = Modifier.size(64.dp),
                    enabled = secondaryEnabled,
                ) {
                    Icon(
                        if (awaitingContinue) Icons.Filled.Stop else if (isPaused) Icons.Filled.PlayCircleFilled else Icons.Filled.Pause,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = if (secondaryEnabled) Color.Gray else Color.Transparent,
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // --- BOTTOM SHEETS & DIALOGS ---
    if (showTargetSelection) {
        TargetSelectorBottomSheet(
            onDismissRequest = { showTargetSelection = false },
            tags = allTags,
            tasks = tasks,
            habitsWithCompletions = habitsWithCompletions,
            selectedType = selectedTarget?.type ?: FocusTargetType.TAG,
            selectedId = selectedTarget?.id,
            onTagSelected = { focusViewModel.setSelectedTag(it) },
            onTaskSelected = {
                focusViewModel.setSelectedTask(
                    it.id,
                    it.title,
                    TaskColor.toArgb()
                )
            },
            onHabitSelected = {
                focusViewModel.setSelectedHabit(
                    it.id,
                    it.name,
                    HabitColor.toArgb()
                )
            },
            onCreateNewTag = { showCreateTagDialog = true },
        )
    }

    if (showDistraction) {
        DistractionBottomSheet(
            onDismissRequest = { showDistraction = false },
            onEventSelected = { type -> focusViewModel.logDistraction(type.entityType) },
        )
    }

    if (showTimeMachine) {
        TimeMachineDialog(
            modes = allModes,
            tags = allTags,
            initialSelectedTagId = selectedTarget?.takeIf { it.type == FocusTargetType.TAG }?.id,
            onDismiss = { showTimeMachine = false },
            onLog = { mode, start, end, tagId ->
                focusViewModel.logPastSession(
                    mode,
                    start,
                    end,
                    allTags.firstOrNull { it.id == tagId })
                showTimeMachine = false
            },
        )
    }

    TextInputDialog(
        visible = showCreateTagDialog,
        title = stringResource(R.string.focusTagsDialogTitleAdd),
        labelText = stringResource(R.string.focusTagsDialogLabelName),
        hintText = stringResource(R.string.focusTagsDialogLabelHint),
        onConfirm = { name ->
            focusViewModel.createTag(name, TaskColor.toArgb())
            showCreateTagDialog = false
        },
        onDismiss = { showCreateTagDialog = false },
    )

    if (showStopConfirmation) {
        val cancelStop = {
            if (stopWasRunning) sendAction(FocusTimerService.ACTION_START)
            showStopConfirmation = false
        }
        AlertDialog(
            onDismissRequest = cancelStop,
            title = { Text(stringResource(R.string.focusDialogSessionStopTitle)) },
            text = { Text(stringResource(R.string.focusDialogSessionStopContent)) },
            confirmButton = {
                Button(onClick = {
                    val elapsed =
                        if (timerState.isStopwatch) timerState.elapsedSeconds else timerState.totalPhaseSeconds - timerState.secondsRemaining
                    focusViewModel.addPartialPhaseTime(elapsed, timerState.isRestPhase)
                    sendAction(io.github.benji377.timety.services.FocusTimerService.ACTION_STOP)
                    focusViewModel.setAwaitingContinue(false)
                    focusViewModel.resetPhaseIndex()
                    focusViewModel.completeSessionAndLog()
                    showStopConfirmation = false
                }) { Text(stringResource(R.string.commonLabelConfirm)) }
            },
            dismissButton = {
                TextButton(onClick = cancelStop) { Text(stringResource(R.string.commonLabelCancel)) }
            },
        )
    }

    if (showResetConfirmation) {
        val cancelReset = {
            if (resetWasRunning) sendAction(FocusTimerService.ACTION_START)
            showResetConfirmation = false
        }
        AlertDialog(
            onDismissRequest = cancelReset,
            title = { Text(stringResource(R.string.focusDialogSessionResetTitle)) },
            text = { Text(stringResource(R.string.focusDialogSessionResetContent)) },
            confirmButton = {
                Button(onClick = {
                    sendAction(FocusTimerService.ACTION_STOP)
                    focusViewModel.setAwaitingContinue(false)
                    focusViewModel.resetPhaseIndex()
                    showResetConfirmation = false
                }) { Text(stringResource(R.string.commonLabelConfirm)) }
            },
            dismissButton = {
                TextButton(onClick = cancelReset) { Text(stringResource(R.string.commonLabelCancel)) }
            },
        )
    }
}
