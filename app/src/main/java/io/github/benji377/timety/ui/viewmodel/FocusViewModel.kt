package io.github.benji377.timety.ui.viewmodel

import androidx.glance.appwidget.updateAll
import androidx.lifecycle.viewModelScope
import io.github.benji377.timety.data.model.focus.DistractionEntity
import io.github.benji377.timety.data.model.focus.DistractionType
import io.github.benji377.timety.data.model.focus.FocusModeEntity
import io.github.benji377.timety.data.model.focus.FocusModeType
import io.github.benji377.timety.data.model.focus.FocusSessionEntity
import io.github.benji377.timety.data.model.focus.FocusTargetSelection
import io.github.benji377.timety.data.model.focus.FocusTargetType
import io.github.benji377.timety.data.model.focus.PhaseType
import io.github.benji377.timety.data.model.focus.SessionPhaseEntity
import io.github.benji377.timety.data.model.habit.HabitCompletionEntity
import io.github.benji377.timety.data.model.habit.HabitWithCompletions
import io.github.benji377.timety.data.model.tag.TaggedItemType
import io.github.benji377.timety.data.repository.FocusRepository
import io.github.benji377.timety.data.repository.HabitRepository
import io.github.benji377.timety.data.repository.RecurringTaskRepository
import io.github.benji377.timety.data.repository.SettingsRepository
import io.github.benji377.timety.data.repository.TagRepository
import io.github.benji377.timety.data.repository.TaskRepository
import io.github.benji377.timety.services.CompletionActions
import io.github.benji377.timety.services.FocusTimerManager
import io.github.benji377.timety.services.ReminderScheduler
import io.github.benji377.timety.util.habit.HabitUtils
import io.github.benji377.timety.util.stats.FocusSessionDayStats
import io.github.benji377.timety.widget.HabitWidget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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

/** Why a focus mode can't be deleted right now. */
enum class ModeDeleteBlocker { HAS_SESSIONS, SESSION_RUNNING }

/**
 * Drives focus mode selection, active session/phase navigation, and session logging, including
 * auto-completion of the linked task, recurring task or habit once a session with a finished
 * focus phase is logged. A discarded session completes nothing.
 */
class FocusViewModel(
    application: android.app.Application,
    private val focusRepository: FocusRepository,
    private val habitRepository: HabitRepository,
    private val taskRepository: TaskRepository,
    private val recurringTaskRepository: RecurringTaskRepository,
    private val settingsRepository: SettingsRepository,
    private val tagRepository: TagRepository
) : androidx.lifecycle.AndroidViewModel(application) {
    // Modes and sessions.
    val allModes: StateFlow<List<FocusModeEntity>> = focusRepository.allModes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSessions: StateFlow<List<FocusSessionEntity>> = focusRepository.allSessions
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

    // Active mode / phase navigation state. The mode is kept by id, so deleting another mode can't
    // shift the selection; null or a deleted id falls back to the first mode.
    private val _currentModeId = MutableStateFlow<String?>(null)
    val currentModeId = _currentModeId.asStateFlow()
    fun setCurrentModeId(id: String) {
        _currentModeId.value = id
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

    fun setSelectedTarget(target: FocusTargetSelection?) {
        _selectedTarget.value = target
    }

    /** The tags a session logged against [target] gets: the tag itself, or a copy of the item's. */
    private suspend fun tagIdsFor(target: FocusTargetSelection?): Set<String> {
        val itemType = when (target?.type) {
            null -> return emptySet()
            FocusTargetType.TAG -> return setOf(target.id)
            FocusTargetType.TASK -> TaggedItemType.TASK
            FocusTargetType.RECURRING_TASK -> TaggedItemType.RECURRING_TASK
            FocusTargetType.HABIT -> TaggedItemType.HABIT
            FocusTargetType.GOAL -> TaggedItemType.GOAL
        }
        return tagRepository.tagsByItem(itemType).first()[target.id].orEmpty().mapTo(HashSet()) { it.id }
    }

    private var currentSessionId: String = UUID.randomUUID().toString()
    fun resetCurrentSession() {
        currentSessionId = UUID.randomUUID().toString()
    }

    init {
        // Seed the three built-in system modes on first run.
        viewModelScope.launch {
            focusRepository.allModes.collect { modes -> ensureSystemModes(modes) }
        }

        // Keeps the selected target pointing at something that exists: a deleted tag, task or habit
        // (also one a restore replaced) clears it, and a renamed tag shows its new name. Left alone
        // mid-session, which logs against the target it began with. Goals aren't checked: a session
        // only records its time against one and never completes it.
        viewModelScope.launch {
            val sessionActive = FocusTimerManager.timerState.map { it.isSessionActive }.distinctUntilChanged()
            combine(
                combine(
                    tagRepository.allTags,
                    taskRepository.allTasks,
                    habitRepository.allHabits,
                    recurringTaskRepository.allRecurringTasks,
                ) { tags, tasks, habits, recurring ->
                    tags to buildMap {
                        put(FocusTargetType.TASK, tasks.mapTo(HashSet()) { it.task.id })
                        put(FocusTargetType.HABIT, habits.mapTo(HashSet()) { it.id })
                        put(FocusTargetType.RECURRING_TASK, recurring.mapTo(HashSet()) { it.task.id })
                    }
                },
                sessionActive,
            ) { (tags, idsByType), active -> Triple(tags, idsByType, active) }
                .collect { (tags, idsByType, active) ->
                    val current = _selectedTarget.value ?: return@collect
                    if (active) return@collect
                    _selectedTarget.value = when (current.type) {
                        FocusTargetType.TAG ->
                            tags.firstOrNull { it.id == current.id }?.let { FocusTargetSelection.tag(it) }
                        FocusTargetType.GOAL -> current
                        else -> current.takeIf { idsByType[current.type]?.contains(current.id) == true }
                    }
                }
        }

        viewModelScope.launch {
            FocusTimerManager.phaseCompleteEvent.collect { (isRestPhase, durationSeconds) ->
                // Auto-complete waits for the session to be logged, so a discarded one completes nothing.
                if (!isRestPhase) {
                    sessionAccumulatedFocusSeconds += durationSeconds
                    sessionFocusPhaseCompleted = true
                }

                if (sessionStartTime == null) {
                    sessionStartTime = Instant.now().minusSeconds(durationSeconds.toLong())
                }

                var isLastPhase = false
                activeSessionModeId?.let { modeId ->
                    val phases = getPhasesForMode(modeId).first()
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
                    completeSessionAndLog(info.startedAt, info.endedAt)
                }
            }
        }
    }

    private var sessionAccumulatedFocusSeconds: Int = 0

    // Auto-complete needs a finished focus phase, not just a session stopped partway.
    private var sessionFocusPhaseCompleted = false
    private var sessionStartTime: Instant? = null
    private val pendingDistractions = mutableListOf<DistractionEntity>()

    // Mode the running session was started with, captured at start time. Reading
    // currentModeId/allModes at stop time is unreliable (the stop can arrive from the
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

    /**
     * Logs the session spanning [startedAt] to [endedAt], as reported by the timer. Without them it
     * falls back to a span ending now, derived from the banked focus time.
     */
    fun completeSessionAndLog(startedAt: Instant? = null, endedAt: Instant? = null) {
        val modeId = activeSessionModeId
        // Sessions under a minute round down to "0m" everywhere in the UI and only add noise
        // to the stats and calendar, so they are dropped instead of logged.
        if (sessionAccumulatedFocusSeconds >= 60 && modeId != null) {
            val sessionToLog = FocusSessionEntity(
                id = currentSessionId,
                modeId = modeId,
                startTime = startedAt ?: sessionStartTime ?: Instant.now()
                    .minusSeconds(sessionAccumulatedFocusSeconds.toLong()),
                endTime = endedAt ?: Instant.now(),
                totalSecondsFocused = sessionAccumulatedFocusSeconds,
                isCompleted = true,
            ).withTarget(selectedTarget.value)
            val targetToLog = selectedTarget.value
            val distractionsToLog = pendingDistractions.toList()
            val targetToComplete = selectedTarget.value?.takeIf { sessionFocusPhaseCompleted }

            viewModelScope.launch {
                focusRepository.insertSession(sessionToLog)
                tagRepository.setItemTags(TaggedItemType.SESSION, sessionToLog.id, tagIdsFor(targetToLog))
                distractionsToLog.forEach { focusRepository.insertDistraction(it) }
                if (targetToComplete != null && settingsRepository.autoCompleteFocusFlow.first()) {
                    autoCompleteTarget(targetToComplete)
                }
            }
        }
        resetSessionState()
    }

    fun discardSession() = resetSessionState()

    private fun resetSessionState() {
        pendingDistractions.clear()
        sessionAccumulatedFocusSeconds = 0
        sessionFocusPhaseCompleted = false
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
                getApplication(), taskRepository, target.id
            )

            FocusTargetType.RECURRING_TASK -> {
                val entry = recurringTaskRepository.observeTask(target.id).first() ?: return
                // Once per day, like a habit: a second session must not log another occurrence.
                val today = LocalDate.now()
                val zone = ZoneId.systemDefault()
                if (entry.occurrences.any { it.completedAt.atZone(zone).toLocalDate() == today }) return
                val task = entry.task
                CompletionActions.completeRecurringOccurrence(
                    getApplication(), recurringTaskRepository, task
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
        }
        ReminderScheduler.refreshHabitReminder(getApplication(), habitId)
        // Auto-completion bypasses HabitViewModel, so refresh the habit widget here too.
        HabitWidget().updateAll(getApplication())
    }

    private suspend fun ensureSystemModes(modes: List<FocusModeEntity>) {
        val existing = modes.mapTo(HashSet()) { it.id }
        for ((mode, phases) in SYSTEM_MODES) {
            if (mode.id in existing) continue
            focusRepository.insertModeWithPhases(
                mode,
                phases.mapIndexed { index, (type, minutes) ->
                    SessionPhaseEntity(modeId = mode.id, type = type, durationMinutes = minutes, orderIndex = index)
                },
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

    fun deleteMode(mode: FocusModeEntity, onBlocked: (ModeDeleteBlocker) -> Unit = {}) {
        if (mode.isSystem) return
        // The running session is logged against its mode when it ends.
        if (mode.id == activeSessionModeId) {
            onBlocked(ModeDeleteBlocker.SESSION_RUNNING)
            return
        }
        viewModelScope.launch {
            // focus_sessions references modes with a RESTRICT foreign key, so deleting a mode
            // that has logged sessions would throw. Keep the mode and tell the user instead.
            if (focusRepository.modeHasSessions(mode.id)) {
                onBlocked(ModeDeleteBlocker.HAS_SESSIONS)
            } else {
                focusRepository.deleteMode(mode)
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
     * Logs a manually backfilled session for a time range that already elapsed. Never
     * auto-completes [target]: backfilling only records time that was already spent.
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
     * Logs a backfilled session attributed to the given task with a copy of its tags, used when the
     * user attaches the time they spent while checking the task off.
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

        val session = FocusSessionEntity(
            id = UUID.randomUUID().toString(),
            modeId = mode.id,
            startTime = startTime,
            endTime = endTime,
            totalSecondsFocused = totalSeconds.toInt(),
            isCompleted = true,
        ).withTarget(target)
        viewModelScope.launch {
            focusRepository.insertSession(session)
            tagRepository.setItemTags(TaggedItemType.SESSION, session.id, tagIdsFor(target))
        }
    }

    /**
     * Applies an edit to an already logged session: its focused time is re-derived from the new
     * range. A session keeps the tags it was logged with until it is retargeted, so retagging an
     * item later doesn't rewrite its history. Retargeting doesn't undo or redo any auto-completion the session triggered when it
     * was recorded.
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
            val sameItem = target != null && target.type != FocusTargetType.TAG &&
                target.type == session.targetType && target.id == session.targetId
            if (!sameItem) tagRepository.setItemTags(TaggedItemType.SESSION, updated.id, tagIdsFor(target))
        }
    }

    /** Deletes a logged session (and its distractions, by cascade). */
    fun deleteSession(session: FocusSessionEntity) {
        viewModelScope.launch {
            focusRepository.deleteSession(session)
        }
    }

    // A tag target lives in session_tags alone, so a rename can't leave a stale label behind.
    private fun FocusSessionEntity.withTarget(target: FocusTargetSelection?): FocusSessionEntity {
        val item = target?.takeIf { it.type != FocusTargetType.TAG }
        return copy(
            targetType = item?.type ?: FocusTargetType.TAG,
            targetId = item?.id,
            targetLabel = item?.label,
        )
    }
}

/** Built-in modes with their phases as (type, minutes); 0 minutes is open-ended, -1 is user-set. */
private val SYSTEM_MODES = listOf(
    FocusModeEntity(
        id = FocusModeEntity.SYSTEM_STOPWATCH_ID,
        name = "Stopwatch",
        type = FocusModeType.STOPWATCH,
        isSystem = true,
    ) to listOf(PhaseType.FOCUS to 0),
    FocusModeEntity(
        id = FocusModeEntity.SYSTEM_FLEXIBLE_ID,
        name = "Flexible",
        type = FocusModeType.FLEXIBLE,
        isSystem = true,
    ) to listOf(PhaseType.FOCUS to -1),
    // Four 25-minute focus blocks; the last break is the long one.
    FocusModeEntity(
        id = FocusModeEntity.SYSTEM_POMODORO_ID,
        name = "Pomodoro Classic",
        type = FocusModeType.POMODORO,
        isSystem = true,
    ) to (1..4).flatMap { block -> listOf(PhaseType.FOCUS to 25, PhaseType.REST to if (block == 4) 15 else 5) },
)
