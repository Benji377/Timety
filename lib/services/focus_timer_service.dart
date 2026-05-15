import 'dart:async';
import '../data/focus/focus_models.dart';

/// Callback for when a phase completes
typedef OnPhaseComplete = void Function({required bool hasMorePhases});

/// Callback for timer tick
typedef OnTimerTick =
    void Function({required int secondsRemaining, required int secondsFocused});

/// Handles the core timer mechanics for focus sessions
///
/// This service manages:
/// - Timer state (running, paused, stopped)
/// - Phase progression
/// - Elapsed time calculations (including background sleep handling)
/// - Stopwatch vs. timed mode
class FocusTimerService {
  Timer? _timer;

  // State
  bool _isRunning = false;
  bool _isPaused = false;
  int _currentSecondsFocused = 0;
  int _secondsRemainingInPhase = 0;

  // Time anchors for wall-clock calculations
  DateTime? _sessionBaseTime;
  DateTime? _phaseBaseTime;
  int _currentPhaseTotalSeconds = 0;

  // Callbacks
  final OnTimerTick? onTick;
  final OnPhaseComplete? onPhaseComplete;

  FocusTimerService({this.onTick, this.onPhaseComplete});

  bool get isRunning => _isRunning;
  bool get isPaused => _isPaused;
  int get currentSecondsFocused => _currentSecondsFocused;
  int get secondsRemainingInPhase => _secondsRemainingInPhase;

  /// Starts the timer with the given phase configuration
  void start({
    required SessionPhase phase,
    required bool isFocusPhase,
    required bool isFlexibleMode,
    int? flexibleDurationMinutes,
  }) {
    if (_isRunning) return;

    _isRunning = true;
    _isPaused = false;
    _sessionBaseTime ??= DateTime.now();

    _timer = Timer.periodic(const Duration(seconds: 1), (timer) {
      _tick(
        phase: phase,
        isFocusPhase: isFocusPhase,
        isFlexibleMode: isFlexibleMode,
        flexibleDurationMinutes: flexibleDurationMinutes ?? 25,
      );
    });
  }

  /// Pauses the timer
  void pause() {
    if (!_isRunning) return;

    _timer?.cancel();
    _isPaused = true;
    _isRunning = false;
  }

  /// Resumes the paused timer
  void resume() {
    if (_isPaused) {
      _isRunning = true;
      _isPaused = false;

      _timer = Timer.periodic(const Duration(seconds: 1), (timer) {
        // Note: Caller should provide phase context
      });
    }
  }

  /// Stops the timer and resets state
  void stop() {
    _timer?.cancel();
    _isRunning = false;
    _isPaused = false;
    _sessionBaseTime = null;
    _phaseBaseTime = null;
    _currentPhaseTotalSeconds = 0;
  }

  /// Resets the timer to initial state
  void reset() {
    stop();
    _currentSecondsFocused = 0;
    _secondsRemainingInPhase = 0;
  }

  /// Core timer tick logic
  /// Handles stopwatch mode, timed phases, and background sleep recovery
  void _tick({
    required SessionPhase phase,
    required bool isFocusPhase,
    required bool isFlexibleMode,
    required int flexibleDurationMinutes,
  }) {
    final isStopwatch = phase.durationMinutes == -1 && !isFlexibleMode;

    if (isStopwatch) {
      // Stopwatch mode: compute elapsed from session start (wall-clock based)
      _currentSecondsFocused = DateTime.now()
          .difference(_sessionBaseTime!)
          .inSeconds;
    } else {
      // Timed phase: track focused time if this is a focus phase
      if (isFocusPhase) {
        _currentSecondsFocused++;
      }

      // Update phase time tracking
      if (isFlexibleMode || phase.durationMinutes > 0) {
        if (_phaseBaseTime == null) {
          _phaseBaseTime = DateTime.now();
          _currentPhaseTotalSeconds = isFlexibleMode
              ? flexibleDurationMinutes * 60
              : phase.durationMinutes * 60;
        }

        // Calculate remaining time from wall-clock (survives background sleeps)
        final elapsedInPhase = DateTime.now()
            .difference(_phaseBaseTime!)
            .inSeconds;
        _secondsRemainingInPhase = _currentPhaseTotalSeconds - elapsedInPhase;

        // Check if phase is complete
        if (_secondsRemainingInPhase <= 0) {
          _secondsRemainingInPhase = 0;
          onPhaseComplete?.call(
            hasMorePhases: true, // Caller determines if more phases exist
          );
          stop();
          return;
        }
      }
    }

    // Notify listeners of tick
    onTick?.call(
      secondsRemaining: _secondsRemainingInPhase,
      secondsFocused: _currentSecondsFocused,
    );
  }

  /// Advances to the next phase
  void advancePhase() {
    _phaseBaseTime = null;
    _secondsRemainingInPhase = 0;
    _isPaused = false;
    _isRunning = false;
  }

  /// Sets the flexible duration for flexible phases
  void setFlexibleDuration(int minutes) {
    final clamped = minutes.clamp(1, 120);
    if (_currentPhaseTotalSeconds > 0 && _isRunning) {
      // Adjust remaining time if timer is running
      final elapsedInPhase = DateTime.now()
          .difference(_phaseBaseTime!)
          .inSeconds;
      _currentPhaseTotalSeconds = clamped * 60;
      _secondsRemainingInPhase = _currentPhaseTotalSeconds - elapsedInPhase;
    }
  }

  /// Gets the total phase duration
  int getPhaseDurationSeconds() => _currentPhaseTotalSeconds;

  /// Disposes resources
  void dispose() {
    _timer?.cancel();
  }
}
