import 'dart:async';
import 'package:flutter/material.dart';
import 'package:audioplayers/audioplayers.dart';
import '../theme/app_theme.dart';
import '../data/focus/focus_models.dart';
import '../data/focus/focus_repository.dart';
import '../services/notification_service.dart';
import '../utils/habit/habit_utils.dart';
import '../utils/ui/l10n_utils.dart';
import '../utils/stats/xp_calculator.dart';
import 'habit_provider.dart';
import 'task_provider.dart';
import 'user_provider.dart';
import 'settings_provider.dart';

/// Manages the state and logic for Focus Sessions (Timers, Stopwatches, Pomodoro).
///
/// Handles background timers, notification updates, session history saving,
/// and integration with Tasks and Habits for auto-completion.
class FocusProvider extends ChangeNotifier with WidgetsBindingObserver {
  // External providers. These are set via attach methods to avoid circular dependencies.
  final FocusRepository repository;
  UserProvider? _userProvider;
  SettingsProvider? _settingsProvider;
  TaskProvider? _taskProvider;
  HabitProvider? _habitProvider;

  // --- STATE ---
  List<FocusMode> _modes = [];
  List<FocusSession> _history = [];
  List<FocusTag> _tags = [];
  FocusTag? _selectedTag;
  FocusTargetSelection? _selectedTarget;

  FocusMode? _activeMode;
  FocusSession? _currentSession;

  Timer? _timer;
  bool _isRunning = false;
  bool _isPaused = false;
  int _currentSecondsFocussed = 0;

  DateTime? _sessionBaseTime;
  DateTime? _phaseBaseTime;
  int _currentPhaseTotalSeconds = 0;
  int _accumulatedFocusSecondsBeforePhase = 0;

  bool _awaitingPhaseContinue = false;
  int _flexibleDurationMinutes = 25;

  int _currentPhaseIndex = 0;
  int _secondsRemainingInPhase = 0;
  PhaseType _currentPhaseType = PhaseType.focus;

  // --- GETTERS ---
  List<FocusMode> get modes => _modes;
  List<FocusSession> get history => _history;
  FocusMode? get activeMode => _activeMode;
  bool get isRunning => _isRunning;
  bool get isPaused => _isPaused;
  int get currentSecondsFocussed => _currentSecondsFocussed;
  int get currentPhaseIndex => _currentPhaseIndex;
  int get secondsRemainingInPhase => _secondsRemainingInPhase;
  PhaseType get currentPhaseType => _currentPhaseType;
  bool get awaitingPhaseContinue => _awaitingPhaseContinue;
  int get flexibleDurationMinutes => _flexibleDurationMinutes;
  List<FocusTag> get tags => _tags;
  FocusTag? get selectedTag => _selectedTag;
  FocusTargetSelection? get selectedTarget => _selectedTarget;
  String? get selectedTargetLabel => _selectedTarget?.label;
  Color get selectedTargetColor => _selectedTarget == null
      ? AppTheme.focusColor
      : Color(_selectedTarget!.colorValue);

  /// Returns true if the currently selected habit is part of a sequence
  /// and the preceding habit has not been completed yet.
  bool get selectedTargetIsLocked {
    final target = _selectedTarget;
    if (target?.type != FocusTargetType.habit) return false;
    return _isHabitTargetLocked(target!.id);
  }

  FocusProvider({required this.repository}) {
    _init();
  }

  /// Detects when the user unlocks the screen and returns to the app.
  /// Flutter suspends timers when the app is backgrounded on iOS/Android,
  /// so this revives the timer to ensure the UI catches up instantly.
  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      if (_isRunning) {
        _timer?.cancel();
        _tick();
        _timer = Timer.periodic(const Duration(seconds: 1), _tick);
      } else if (_isPaused) {
        notifyListeners();
      }
    }
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _timer?.cancel();
    super.dispose();
  }

  /// Loads saved data from Hive and ensures the three core system modes exist.
  Future<void> _init() async {
    WidgetsBinding.instance.addObserver(this);

    _modes = await repository.fetchModes();
    _history = await repository.fetchSessions();
    _tags = await repository.fetchTags();

    // Ensure there's always at least one tag for the default target
    if (_tags.isEmpty) {
      final defaultTag = FocusTag(
        id: 'default_tag',
        name: 'None',
        colorValue: AppTheme.focusColor.toARGB32(),
      );
      await repository.saveTag(defaultTag);
      _tags.add(defaultTag);
    }
    _selectedTarget = FocusTargetSelection.tag(_tags.first);
    _selectedTag = _tags.first;

    // Create the system modes if they don't exist yet
    if (!_modes.any((m) => m.id == 'system_stopwatch')) {
      final sw = FocusMode.stopwatch();
      await repository.saveMode(sw);
      _modes.add(sw);
    }
    if (!_modes.any((m) => m.id == 'system_flexible')) {
      final flex = FocusMode.flexible();
      await repository.saveMode(flex);
      _modes.add(flex);
    }
    if (!_modes.any((m) => m.id == 'system_pomodoro')) {
      final pomo = FocusMode.classicPomodoro();
      await repository.saveMode(pomo);
      _modes.add(pomo);
    }

    _activeMode = _modes.firstWhere((m) => m.id == 'system_stopwatch');
    _setupPhase(0);
    notifyListeners();
  }

  // --- ATTACH EXTERNAL PROVIDERS ---

  void attachUserProvider(UserProvider userProvider) {
    _userProvider = userProvider;
  }

  void attachSettingsProvider(SettingsProvider settingsProvider) {
    _settingsProvider = settingsProvider;
  }

  void attachTaskProvider(TaskProvider taskProvider) {
    _taskProvider = taskProvider;
  }

  void attachHabitProvider(HabitProvider habitProvider) {
    _habitProvider = habitProvider;
  }

  // Checks if the given habit is locked due to being part of a stack where the previous habit(s) have not been completed today.
  bool _isHabitTargetLocked(String habitId) {
    final habitProvider = _habitProvider;
    if (habitProvider == null) return false;

    final habit = habitProvider.getHabitById(habitId);
    if (habit == null) return false;

    final stackName = habit.stackName?.trim();
    if (stackName == null || stackName.isEmpty) return false;

    final stackHabits =
        habitProvider.habits
            .where((item) => item.stackName?.trim() == stackName)
            .toList()
          ..sort((a, b) => (a.stackOrder ?? 99).compareTo(b.stackOrder ?? 99));

    final index = stackHabits.indexWhere((item) => item.id == habitId);
    if (index <= 0) return false;

    final isCurrentHabitDone = habitProvider.isCompletedOn(
      habit,
      DateTime.now(),
    );
    final isPreviousHabitDone = habitProvider.isCompletedOn(
      stackHabits[index - 1],
      DateTime.now(),
    );
    return HabitUtils.isHabitLocked(
      index: index,
      isCurrentHabitDone: isCurrentHabitDone,
      isPreviousHabitDone: isPreviousHabitDone,
    );
  }

  void _resetSelectedTargetToDefaultTag() {
    if (_tags.isEmpty) return;

    _selectedTag = _tags.first;
    _selectedTarget = FocusTargetSelection.tag(_selectedTag!);
  }

  // --- MODE MANAGEMENT ---

  /// Sets the active focus mode. Cannot be changed while a session is running to prevent inconsistencies.
  void setActiveMode(FocusMode mode) {
    if (_isRunning) return;
    _activeMode = mode;
    _setupPhase(0);
    notifyListeners();
  }

  /// Saves a custom mode. If the mode already exists, it updates it; otherwise, it adds a new one. System modes cannot be modified.
  Future<void> saveCustomMode(FocusMode mode) async {
    if (mode.isSystem) return;
    await repository.saveMode(mode);
    final index = _modes.indexWhere((m) => m.id == mode.id);
    if (index != -1) {
      _modes[index] = mode;
    } else {
      _modes.add(mode);
    }
    notifyListeners();
  }

  /// Deletes a custom mode. System modes cannot be deleted. If the active mode is deleted, it falls back to the default stopwatch mode.
  Future<void> deleteMode(String modeId) async {
    final mode = _modes.firstWhere((m) => m.id == modeId);
    if (mode.isSystem) return;
    await repository.deleteMode(modeId);
    _modes.removeWhere((m) => m.id == modeId);
    if (_activeMode?.id == modeId) {
      _activeMode = _modes.firstWhere((m) => m.id == 'system_stopwatch');
    }
    notifyListeners();
  }

  /// For flexible mode, allows the user to set a custom focus duration. Cannot be changed while running to prevent inconsistencies.
  void setFlexibleDuration(int minutes) {
    if (_activeMode?.type == FocusModeType.flexible && !_isRunning) {
      final clamped = minutes > 120 ? 120 : minutes;
      _flexibleDurationMinutes = clamped;
      _activeMode!.phases.first.durationMinutes = clamped;
      _secondsRemainingInPhase = clamped * 60;
      _currentPhaseTotalSeconds = clamped * 60;
      _phaseBaseTime = DateTime.now();
      notifyListeners();
    }
  }

  /// Updates the focus timer notification based on the current state.
  void _updateNotification({bool asPaused = false}) {
    if (_activeMode == null || _activeMode!.phases.isEmpty) return;

    final l10n = getL10n(settings: _settingsProvider);
    final currentPhase = _activeMode!.phases[_currentPhaseIndex];
    final isStopwatch = _activeMode!.type == FocusModeType.stopwatch;

    final String titleText = asPaused
        ? l10n.focusStatePaused
        : _activeMode!.getLocalizedName(l10n);
    final List<String> bodyParts = [];

    if (selectedTargetLabel != null && selectedTargetLabel!.isNotEmpty) {
      bodyParts.add('${l10n.focusTargetLabel}: $selectedTargetLabel');
    }

    DateTime targetTime;

    if (asPaused) {
      final safeRemaining = _secondsRemainingInPhase > 0
          ? _secondsRemainingInPhase
          : 0;
      final mins = safeRemaining ~/ 60;
      final secs = (safeRemaining % 60).toString().padLeft(2, '0');

      if (!isStopwatch) {
        bodyParts.add(l10n.notificationTimeRemaining(mins.toString(), secs));
      }
      targetTime = DateTime.now();
    } else {
      final int remainingForNotification =
          (!isStopwatch && _secondsRemainingInPhase > 0)
          ? _secondsRemainingInPhase
          : 1;
      final int targetSeconds = remainingForNotification < 1
          ? 1
          : remainingForNotification;

      if (isStopwatch) {
        targetTime = DateTime.now().subtract(
          Duration(seconds: _currentSecondsFocussed),
        );
        if (_settingsProvider?.maxStopwatchMins != null) {
          bodyParts.add(
            l10n.focusLimitMax(_settingsProvider!.maxStopwatchMins),
          );
        }
      } else {
        targetTime = DateTime.now().add(Duration(seconds: targetSeconds));
        final timeString = _settingsProvider != null
            ? _settingsProvider!.getFormattedTime(targetTime)
            : "${targetTime.hour.toString().padLeft(2, '0')}:${targetTime.minute.toString().padLeft(2, '0')}";

        bodyParts.add('${l10n.focusEndsAt} $timeString');
      }
    }

    final String bodyText = bodyParts.isEmpty && !asPaused
        ? l10n.focusStateActive
        : bodyParts.join('  |  ');

    NotificationService.instance.showFocusTimerNotification(
      title: titleText,
      body: bodyText,
      targetTime: targetTime,
      isStopwatch: isStopwatch,
      notificationColor: currentPhase.type == PhaseType.rest
          ? AppTheme.warningColor
          : AppTheme.focusColor,
      isPaused: asPaused,
      l10n: l10n,
    );
  }

  // --- CORE TIMER LOGIC ---

  /// Starts or resumes the current focus session.
  /// [context] is required to cache the UI localization strings.
  void startSession() {
    if (_activeMode == null || _activeMode!.phases.isEmpty) return;
    if (_selectedTarget?.type == FocusTargetType.habit &&
        _isHabitTargetLocked(_selectedTarget!.id)) {
      return;
    }

    // If we're starting fresh (not resuming), initialize the session and phase state
    if (!_isRunning && !_isPaused) {
      _currentPhaseIndex = 0;
      _currentSecondsFocussed = 0;
      _setupPhase(_currentPhaseIndex);

      final target = _selectedTarget ?? FocusTargetSelection.tag(_tags.first);

      _currentSession = FocusSession(
        id: DateTime.now().toString(),
        modeId: _activeMode!.id,
        startTime: DateTime.now(),
        tagId: target.type == FocusTargetType.tag ? target.id : null,
        targetType: target.type,
        targetId: target.id,
        targetLabel: target.label,
      );
    }

    _isRunning = true;
    _isPaused = false;

    // Adjust the base times to account for any time that has already elapsed in the current session/phase
    _sessionBaseTime = DateTime.now().subtract(
      Duration(seconds: _currentSecondsFocussed),
    );

    final currentPhase = _activeMode!.phases[_currentPhaseIndex];
    if (_activeMode!.type == FocusModeType.flexible) {
      _currentPhaseTotalSeconds = _flexibleDurationMinutes * 60;
      _phaseBaseTime = DateTime.now().subtract(
        Duration(seconds: _currentPhaseTotalSeconds - _secondsRemainingInPhase),
      );
    } else if (currentPhase.durationMinutes > 0) {
      _currentPhaseTotalSeconds = currentPhase.durationMinutes * 60;
      _phaseBaseTime = DateTime.now().subtract(
        Duration(seconds: _currentPhaseTotalSeconds - _secondsRemainingInPhase),
      );
    } else {
      _phaseBaseTime = DateTime.now();
    }

    _updateNotification();
    notifyListeners();

    _timer?.cancel();
    _timer = Timer.periodic(const Duration(seconds: 1), _tick);
  }

  // Prepare the state for the given phase index. If the index exceeds the number of phases, it means the session is complete.
  void _setupPhase(int index) {
    if (_activeMode == null || index >= _activeMode!.phases.length) {
      stopSession(completed: true, userProvider: _userProvider);
      return;
    }

    final phase = _activeMode!.phases[index];
    _currentPhaseType = phase.type;
    _currentPhaseTotalSeconds = _activeMode!.type == FocusModeType.flexible
        ? _flexibleDurationMinutes * 60
        : (phase.durationMinutes > 0 ? phase.durationMinutes * 60 : 0);

    _secondsRemainingInPhase = _currentPhaseTotalSeconds;
    _phaseBaseTime = DateTime.now();
    _accumulatedFocusSecondsBeforePhase = _currentSecondsFocussed;
    _awaitingPhaseContinue = false;
  }

  // Plays a "ding" sound to notify the user of phase transitions.
  Future<void> _playDing() async {
    try {
      final player = AudioPlayer();
      await player.play(AssetSource('ding.mp3'));
      await player.onPlayerComplete.first;
      await player.dispose();
    } catch (e) {
      debugPrint('Error playing sound: $e');
    }
  }

  /// Moves to the next phase in the current mode. If there are no more phases, it completes the session.
  Future<void> continueToNextPhase() async {
    if (!_awaitingPhaseContinue) return;
    if (_activeMode == null) return;

    _awaitingPhaseContinue = false;
    _currentPhaseIndex++;
    if (_currentPhaseIndex >= _activeMode!.phases.length) {
      stopSession(completed: true, userProvider: _userProvider);
      return;
    }

    _setupPhase(_currentPhaseIndex);
    _isRunning = true;
    _isPaused = false;
    _updateNotification();
    _timer?.cancel();
    _timer = Timer.periodic(const Duration(seconds: 1), _tick);
    notifyListeners();
  }

  // The core loop executed every second.
  // Calculates time elapsed using absolute DateTime differences rather than counting integers
  // to prevent timer drift if the device lags or momentarily suspends the app.
  void _tick([Timer? timer]) {
    final currentPhase = _activeMode!.phases[_currentPhaseIndex];
    final bool isFlexible = _activeMode!.type == FocusModeType.flexible;
    final bool isStopwatch = _activeMode!.type == FocusModeType.stopwatch;

    if (isStopwatch) {
      _sessionBaseTime ??= DateTime.now().subtract(
        Duration(seconds: _currentSecondsFocussed),
      );
      _currentSecondsFocussed = DateTime.now()
          .difference(_sessionBaseTime!)
          .inSeconds;
    } else {
      if (isFlexible || currentPhase.durationMinutes > 0) {
        if (_phaseBaseTime == null) {
          _phaseBaseTime = DateTime.now();
          _currentPhaseTotalSeconds = isFlexible
              ? _flexibleDurationMinutes * 60
              : currentPhase.durationMinutes * 60;
        }

        final elapsedInPhase = DateTime.now()
            .difference(_phaseBaseTime!)
            .inSeconds;
        _secondsRemainingInPhase = _currentPhaseTotalSeconds - elapsedInPhase;

        if (_currentPhaseType == PhaseType.focus) {
          _currentSecondsFocussed =
              _accumulatedFocusSecondsBeforePhase + elapsedInPhase;
        }

        if (_secondsRemainingInPhase <= 0) {
          if (_currentPhaseIndex + 1 < _activeMode!.phases.length) {
            _awaitingPhaseContinue = true;
            _timer?.cancel();
            _isRunning = false;
            _isPaused = false;
            _secondsRemainingInPhase = 0;

            if (_currentPhaseType == PhaseType.focus) {
              _currentSecondsFocussed =
                  _accumulatedFocusSecondsBeforePhase +
                  _currentPhaseTotalSeconds;
            }

            _playDing();
            _updateNotification(asPaused: true);
          } else {
            if (_currentPhaseType == PhaseType.focus) {
              _currentSecondsFocussed =
                  _accumulatedFocusSecondsBeforePhase +
                  _currentPhaseTotalSeconds;
            }
            _playDing();
            stopSession(completed: true, userProvider: _userProvider);
            return;
          }
        }
      }
    }
    notifyListeners();
  }

  /// Pauses the session, cancelling the timer but preserving the current state to allow resuming later
  void pauseSession() {
    _timer?.cancel();
    _isPaused = true;
    _isRunning = false;
    _updateNotification(asPaused: true);
    notifyListeners();
  }

  /// Stops the session and resets all related state.
  /// If [completed] is true, it marks the session as completed and awards XP accordingly.
  void stopSession({bool completed = false, UserProvider? userProvider}) async {
    _timer?.cancel();
    NotificationService.instance.cancelFocusTimerNotification();

    _sessionBaseTime = null;
    _phaseBaseTime = null;
    _currentPhaseTotalSeconds = 0;

    if (_currentSession != null) {
      _currentSession!.endTime = DateTime.now();
      _currentSession!.totalSecondsFocused = _currentSecondsFocussed;
      _currentSession!.isCompleted = completed;
      _currentSession!.tagId =
          _currentSession!.targetType == FocusTargetType.tag
          ? _currentSession!.targetId
          : null;

      // Handles Task/habit auto-completion
      if (completed &&
          _settingsProvider?.autoCompleteFocusTargetOnFinish == true) {
        if (_currentSession!.targetType == FocusTargetType.task &&
            _currentSession!.targetId != null) {
          await _taskProvider?.markTaskCompleted(
            _currentSession!.targetId!,
            userProvider: userProvider,
          );
        } else if (_currentSession!.targetType == FocusTargetType.habit &&
            _currentSession!.targetId != null) {
          await _habitProvider?.markHabitCompletedToday(
            _currentSession!.targetId!,
            userProvider: userProvider,
          );
        }

        _resetSelectedTargetToDefaultTag();
      }

      await repository.saveSession(_currentSession!);
      _history.add(_currentSession!);

      final int focusMinutes = _currentSecondsFocussed ~/ 60;
      if (focusMinutes > 0) {
        userProvider?.addXp(focusMinutes * ExperienceEngine.xpPerFocusMin);
      }
    }

    _isRunning = false;
    _isPaused = false;
    _currentSession = null;
    _currentSecondsFocussed = 0;
    _currentPhaseIndex = 0;
    _flexibleDurationMinutes = 25;
    _setupPhase(0);
    notifyListeners();
  }

  /// Resets the current session without saving it to history
  void resetSession() {
    _timer?.cancel();
    NotificationService.instance.cancelFocusTimerNotification();

    _isRunning = false;
    _isPaused = false;
    _currentSession = null;
    _currentSecondsFocussed = 0;
    _currentPhaseIndex = 0;
    _flexibleDurationMinutes = 25;
    _phaseBaseTime = null;
    _currentPhaseTotalSeconds = 0;
    _setupPhase(0);
    notifyListeners();
  }

  /// Adds a distraction note to the current session
  void logDistraction(String note) {
    if (_currentSession != null) {
      final growableDistractions = List<Distraction>.from(
        _currentSession!.distractions,
      );
      growableDistractions.add(Distraction(time: DateTime.now(), note: note));
      _currentSession!.distractions = growableDistractions;
      repository.saveSession(_currentSession!);
      notifyListeners();
    }
  }

  /// Calculates the total focused minutes for today by summing completed sessions and the current session if it's active.
  int getMinutesFocusedToday() {
    final today = DateTime.now();
    int totalSeconds = 0;
    for (var session in _history) {
      if (session.startTime.year == today.year &&
          session.startTime.month == today.month &&
          session.startTime.day == today.day) {
        totalSeconds += session.totalSecondsFocused;
      }
    }
    if (_isRunning) {
      totalSeconds += _currentSecondsFocussed;
    }
    return totalSeconds ~/ 60;
  }

  /// Allows the user to manually insert a completed focus session
  Future<void> logPastSession({
    required FocusMode mode,
    required DateTime startTime,
    required DateTime endTime,
    FocusTag? tag,
    UserProvider? userProvider,
  }) async {
    final int totalSeconds = endTime.difference(startTime).inSeconds;
    if (totalSeconds <= 0) return;

    final session = FocusSession(
      id: DateTime.now().toString(),
      modeId: mode.id,
      startTime: startTime,
      endTime: endTime,
      totalSecondsFocused: totalSeconds,
      isCompleted: true,
      tagId: tag?.id,
    );

    await repository.saveSession(session);
    _history.add(session);

    final int focusMinutes = totalSeconds ~/ 60;
    if (focusMinutes > 0) {
      userProvider?.addXp(focusMinutes * ExperienceEngine.xpPerFocusMin);
    }
    notifyListeners();
  }

  // --- TARGET MANAGEMENT ---

  void setSelectedTag(FocusTag tag) {
    _selectedTag = tag;
    _selectedTarget = FocusTargetSelection.tag(tag);
    notifyListeners();
  }

  void setSelectedTask({required String id, required String label}) {
    _selectedTag = null;
    _selectedTarget = FocusTargetSelection.task(id: id, label: label);
    notifyListeners();
  }

  void setSelectedHabit({required String id, required String label}) {
    _selectedTag = null;
    _selectedTarget = FocusTargetSelection.habit(id: id, label: label);
    notifyListeners();
  }

  // --- TAG MANAGEMENT ---

  Future<void> createTag(String name, Color color) async {
    final newTag = FocusTag(
      id: DateTime.now().toString(),
      name: name,
      colorValue: color.toARGB32(),
    );
    await repository.saveTag(newTag);
    _tags.add(newTag);
    _selectedTag = newTag;
    _selectedTarget = FocusTargetSelection.tag(newTag);
    notifyListeners();
  }

  Future<void> updateTag(String id, String name, Color color) async {
    final index = _tags.indexWhere((t) => t.id == id);
    if (index != -1) {
      _tags[index] = FocusTag(id: id, name: name, colorValue: color.toARGB32());
      await repository.saveTag(_tags[index]);
      notifyListeners();
    }
  }

  Future<void> deleteTag(String id) async {
    await repository.deleteTag(id);
    _tags.removeWhere((t) => t.id == id);
    if (_selectedTarget?.type == FocusTargetType.tag &&
        _selectedTarget?.id == id) {
      _selectedTag = _tags.isNotEmpty ? _tags.first : null;
      _selectedTarget = _selectedTag == null
          ? null
          : FocusTargetSelection.tag(_selectedTag!);
    }
    notifyListeners();
  }
}
