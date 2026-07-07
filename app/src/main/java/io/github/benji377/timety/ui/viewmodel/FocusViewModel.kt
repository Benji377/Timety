package io.github.benji377.timety.ui.viewmodel

import androidx.compose.ui.graphics.toArgb
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.viewModelScope
import io.github.benji377.timety.data.model.focus.DistractionEntity
import io.github.benji377.timety.data.model.focus.FocusModeEntity
import io.github.benji377.timety.data.model.focus.FocusModeType
import io.github.benji377.timety.data.model.focus.FocusSessionEntity
import io.github.benji377.timety.data.model.focus.FocusTagEntity
import io.github.benji377.timety.data.model.focus.FocusTargetSelection
import io.github.benji377.timety.data.model.focus.FocusTargetType
import io.github.benji377.timety.data.model.focus.PhaseType
import io.github.benji377.timety.data.model.focus.SessionPhaseEntity
import io.github.benji377.timety.data.repository.FocusRepository
import io.github.benji377.timety.data.repository.UserRepository
import io.github.benji377.timety.services.FocusTimerManager
import io.github.benji377.timety.ui.theme.FocusColor
import io.github.benji377.timety.util.stats.ExperienceEngine
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import io.github.benji377.timety.data.model.focus.DistractionType
import io.github.benji377.timety.data.model.habit.HabitCompletionEntity
import io.github.benji377.timety.data.model.habit.HabitWithCompletions
import io.github.benji377.timety.data.repository.HabitRepository
import io.github.benji377.timety.data.repository.SettingsRepository
import io.github.benji377.timety.util.habit.HabitUtils
import io.github.benji377.timety.widget.HabitWidget


data class DistractionWithSession(
    val distraction: DistractionEntity,
    val session: FocusSessionEntity
)

class FocusViewModel(
    application: android.app.Application,
    private val focusRepository: FocusRepository,
    private val userRepository: UserRepository,
    private val habitRepository: HabitRepository,
    private val settingsRepository: SettingsRepository
) : androidx.lifecycle.AndroidViewModel(application) {

    // --- MODES / SESSIONS / TAGS (existing) ---
    val allModes: StateFlow<List<FocusModeEntity>> = focusRepository.allModes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSessions: StateFlow<List<FocusSessionEntity>> = focusRepository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTags: StateFlow<List<FocusTagEntity>> = focusRepository.allTags
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    // A single live query over the distractions table joined in memory with the sessions list.
    // The previous flatMapLatest+combine version opened one Room flow PER session, which meant
    // hundreds of concurrent live queries once real usage history accumulated.
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

    // --- ACTIVE MODE / PHASE NAVIGATION ---
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

    private val _autoCompleteTaskEvent = MutableSharedFlow<String>()
    val autoCompleteTaskEvent = _autoCompleteTaskEvent.asSharedFlow()

    // --- SESSION TARGET (tag / task / habit) ---
    private val _selectedTarget = MutableStateFlow<FocusTargetSelection?>(null)
    val selectedTarget: StateFlow<FocusTargetSelection?> = _selectedTarget.asStateFlow()

    fun setSelectedTag(tag: FocusTagEntity) {
        _selectedTarget.value = FocusTargetSelection.tag(tag)
    }

    fun setSelectedTask(id: String, label: String, colorValue: Int) {
        _selectedTarget.value = FocusTargetSelection.task(id, label, colorValue)
    }

    fun setSelectedHabit(id: String, label: String, colorValue: Int) {
        _selectedTarget.value = FocusTargetSelection.habit(id, label, colorValue)
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
        // Seed the default tag + the three built-in system modes on first run, and default the
        // selected target to the first tag. Mirrors `FocusProvider.loadFocusData`.
        // NOTE (viewmodel addition - see report): nothing seeded these before; without this the
        // app launched with zero focus modes and no usable target. This collects the *raw*
        // repository flows (not the `stateIn`-wrapped `allTags`/`allModes` above) so the check
        // only sees genuine Room emissions - `stateIn`'s synthetic `emptyList()` initial value
        // would otherwise look like "no tags yet" on every launch and stomp a user-renamed
        // default tag.
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
                    }
                    sessionAccumulatedFocusSeconds += durationSeconds

                    // --- Auto-completion logic ---
                    val autoCompleteEnabled = settingsRepository.autoCompleteFocusFlow.first()
                    if (autoCompleteEnabled && target != null) {
                        when (target.type) {
                            FocusTargetType.TASK -> {
                                _autoCompleteTaskEvent.emit(target.id)
                            }

                            FocusTargetType.HABIT -> {
                                val today = LocalDate.now()
                                val hcEntity = habitRepository.getHabitById(target.id)
                                if (hcEntity != null) {
                                    val completions =
                                        habitRepository.getCompletionsForHabit(target.id).first()
                                    val hwc =
                                        HabitWithCompletions(
                                            hcEntity,
                                            completions
                                        )
                                    if (!HabitUtils.isCompletedOn(
                                            hwc,
                                            today
                                        )
                                    ) {
                                        habitRepository.insertCompletion(
                                            HabitCompletionEntity(
                                                habitId = hcEntity.id,
                                                completionDate = Instant.now()
                                            )
                                        )
                                        userRepository.addXp(ExperienceEngine.XP_PER_HABIT)
                                        // Auto-completion bypasses HabitViewModel, so refresh
                                        // the habit widget here too.
                                        HabitWidget()
                                            .updateAll(getApplication())
                                    }
                                }
                            }

                            else -> {}
                        }
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
            val target = selectedTarget.value
            val sessionToLog = FocusSessionEntity(
                id = currentSessionId,
                modeId = modeId,
                startTime = sessionStartTime ?: Instant.now()
                    .minusSeconds(sessionAccumulatedFocusSeconds.toLong()),
                endTime = Instant.now(),
                totalSecondsFocused = sessionAccumulatedFocusSeconds,
                isCompleted = true,
                tagId = if (target?.type == FocusTargetType.TAG) target.id else null,
                targetType = target?.type ?: FocusTargetType.TAG,
                targetId = target?.id,
                targetLabel = target?.label,
            )
            val distractionsToLog = pendingDistractions.toList()
            
            viewModelScope.launch {
                focusRepository.insertSession(sessionToLog)
                distractionsToLog.forEach { focusRepository.insertDistraction(it) }
            }
        }
        pendingDistractions.clear()
        sessionAccumulatedFocusSeconds = 0
        sessionStartTime = null
        activeSessionModeId = null
        resetCurrentSession()
    }


    fun discardSession() {
        pendingDistractions.clear()
        sessionAccumulatedFocusSeconds = 0
        sessionStartTime = null
        activeSessionModeId = null
        resetCurrentSession()
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

    // --- TAGS ---
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

    // --- STATS HELPERS ---


    fun getMinutesFocusedOnDay(day: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Int {
        val totalSeconds = allSessions.value
            .filter { it.startTime.atZone(zone).toLocalDate() == day }
            .sumOf { it.totalSecondsFocused }
        return totalSeconds / 60
    }

    fun getMinutesFocusedToday(zone: ZoneId = ZoneId.systemDefault()): Int =
        getMinutesFocusedOnDay(LocalDate.now(), zone)


    fun logPastSession(
        mode: FocusModeEntity,
        startTime: Instant,
        endTime: Instant,
        tag: FocusTagEntity?
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
                    tagId = tag?.id,
                    targetType = FocusTargetType.TAG,
                    targetId = tag?.id,
                    targetLabel = tag?.name,
                )
            )
            val focusMinutes = (totalSeconds / 60).toInt()
            if (focusMinutes > 0) {
                userRepository.addXp(focusMinutes * ExperienceEngine.XP_PER_FOCUS_MINS)
            }
        }
    }
}
