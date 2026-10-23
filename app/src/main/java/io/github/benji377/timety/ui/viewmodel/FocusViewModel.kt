package io.github.benji377.timety.ui.viewmodel

import androidx.compose.ui.graphics.toArgb
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.viewModelScope
import io.github.benji377.timety.data.model.focus.DistractionEntity
import io.github.benji377.timety.data.model.focus.DistractionType
import io.github.benji377.timety.data.model.focus.FocusModeEntity
import io.github.benji377.timety.data.model.focus.FocusModeType
import io.github.benji377.timety.data.model.focus.FocusSessionEntity
import io.github.benji377.timety.data.model.focus.FocusTagEntity
import io.github.benji377.timety.data.model.focus.FocusTargetSelection
import io.github.benji377.timety.data.model.focus.FocusTargetType
import io.github.benji377.timety.data.model.focus.PhaseType
import io.github.benji377.timety.data.model.focus.SessionPhaseEntity
import io.github.benji377.timety.data.model.habit.HabitCompletionEntity
import io.github.benji377.timety.data.model.habit.HabitWithCompletions
import io.github.benji377.timety.data.repository.FocusRepository
import io.github.benji377.timety.data.repository.HabitRepository
import io.github.benji377.timety.data.repository.RecurringTaskRepository
import io.github.benji377.timety.data.repository.SettingsRepository
import io.github.benji377.timety.data.repository.TaskRepository
import io.github.benji377.timety.data.repository.UserRepository
import io.github.benji377.timety.services.CompletionActions
import io.github.benji377.timety.services.FocusTimerManager
import io.github.benji377.timety.services.ReminderScheduler
import io.github.benji377.timety.ui.theme.FocusColor
import io.github.benji377.timety.util.habit.HabitUtils
import io.github.benji377.timety.util.stats.ExperienceEngine
import io.github.benji377.timety.util.stats.FocusSessionDayStats
import io.github.benji377.timety.widget.HabitWidget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID


/** A logged distraction paired with the focus session it was recorded against. */
data class DistractionWithSession(
    val distraction: DistractionEntity,
    val session: FocusSessionEntity
)

/**
 * Drives focus mode selection, active session/phase navigation, and session logging, including
 * XP awards and auto-completion of the linked task, recurring task or habit when a focus phase
 * finishes.
 */
class FocusViewModel(
    application: android.app.Application,
    private val focusRepository: FocusRepository,
    private val userRepository: UserRepository,
    private val habitRepository: HabitRepository,
    private val taskRepository: TaskRepository,
    private val recurringTaskRepository: RecurringTaskRepository,
    private val settingsRepository: SettingsRepository
) : androidx.lifecycle.AndroidViewModel(application) {

    // Modes, sessions, and tags.
    val allModes: StateFlow<List<FocusModeEntity>> = focusRepository.allModes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSessions: StateFlow<List<FocusSessionEntity>> = focusRepository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTags: StateFlow<List<FocusTagEntity>> = focusRepository.allTags
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    // A single live query over the distractions table joined in memory with the sessions list.
    // Joining per session instead would open one Room flow PER session — hundreds of concurrent
    // live queries once real usage history accumulates.
    val allDistractions: StateFlow<List<DistractionWithSession>> = combine(
        focusRepository.allSessions,
        focusRepository.allDistractions
    ) { sessions, distractions ->
        val sessionById = sessions.associateBy { it.id }
        distractions.mapNotNull { d ->
            sessionById[d.sessionId]?.let { DistractionWithSession(d, it) }
        }
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active mode / phase navigation state.
    private val _currentModeIndex = MutableStateFlow(0)
    val currentModeIndex = _currentModeIndex.asStateFlow()
    fun setCurrentModeIndex(index: Int) {
        _currentModeIndex.value = index
    }


    private val _currentPhaseIndex = MutableStateFlow(0)
    val currentPhaseIndex = _currentPhaseIndex.asStateFlow()
    fun setCurrentPhaseIndex(index: Int) {
        _currentPhaseIndex.value = index
    }

    fun resetPhaseIndex() {
        _currentPhaseIndex.value = 0
    }


    private val _awaitingContinue = MutableStateFlow(false)
    val awaitingContinue: StateFlow<Boolean> = _awaitingContinue.asStateFlow()
    fun setAwaitingContinue(awaiting: Boolean) {
        _awaitingContinue.value = awaiting
    }

    private val _selectedTarget = MutableStateFlow<FocusTargetSelection?>(null)
    val selectedTarget: StateFlow<FocusTargetSelection?> = _selectedTarget.asStateFlow()

    fun setSelectedTarget(target: FocusTargetSelection) {
        _selectedTarget.value = target
    }

    fun clearSelectedTarget() {
        val defaultTag = allTags.value.firstOrNull()
        _selectedTarget.value = defaultTag?.let { FocusTargetSelection.tag(it) }
    }

    private var currentSessionId: String = UUID.randomUUID().toString()
    fun resetCurrentSession() {
        currentSessionId = UUID.randomUUID().toString()
    }

    init {
        // Seed the default tag and the three built-in system modes on first run, and default the
        // selected target to the first tag. This collects the *raw* repository flows (not the
        // `stateIn`-wrapped `allTags`/`allModes` above) so the check only sees genuine Room
        // emissions - `stateIn`'s synthetic `emptyList()` initial value would otherwise look like
        // "no tags yet" on every launch and stomp a user-renamed default tag.
        viewModelScope.launch {
            focusRepository.allTags.collect { tags ->
                if (tags.isEmpty()) {
                    focusRepository.insertTag(
                        FocusTagEntity(
                            id = "default_tag",
                            name = "None",
                            colorValue = FocusColor.toArgb()
                        )
                    )
                } else if (_selectedTarget.value == null) {
                    _selectedTarget.value = FocusTargetSelection.tag(tags.first())
                }
            }
        }
        viewModelScope.launch {
            focusRepository.allModes.collect { modes -> ensureSystemModes(modes) }
        }

        viewModelScope.launch {
            FocusTimerManager.phaseCompleteEvent.collect { (isRestPhase, durationSeconds) ->
                val target = selectedTarget.value

                if (!isRestPhase) {
                    val durationMinutes = durationSeconds / 60
                    if (durationMinutes > 0) {
                        userRepository.addXp(durationMinutes * ExperienceEngine.XP_PER_FOCUS_MINS)
                        sessionXpAwardedMinutes += durationMinutes
                    }
                    sessionAccumulatedFocusSeconds += durationSeconds

                    // Once per session: a multi-phase session would otherwise complete a
                    // recurring task again after every focus phase, skipping occurrences.
                    if (target != null && !sessionTargetAutoCompleted &&
                        settingsRepository.autoCompleteFocusFlow.first()
                    ) {
                        sessionTargetAutoCompleted = true
                        autoCompleteTarget(target)
                    }
                }

                if (sessionStartTime == null) {
                    sessionStartTime = Instant.now().minusSeconds(durationSeconds.toLong())
                }

                val currentMode = allModes.value.getOrNull(currentModeIndex.value)
                var isLastPhase = false
                if (currentMode != null) {
                    val phases = getPhasesForMode(currentMode.id).first()
                    if (currentPhaseIndex.value + 1 >= phases.size) {
                        isLastPhase = true
                    }
                }

                // We rely on the AlarmManager to natively play the sound.
                // The FocusTimerService will not cancel the alarm if the phase naturally completes,
                // so the sound will correctly play without double dings.

                if (isLastPhase) {
                    FocusTimerManager.stopTimer(discard = false)
                } else {
                    setAwaitingContinue(true)
                }
            }
        }

        // Single bookkeeping path for every way the timer can stop (in-app stop dialog, in-app
        // reset, or the notification's Stop action while the app is backgrounded): bank the
        // partial phase, then either log the session or discard it, and reset the phase cursor.
        viewModelScope.launch {
            FocusTimerManager.stopEvent.collect { info ->
                setAwaitingContinue(false)
                resetPhaseIndex()
                if (info.discard) {
                    discardSession()
                } else {
                    addPartialPhaseTime(info.elapsedFocusSeconds, info.wasRestPhase)
                    completeSessionAndLog()
                }
            }
        }
    }

    private var sessionAccumulatedFocusSeconds: Int = 0
    private var sessionTargetAutoCompleted = false

    // Focus minutes already converted to XP by completed phases, so the remainder (stopwatch
    // time, a phase stopped midway) can be awarded at log time without double-paying.
    private var sessionXpAwardedMinutes: Int = 0
    private var sessionStartTime: Instant? = null
    private val pendingDistractions = mutableListOf<DistractionEntity>()

    // Mode the running session was started with, captured at start time. Reading
    // currentModeIndex/allModes at stop time is unreliable (the stop can arrive from the
    // notification while the mode list flow is not being collected), and inserting a session
    // with a made-up mode id violates the focus_sessions foreign key and crashes the app.
    private var activeSessionModeId: String? = null

    fun setActiveSessionMode(modeId: String) {
        activeSessionModeId = modeId
    }

    fun addPartialPhaseTime(seconds: Int, isRestPhase: Boolean) {
        if (!isRestPhase && seconds > 0) {
            sessionAccumulatedFocusSeconds += seconds
            if (sessionStartTime == null) {
                sessionStartTime = Instant.now().minusSeconds(seconds.toLong())
            }
        }
    }

    fun completeSessionAndLog() {
        val modeId = activeSessionModeId ?: allModes.value.getOrNull(currentModeIndex.value)?.id
        // Sessions under a minute round down to "0m" everywhere in the UI and only add noise
        // to the stats and calendar, so they are dropped instead of logged.
        if (sessionAccumulatedFocusSeconds >= 60 && modeId != null) {
            val sessionToLog = FocusSessionEntity(
                id = currentSessionId,
                modeId = modeId,
                startTime = sessionStartTime ?: Instant.now()
                    .minusSeconds(sessionAccumulatedFocusSeconds.toLong()),
                endTime = Instant.now(),
                totalSecondsFocused = sessionAccumulatedFocusSeconds,
                isCompleted = true,
            ).withTarget(selectedTarget.value)
            val distractionsToLog = pendingDistractions.toList()
            // Stopwatch time and a phase stopped midway never pass through phaseCompleteEvent,
            // so their focus minutes would otherwise earn none of the advertised per-minute XP.
            val unawardedMinutes =
                sessionAccumulatedFocusSeconds / 60 - sessionXpAwardedMinutes

            viewModelScope.launch {
                focusRepository.insertSession(sessionToLog)
                distractionsToLog.forEach { focusRepository.insertDistraction(it) }
                if (unawardedMinutes > 0) {
                    userRepository.addXp(unawardedMinutes * ExperienceEngine.XP_PER_FOCUS_MINS)
                }
            }
        }
        resetSessionState()
    }

    fun discardSession() = resetSessionState()

    private fun resetSessionState() {
        pendingDistractions.clear()
        sessionAccumulatedFocusSeconds = 0
        sessionXpAwardedMinutes = 0
        sessionTargetAutoCompleted = false
        sessionStartTime = null
        activeSessionModeId = null
        resetCurrentSession()
    }

    /**
     * Completes the session's target when the auto-complete setting is on. Runs here rather than
     * in a screen so it also happens while the focus screen isn't shown. Goals are never
     * completed: a session only counts time toward them, and their progress units aren't time.
     */
    private suspend fun autoCompleteTarget(target: FocusTargetSelection) {
        when (target.type) {
            FocusTargetType.TASK -> CompletionActions.completeTask(
                getApplication(), taskRepository, userRepository, target.id
            )

            FocusTargetType.RECURRING_TASK -> {
                val entry = recurringTaskRepository.observeTask(target.id).first() ?: return
                // Once per day, like a habit: a second session must not log another occurrence.
                val today = LocalDate.now()
                val zone = ZoneId.systemDefault()
                if (entry.occurrences.any { it.completedAt.atZone(zone).toLocalDate() == today }) return
                val task = entry.task
                CompletionActions.completeRecurringOccurrence(
                    getApplication(), recurringTaskRepository, userRepository, task
                )
            }

            FocusTargetType.HABIT -> completeHabitToday(target.id)
            FocusTargetType.TAG, FocusTargetType.GOAL -> {}
        }
    }

    private suspend fun completeHabitToday(habitId: String) {
        val habit = habitRepository.getHabitById(habitId) ?: return
        // Shares the completion lock with the in-app and widget toggles, which read-check-write
        // the same row; unsynchronized, a concurrent tap could add a duplicate completion.
        habitRepository.completionMutex.withLock {
            val completions = habitRepository.getCompletionsForHabit(habitId).first()
            if (HabitUtils.isCompletedOn(HabitWithCompletions(habit, completions), LocalDate.now())) {
                return
            }
            habitRepository.insertCompletion(
                HabitCompletionEntity(habitId = habitId, completionDate = Instant.now())
            )
            userRepository.addXp(ExperienceEngine.XP_PER_HABIT)
        }
        ReminderScheduler.create(getApplication()).dismissHabitNotifications(habitId)
        // Auto-completion bypasses HabitViewModel, so refresh the habit widget here too.
        HabitWidget().updateAll(getApplication())
    }

    private suspend fun ensureSystemModes(modes: List<FocusModeEntity>) {
        if (modes.none { it.id == FocusModeEntity.SYSTEM_STOPWATCH_ID }) {
            focusRepository.insertModeWithPhases(
                FocusModeEntity(
                    id = FocusModeEntity.SYSTEM_STOPWATCH_ID,
                    name = "Stopwatch",
                    type = FocusModeType.STOPWATCH,
                    isSystem = true
                ),
                listOf(
                    SessionPhaseEntity(
                        modeId = FocusModeEntity.SYSTEM_STOPWATCH_ID,
                        type = PhaseType.FOCUS,
                        durationMinutes = 0,
                        orderIndex = 0
                    )
                ),
            )
        }
        if (modes.none { it.id == FocusModeEntity.SYSTEM_FLEXIBLE_ID }) {
            focusRepository.insertModeWithPhases(
                FocusModeEntity(
                    id = FocusModeEntity.SYSTEM_FLEXIBLE_ID,
                    name = "Flexible",
                    type = FocusModeType.FLEXIBLE,
                    isSystem = true
                ),
                listOf(
                    SessionPhaseEntity(
                        modeId = FocusModeEntity.SYSTEM_FLEXIBLE_ID,
                        type = PhaseType.FOCUS,
                        durationMinutes = -1,
                        orderIndex = 0
                    )
                ),
            )
        }
        if (modes.none { it.id == FocusModeEntity.SYSTEM_POMODORO_ID }) {
            focusRepository.insertModeWithPhases(
                FocusModeEntity(
                    id = FocusModeEntity.SYSTEM_POMODORO_ID,
                    name = "Pomodoro Classic",
                    type = FocusModeType.POMODORO,
                    isSystem = true
                ),
                listOf(
                    SessionPhaseEntity(
                        modeId = FocusModeEntity.SYSTEM_POMODORO_ID,
                        type = PhaseType.FOCUS,
                        durationMinutes = 25,
                        orderIndex = 0
                    ),
                    SessionPhaseEntity(
                        modeId = FocusModeEntity.SYSTEM_POMODORO_ID,
                        type = PhaseType.REST,
                        durationMinutes = 5,
                        orderIndex = 1
                    ),
                    SessionPhaseEntity(
                        modeId = FocusModeEntity.SYSTEM_POMODORO_ID,
                        type = PhaseType.FOCUS,
                        durationMinutes = 25,
                        orderIndex = 2
                    ),
                    SessionPhaseEntity(
                        modeId = FocusModeEntity.SYSTEM_POMODORO_ID,
                        type = PhaseType.REST,
                        durationMinutes = 5,
                        orderIndex = 3
                    ),
                    SessionPhaseEntity(
                        modeId = FocusModeEntity.SYSTEM_POMODORO_ID,
                        type = PhaseType.FOCUS,
                        durationMinutes = 25,
                        orderIndex = 4
                    ),
                    SessionPhaseEntity(
                        modeId = FocusModeEntity.SYSTEM_POMODORO_ID,
                        type = PhaseType.REST,
                        durationMinutes = 5,
                        orderIndex = 5
                    ),
                    SessionPhaseEntity(
                        modeId = FocusModeEntity.SYSTEM_POMODORO_ID,
                        type = PhaseType.FOCUS,
                        durationMinutes = 25,
                        orderIndex = 6
                    ),
                    SessionPhaseEntity(
                        modeId = FocusModeEntity.SYSTEM_POMODORO_ID,
                        type = PhaseType.REST,
                        durationMinutes = 15,
                        orderIndex = 7
                    ),
                ),
            )
        }
    }

    fun logDistraction(
        type: DistractionType,
        note: String = ""
    ) {
        pendingDistractions.add(
            DistractionEntity(
                sessionId = currentSessionId,
                time = Instant.now(),
                type = type,
                note = note
            )
        )
    }

    fun getPhasesForMode(modeId: String) = focusRepository.getPhasesForMode(modeId)

    fun saveMode(mode: FocusModeEntity, phases: List<SessionPhaseEntity>) {
        viewModelScope.launch { focusRepository.insertModeWithPhases(mode, phases) }
    }

    fun deleteMode(mode: FocusModeEntity, onBlocked: () -> Unit = {}) {
        if (mode.isSystem) return
        viewModelScope.launch {
            // focus_sessions references modes with a RESTRICT foreign key, so deleting a mode
            // that has logged sessions would throw. Keep the mode and tell the user instead.
            if (focusRepository.modeHasSessions(mode.id)) {
                onBlocked()
            } else {
                focusRepository.deleteMode(mode)
            }
        }
    }

    // Tags.
    fun createTag(name: String, colorValue: Int) {
        viewModelScope.launch {
            val tag = FocusTagEntity(
                id = UUID.randomUUID().toString(),
                name = name,
                colorValue = colorValue
            )
            focusRepository.insertTag(tag)
            _selectedTarget.value = FocusTargetSelection.tag(tag)
        }
    }

    fun updateTag(id: String, name: String, colorValue: Int) {
        viewModelScope.launch {
            focusRepository.insertTag(FocusTagEntity(id = id, name = name, colorValue = colorValue))
        }
    }

    fun deleteTag(tag: FocusTagEntity) {
        viewModelScope.launch {
            focusRepository.deleteTag(tag)
            if (_selectedTarget.value?.type == FocusTargetType.TAG && _selectedTarget.value?.id == tag.id) {
                clearSelectedTarget()
            }
        }
    }

    // Stats helpers.

    fun getMinutesFocusedOnDay(day: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Int =
        // Counts only the minutes that fell on [day], so a session crossing midnight is credited to
        // each day it touched instead of dumping its whole length onto the day it started.
        allSessions.value.sumOf { FocusSessionDayStats.minutesByDay(it, zone)[day] ?: 0 }

    fun getMinutesFocusedToday(zone: ZoneId = ZoneId.systemDefault()): Int =
        getMinutesFocusedOnDay(LocalDate.now(), zone)


    /**
     * Logs a manually backfilled session for a time range that already elapsed, and awards its
     * XP. Never auto-completes [target]: backfilling only records time that was already spent.
     */
    fun logPastSession(
        mode: FocusModeEntity,
        startTime: Instant,
        endTime: Instant,
        target: FocusTargetSelection?
    ) {
        logBackfilledSession(mode, startTime, endTime, target)
    }

    /**
     * Logs a backfilled session attributed to the given task, used when the user attaches the time
     * they spent while checking the task off.
     */
    fun logSessionForTask(
        mode: FocusModeEntity,
        startTime: Instant,
        endTime: Instant,
        taskId: String,
        taskTitle: String
    ) {
        logBackfilledSession(
            mode,
            startTime,
            endTime,
            FocusTargetSelection.of(FocusTargetType.TASK, taskId, taskTitle),
        )
    }

    private fun logBackfilledSession(
        mode: FocusModeEntity,
        startTime: Instant,
        endTime: Instant,
        target: FocusTargetSelection?
    ) {
        val totalSeconds = endTime.epochSecond - startTime.epochSecond
        if (totalSeconds <= 0) return

        viewModelScope.launch {
            focusRepository.insertSession(
                FocusSessionEntity(
                    id = UUID.randomUUID().toString(),
                    modeId = mode.id,
                    startTime = startTime,
                    endTime = endTime,
                    totalSecondsFocused = totalSeconds.toInt(),
                    isCompleted = true,
                ).withTarget(target)
            )
            awardFocusXp((totalSeconds / 60).toInt())
        }
    }

    /**
     * Applies an edit to an already logged session: its focused time is re-derived from the new
     * range and the XP it earned is corrected by the difference. Retargeting doesn't undo or
     * redo any auto-completion the session triggered when it was recorded.
     */
    fun updateSession(
        session: FocusSessionEntity,
        mode: FocusModeEntity,
        startTime: Instant,
        endTime: Instant,
        target: FocusTargetSelection?
    ) {
        val span = (endTime.epochSecond - startTime.epochSecond).toInt()
        if (span <= 0) return
        // The editor's span includes breaks; keep the real focus time unless the times changed.
        val storedEnd = session.endTime
        val timesUnchanged = startTime == session.startTime && storedEnd != null &&
            Duration.between(session.startTime, storedEnd).toMinutes() ==
            Duration.between(startTime, endTime).toMinutes()
        val totalSeconds = if (timesUnchanged) session.totalSecondsFocused else span

        val updated = session.copy(
            modeId = mode.id,
            startTime = startTime,
            endTime = if (timesUnchanged) storedEnd else endTime,
            totalSecondsFocused = totalSeconds,
        ).withTarget(target)

        viewModelScope.launch {
            focusRepository.updateSession(updated)
            awardFocusXp(totalSeconds / 60 - session.totalSecondsFocused / 60)
        }
    }

    /** Deletes a logged session (and its distractions, by cascade) and takes back the XP it earned. */
    fun deleteSession(session: FocusSessionEntity) {
        viewModelScope.launch {
            focusRepository.deleteSession(session)
            awardFocusXp(-(session.totalSecondsFocused / 60))
        }
    }

    // tagId mirrors targetId for tag targets only; the stats tag filter and the calendar read it.
    private fun FocusSessionEntity.withTarget(target: FocusTargetSelection?) = copy(
        tagId = target?.takeIf { it.type == FocusTargetType.TAG }?.id,
        targetType = target?.type ?: FocusTargetType.TAG,
        targetId = target?.id,
        targetLabel = target?.label,
    )

    // Focus XP is paid per whole minute, so a session's XP can be handed out and taken back
    // symmetrically: editing or deleting one reverses exactly what logging it awarded.
    private suspend fun awardFocusXp(minutes: Int) {
        if (minutes != 0) {
            userRepository.addXp(minutes * ExperienceEngine.XP_PER_FOCUS_MINS)
        }
    }
}
