import 'dart:async';
import 'package:flutter/material.dart';
import '../theme/app_theme.dart';
import '../data/focus/focus_models.dart';
import '../data/focus/focus_repository.dart';
import '../services/notification_service.dart';
import '../utils/xp_calculator.dart';
import 'user_provider.dart';

class FocusProvider extends ChangeNotifier {
  final FocusRepository repository;
  UserProvider? _userProvider;

  // --- STATE ---
  List<FocusMode> _modes = [];
  List<FocusSession> _history = [];
  List<FocusTag> _tags = [];
  FocusTag? _selectedTag;

  FocusMode? _activeMode;
  FocusSession? _currentSession;

  Timer? _timer;
  bool _isRunning = false;
  bool _isPaused = false;
  int _currentSecondsFocussed = 0;

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
  List<FocusTag> get tags => _tags;
  FocusTag? get selectedTag => _selectedTag;

  // Require the repository in the constructor
  FocusProvider({required this.repository}) {
    _init();
  }

  void attachUserProvider(UserProvider userProvider) {
    _userProvider = userProvider;
  }

  Future<void> _init() async {
    _modes = await repository.fetchModes();
    _history = await repository.fetchSessions();
    _tags = await repository.fetchTags();
    if (_tags.isEmpty) {
      // Create a default tag so it isn't empty
      final defaultTag = FocusTag(
        id: 'default_tag',
        name: 'None',
        colorValue: AppTheme.focusColor.toARGB32(),
      );
      await repository.saveTag(defaultTag);
      _tags.add(defaultTag);
    }
    _selectedTag = _tags.first;

    // Inject System Modes if they don't exist
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

  // --- MODE MANAGEMENT ---

  void setActiveMode(FocusMode mode) {
    if (_isRunning) return;
    _activeMode = mode;
    _setupPhase(0);
    notifyListeners();
  }

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

  // --- FLEXIBLE TIMER SETTER ---
  void setFlexibleDuration(int minutes) {
    if (_activeMode?.type == FocusModeType.flexible && !_isRunning) {
      final clamped = minutes > 120 ? 120 : minutes;
      _activeMode!.phases.first.durationMinutes = clamped;
      _secondsRemainingInPhase = clamped * 60;
      notifyListeners();
    }
  }

  // --- SESSION ACTIONS ---

  void _updateNotification({bool asPaused = false}) {
    if (_activeMode == null || _activeMode!.phases.isEmpty) return;

    final currentPhase = _activeMode!.phases[_currentPhaseIndex];
    final isStopwatch = currentPhase.durationMinutes == -1;

    if (asPaused) {
      final mins = _secondsRemainingInPhase ~/ 60;
      final secs = (_secondsRemainingInPhase % 60).toString().padLeft(2, '0');
      NotificationService.instance.showFocusTimerNotification(
        phaseName: currentPhase.type.name,
        targetTime: DateTime.now(),
        isStopwatch: isStopwatch,
        isPaused: true,
        pausedText: isStopwatch ? "Paused" : "$mins:$secs remaining",
      );
    } else {
      final targetTime = isStopwatch
          ? DateTime.now().subtract(Duration(seconds: _currentSecondsFocussed))
          : DateTime.now().add(Duration(seconds: _secondsRemainingInPhase));

      NotificationService.instance.showFocusTimerNotification(
        phaseName: currentPhase.type.name,
        targetTime: targetTime,
        isStopwatch: isStopwatch,
        isPaused: false,
      );
    }
  }

  void startSession() {
    if (_activeMode == null || _activeMode!.phases.isEmpty) return;

    if (!_isRunning && !_isPaused) {
      _currentPhaseIndex = 0;
      _currentSecondsFocussed = 0;
      _setupPhase(_currentPhaseIndex);

      _currentSession = FocusSession(
        id: DateTime.now().toString(),
        modeId: _activeMode!.id,
        startTime: DateTime.now(),
      );
    }

    _isRunning = true;
    _isPaused = false;

    _updateNotification();
    notifyListeners();

    _timer?.cancel();
    _timer = Timer.periodic(const Duration(seconds: 1), _tick);
  }

  void _setupPhase(int index) {
    if (_activeMode == null || index >= _activeMode!.phases.length) {
      stopSession(completed: true, userProvider: _userProvider);
      return;
    }

    final phase = _activeMode!.phases[index];
    _currentPhaseType = phase.type;
    _secondsRemainingInPhase = phase.durationMinutes > 0
        ? phase.durationMinutes * 60
        : 0;
  }

  void _tick(Timer timer) {
    if (_currentPhaseType == PhaseType.focus) {
      _currentSecondsFocussed++;
    }

    final currentPhase = _activeMode!.phases[_currentPhaseIndex];

    if (currentPhase.durationMinutes > 0) {
      _secondsRemainingInPhase--;

      if (_secondsRemainingInPhase <= 0) {
        _currentPhaseIndex++;
        _setupPhase(_currentPhaseIndex);

        if (_isRunning) {
          _updateNotification();
        }
      }
    }
    notifyListeners();
  }

  void pauseSession() {
    _timer?.cancel();
    _isPaused = true;
    _isRunning = false;
    _updateNotification(asPaused: true);
    notifyListeners();
  }

  void stopSession({bool completed = false, UserProvider? userProvider}) async {
    _timer?.cancel();
    NotificationService.instance.cancelFocusTimerNotification();

    if (_currentSession != null) {
      _currentSession!.endTime = DateTime.now();
      _currentSession!.totalSecondsFocused = _currentSecondsFocussed;
      _currentSession!.isCompleted = completed;
      _currentSession!.tagId = _selectedTag?.id;

      await repository.saveSession(_currentSession!);
      _history.add(_currentSession!);

      // ADD XP based on minutes focused
      int focusMinutes = _currentSecondsFocussed ~/ 60;
      if (focusMinutes > 0) {
        userProvider?.addXp(focusMinutes * ExperienceEngine.xpPerFocusMin);
      }
    }

    _isRunning = false;
    _isPaused = false;
    _currentSession = null;
    _currentSecondsFocussed = 0;
    _currentPhaseIndex = 0;
    _setupPhase(0);
    notifyListeners();
  }

  void resetSession() {
    _timer?.cancel();
    NotificationService.instance.cancelFocusTimerNotification();

    _isRunning = false;
    _isPaused = false;
    _currentSession = null;
    _currentSecondsFocussed = 0;
    _currentPhaseIndex = 0;
    _setupPhase(0);
    notifyListeners();
  }

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

  // --- TIME MACHINE LOGGING ---
  Future<void> logPastSession({
    required FocusMode mode,
    required DateTime startTime,
    required DateTime endTime,
    FocusTag? tag,
    UserProvider? userProvider,
  }) async {
    int totalSeconds = endTime.difference(startTime).inSeconds;
    if (totalSeconds <= 0) return;

    final session = FocusSession(
      id: DateTime.now().toString(),
      modeId: mode.id,
      startTime: startTime,
      endTime: endTime,
      totalSecondsFocused:
          totalSeconds, // For manual logs, we assume the time block was all focus
      isCompleted: true,
      tagId: tag?.id,
    );

    await repository.saveSession(session);
    _history.add(session);

    // ADD XP based on minutes focused
    int focusMinutes = totalSeconds ~/ 60;
    if (focusMinutes > 0) {
      userProvider?.addXp(focusMinutes * ExperienceEngine.xpPerFocusMin);
    }

    notifyListeners();
  }

  // --- TAG MANAGEMENT ---

  void setSelectedTag(FocusTag tag) {
    _selectedTag = tag;
    notifyListeners();
  }

  Future<void> createTag(String name, Color color) async {
    final newTag = FocusTag(
      id: DateTime.now().toString(),
      name: name,
      colorValue: color.toARGB32(),
    );
    await repository.saveTag(newTag);
    _tags.add(newTag);
    _selectedTag = newTag; // Auto-select the newly created tag
    notifyListeners();
  }

  Future<void> deleteTag(String id) async {
    await repository.deleteTag(id);
    _tags.removeWhere((t) => t.id == id);
    if (_selectedTag?.id == id) {
      _selectedTag = _tags.isNotEmpty ? _tags.first : null;
    }
    notifyListeners();
  }
}
