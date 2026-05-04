import 'package:flutter/material.dart';
import 'package:hive/hive.dart';
import '../data/focus_models.dart';

class FocusProvider extends ChangeNotifier {
  static const String modeBoxName = 'focusModesBox';
  static const String sessionBoxName = 'focusSessionsBox';

  List<FocusMode> _modes = [];
  List<FocusSession> _history = [];
  
  FocusMode? _activeMode;
  FocusSession? _currentSession;
  
  bool _isRunning = false;
  bool _isPaused = false;
  int _currentSecondsFocussed = 0;
  int _dailyTargetMinutes = 90;

  List<FocusMode> get modes => _modes;
  List<FocusSession> get history => _history;
  FocusMode? get activeMode => _activeMode;
  bool get isRunning => _isRunning;
  bool get isPaused => _isPaused;
  int get currentSecondsFocussed => _currentSecondsFocussed;
  int get dailyTargetMinutes => _dailyTargetMinutes;

  FocusProvider() {
    _init();
  }

  Future<void> _init() async {
    final modeBox = await Hive.openBox<FocusMode>(modeBoxName);
    final sessionBox = await Hive.openBox<FocusSession>(sessionBoxName);

    _modes = modeBox.values.toList();
    _history = sessionBox.values.toList();

    // Inject System Modes if they don't exist
    if (!_modes.any((m) => m.id == 'system_stopwatch')) {
      final sw = FocusMode.stopwatch();
      await modeBox.put(sw.id, sw);
      _modes.add(sw);
    }
    if (!_modes.any((m) => m.id == 'system_flexible')) {
      final flex = FocusMode.flexible();
      await modeBox.put(flex.id, flex);
      _modes.add(flex);
    }
    if (!_modes.any((m) => m.id == 'system_pomodoro')) {
      final pomo = FocusMode.classicPomodoro();
      await modeBox.put(pomo.id, pomo);
      _modes.add(pomo);
    }

    _activeMode = _modes.firstWhere((m) => m.id == 'system_stopwatch');
    notifyListeners();
  }

  // --- MODE MANAGEMENT ---

  void setActiveMode(FocusMode mode) {
    if (_isRunning) return;
    _activeMode = mode;
    notifyListeners();
  }

  Future<void> saveCustomMode(FocusMode mode) async {
    if (mode.isSystem) return; // Guard: Cannot overwrite system modes directly
    
    final modeBox = Hive.box<FocusMode>(modeBoxName);
    await modeBox.put(mode.id, mode);
    
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
    if (mode.isSystem) return; // Guard: Cannot delete system modes
    
    final modeBox = Hive.box<FocusMode>(modeBoxName);
    await modeBox.delete(modeId);
    
    _modes.removeWhere((m) => m.id == modeId);
    if (_activeMode?.id == modeId) {
      _activeMode = _modes.firstWhere((m) => m.id == 'system_stopwatch');
    }
    notifyListeners();
  }

  // --- FLEXIBLE TIMER SETTER ---
  void setFlexibleDuration(int minutes) {
    if (_activeMode?.type == FocusModeType.flexible && !_isRunning) {
      // Clamp to max 120
      final clamped = minutes > 120 ? 120 : minutes;
      _activeMode!.phases.first.durationMinutes = clamped;
      notifyListeners();
    }
  }

  // --- SESSION ACTIONS (Placeholders) ---
  void startSession() {
    _isRunning = true;
    _isPaused = false;
    notifyListeners();
  }

  void pauseSession() {
    _isPaused = true;
    notifyListeners();
  }

  void stopSession() {
    _isRunning = false;
    _isPaused = false;
    _currentSecondsFocussed = 0;
    notifyListeners();
  }

  void logDistraction(String note) {
    if (_currentSession != null) {
      _currentSession!.distractions.add(Distraction(time: DateTime.now(), note: note));
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
}