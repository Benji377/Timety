import 'dart:async';
import '../data/focus/focus_models.dart';

typedef OnPhaseComplete = void Function({required bool hasMorePhases});
typedef OnTimerTick =
    void Function({required int secondsRemaining, required int secondsFocused});

class FocusTimerService {
  Timer? _timer;

  bool _isRunning = false;

  // Time Anchors
  DateTime? _sessionBaseTime;
  DateTime? _phaseBaseTime;
  int _currentPhaseTotalSeconds = 0;
  int _totalFocusedSecondsBeforeThisPhase = 0;

  // Pause Tracking
  Duration _accumulatedPause = Duration.zero;
  DateTime? _pauseStartTime;

  final OnTimerTick? onTick;
  final OnPhaseComplete? onPhaseComplete;

  FocusTimerService({this.onTick, this.onPhaseComplete});

  bool get isRunning => _isRunning && _pauseStartTime == null;
  bool get isPaused => _pauseStartTime != null;
  int _currentSecondsFocused = 0;
  int _secondsRemainingInPhase = 0;

  DateTime get _effectiveNow {
    final base = _pauseStartTime ?? DateTime.now();
    return base.subtract(_accumulatedPause);
  }

  void start({
    required FocusMode mode,
    required SessionPhase phase,
    int? flexibleDurationMinutes,
  }) {
    if (_isRunning) return;
    _isRunning = true;
    _pauseStartTime = null;

    // Anchor times using effectiveNow
    _sessionBaseTime ??= _effectiveNow;
    _phaseBaseTime ??= _effectiveNow;

    _currentPhaseTotalSeconds = mode.type == FocusModeType.flexible
        ? (flexibleDurationMinutes ?? 25) * 60
        : (phase.durationMinutes > 0 ? phase.durationMinutes * 60 : 0);

    _startPeriodicTimer(mode, phase, flexibleDurationMinutes ?? 25);
  }

  void pause() {
    if (isPaused || !_isRunning) return;
    _timer?.cancel();
    _pauseStartTime = DateTime.now();
  }

  void resume({
    required FocusMode mode,
    required SessionPhase phase,
    int? flexibleDurationMinutes,
  }) {
    if (!isPaused) return;

    _accumulatedPause += DateTime.now().difference(_pauseStartTime!);
    _pauseStartTime = null;

    _startPeriodicTimer(mode, phase, flexibleDurationMinutes ?? 25);
  }

  void resuscitateDeadTimer({
    required FocusMode mode,
    required SessionPhase phase,
    int? flexibleDurationMinutes,
  }) {
    if (_isRunning && _pauseStartTime == null) {
      _startPeriodicTimer(mode, phase, flexibleDurationMinutes ?? 25);
      _tick(mode, phase, flexibleDurationMinutes ?? 25);
    }
  }

  void _startPeriodicTimer(FocusMode mode, SessionPhase phase, int flexMins) {
    _timer?.cancel();
    _timer = Timer.periodic(const Duration(seconds: 1), (_) {
      _tick(mode, phase, flexMins);
    });
  }

  void _tick(FocusMode mode, SessionPhase phase, int flexMins) {
    final now = _effectiveNow;

    if (mode.type == FocusModeType.stopwatch) {
      _currentSecondsFocused = now.difference(_sessionBaseTime!).inSeconds;
    } else {
      if (phase.type == PhaseType.focus) {
        _currentSecondsFocused =
            _totalFocusedSecondsBeforeThisPhase +
            now.difference(_phaseBaseTime!).inSeconds;
      }

      if (mode.type == FocusModeType.flexible || phase.durationMinutes > 0) {
        final elapsedInPhase = now.difference(_phaseBaseTime!).inSeconds;
        _secondsRemainingInPhase = _currentPhaseTotalSeconds - elapsedInPhase;

        if (_secondsRemainingInPhase <= 0) {
          _secondsRemainingInPhase = 0;
          onPhaseComplete?.call(hasMorePhases: true);
          stop();
          return;
        }
      }
    }

    onTick?.call(
      secondsRemaining: _secondsRemainingInPhase,
      secondsFocused: _currentSecondsFocused,
    );
  }

  void advancePhase(bool wasFocusPhase) {
    if (wasFocusPhase) {
      _totalFocusedSecondsBeforeThisPhase += _effectiveNow
          .difference(_phaseBaseTime!)
          .inSeconds;
    }
    _phaseBaseTime = _effectiveNow;
    _secondsRemainingInPhase = 0;
    _pauseStartTime = null;
    _isRunning = false;
  }

  void stop() {
    _timer?.cancel();
    _isRunning = false;
    _pauseStartTime = null;
    _accumulatedPause = Duration.zero;
    _sessionBaseTime = null;
    _phaseBaseTime = null;
    _totalFocusedSecondsBeforeThisPhase = 0;
    _currentPhaseTotalSeconds = 0;
  }

  void setFlexibleDuration(int minutes) {
    final clamped = minutes.clamp(1, 120);
    if (_currentPhaseTotalSeconds > 0 && _isRunning) {
      final elapsedInPhase = _effectiveNow
          .difference(_phaseBaseTime!)
          .inSeconds;
      _currentPhaseTotalSeconds = clamped * 60;
      _secondsRemainingInPhase = _currentPhaseTotalSeconds - elapsedInPhase;
    }
  }

  void dispose() {
    _timer?.cancel();
  }
}
