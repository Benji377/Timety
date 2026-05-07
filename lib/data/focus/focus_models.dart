import 'package:hive/hive.dart';

part 'focus_models.g.dart';

@HiveType(typeId: 3)
enum FocusModeType {
  @HiveField(0)
  stopwatch,
  @HiveField(1)
  pomodoro,
  @HiveField(2)
  flexible,
  @HiveField(3)
  custom,
}

@HiveType(typeId: 7)
enum PhaseType {
  @HiveField(0)
  focus,
  @HiveField(1)
  rest,
}

@HiveType(typeId: 8)
class SessionPhase {
  @HiveField(0)
  final PhaseType type;

  // 0 = infinite (Stopwatch)
  // -1 = set at runtime (Flexible)
  // > 0 = exact minutes
  @HiveField(1)
  int durationMinutes;

  SessionPhase({required this.type, required this.durationMinutes});
}

@HiveType(typeId: 4)
class FocusMode {
  @HiveField(0)
  final String id;
  @HiveField(1)
  final String name;
  @HiveField(2)
  final FocusModeType type;
  @HiveField(3)
  final List<SessionPhase> phases; // The sequence of nodes
  @HiveField(4)
  final bool isSystem; // Locks the mode from being edited/deleted

  FocusMode({
    required this.id,
    required this.name,
    required this.type,
    this.phases = const [],
    this.isSystem = false,
  });

  // SYSTEM MODE 1: Stopwatch (Infinite focus node)
  factory FocusMode.stopwatch() => FocusMode(
    id: 'system_stopwatch',
    name: 'Stopwatch',
    type: FocusModeType.stopwatch,
    isSystem: true,
    phases: [SessionPhase(type: PhaseType.focus, durationMinutes: 0)],
  );

  // SYSTEM MODE 2: Flexible (Runtime focus node)
  factory FocusMode.flexible() => FocusMode(
    id: 'system_flexible',
    name: 'Flexible',
    type: FocusModeType.flexible,
    isSystem: true,
    phases: [SessionPhase(type: PhaseType.focus, durationMinutes: -1)],
  );

  // SYSTEM MODE 3: Pomodoro Classic (Hardcoded node sequence)
  factory FocusMode.classicPomodoro() => FocusMode(
    id: 'system_pomodoro',
    name: 'Pomodoro Classic',
    type: FocusModeType.pomodoro,
    isSystem: true,
    phases: [
      SessionPhase(type: PhaseType.focus, durationMinutes: 25),
      SessionPhase(type: PhaseType.rest, durationMinutes: 5),
      SessionPhase(type: PhaseType.focus, durationMinutes: 25),
      SessionPhase(type: PhaseType.rest, durationMinutes: 5),
      SessionPhase(type: PhaseType.focus, durationMinutes: 25),
      SessionPhase(type: PhaseType.rest, durationMinutes: 5),
      SessionPhase(type: PhaseType.focus, durationMinutes: 25),
      SessionPhase(type: PhaseType.rest, durationMinutes: 15),
    ],
  );
}

@HiveType(typeId: 5)
class Distraction {
  @HiveField(0)
  final DateTime time;
  @HiveField(1)
  final String note;

  Distraction({required this.time, this.note = ""});
}

@HiveType(typeId: 9)
class FocusTag {
  @HiveField(0)
  final String id;
  @HiveField(1)
  final String name;
  @HiveField(2)
  final int colorValue; // Stores the integer value of a Flutter Color

  FocusTag({required this.id, required this.name, required this.colorValue});
}

@HiveType(typeId: 6)
class FocusSession {
  @HiveField(0)
  final String id;
  @HiveField(1)
  final String modeId;
  @HiveField(2)
  final DateTime startTime;
  @HiveField(3)
  DateTime? endTime;
  @HiveField(4)
  int totalSecondsFocused;
  @HiveField(5)
  List<Distraction> distractions;
  @HiveField(6)
  bool isCompleted;
  @HiveField(7)
  String? tagId;

  FocusSession({
    required this.id,
    required this.modeId,
    required this.startTime,
    this.endTime,
    this.totalSecondsFocused = 0,
    this.distractions = const [],
    this.isCompleted = false,
    this.tagId,
  });
}
