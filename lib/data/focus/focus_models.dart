import 'package:flutter/material.dart';
import 'package:hive/hive.dart';
import '../../theme/app_theme.dart';
import '../../l10n/app_localizations.dart';

part 'focus_models.g.dart';

@HiveType(typeId: 21)
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

@HiveType(typeId: 22)
enum PhaseType {
  @HiveField(0)
  focus,
  @HiveField(1)
  rest,
}

@HiveType(typeId: 23)
class SessionPhase {
  @HiveField(0)
  final PhaseType type;

  /// The duration of the phase in minutes.
  /// * `0` represents an infinite duration (Stopwatch mode).
  /// * `-1` represents a duration set dynamically at runtime (Flexible mode).
  /// * `> 0` represents an exact minute count.
  @HiveField(1)
  int durationMinutes;

  SessionPhase({required this.type, required this.durationMinutes});
}

@HiveType(typeId: 24)
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

@HiveType(typeId: 25)
class Distraction {
  @HiveField(0)
  final DateTime time;
  @HiveField(1)
  final String note;

  Distraction({required this.time, this.note = ""});
}

@HiveType(typeId: 26)
class FocusTag {
  @HiveField(0)
  final String id;
  @HiveField(1)
  final String name;
  @HiveField(2)
  final int colorValue; // Stores the integer value of a Flutter Color

  FocusTag({required this.id, required this.name, required this.colorValue});
}

@HiveType(typeId: 27)
enum FocusTargetType {
  @HiveField(0)
  tag,
  @HiveField(1)
  task,
  @HiveField(2)
  habit,
}

class FocusTargetSelection {
  final FocusTargetType type;
  final String id;
  final String label;
  final int colorValue;

  const FocusTargetSelection({
    required this.type,
    required this.id,
    required this.label,
    required this.colorValue,
  });

  factory FocusTargetSelection.tag(FocusTag tag) => FocusTargetSelection(
    type: FocusTargetType.tag,
    id: tag.id,
    label: tag.name,
    colorValue: tag.colorValue,
  );

  factory FocusTargetSelection.task({
    required String id,
    required String label,
  }) {
    return FocusTargetSelection(
      type: FocusTargetType.task,
      id: id,
      label: label,
      colorValue: AppTheme.taskColor.toARGB32(),
    );
  }

  factory FocusTargetSelection.habit({
    required String id,
    required String label,
  }) {
    return FocusTargetSelection(
      type: FocusTargetType.habit,
      id: id,
      label: label,
      colorValue: AppTheme.habitColor.toARGB32(),
    );
  }

  Color get color => Color(colorValue);
}

@HiveType(typeId: 20)
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
  @HiveField(8)
  FocusTargetType targetType;
  @HiveField(9)
  String? targetId;
  @HiveField(10)
  String? targetLabel;

  FocusSession({
    required this.id,
    required this.modeId,
    required this.startTime,
    this.endTime,
    this.totalSecondsFocused = 0,
    this.distractions = const [],
    this.isCompleted = false,
    this.tagId,
    this.targetType = FocusTargetType.tag,
    this.targetId,
    this.targetLabel,
  });
}

enum DistractionType {
  distracted('Distracted', Icons.warning_amber, AppTheme.errorColor),
  hydrated('Hydrated / Drink', Icons.water_drop, AppTheme.taskColor),
  stretched('Stretched', Icons.accessibility_new, AppTheme.warningColor),
  snack('Snack', Icons.restaurant, AppTheme.successColor),
  restroom('Restroom', Icons.wc, Colors.grey);

  final String dbId;
  final IconData icon;
  final Color color;

  const DistractionType(this.dbId, this.icon, this.color);

  // Helper to safely parse strings from the database back into Enums
  static DistractionType fromDbId(String id) {
    return DistractionType.values.firstWhere(
      (type) => type.dbId == id,
      // Fallback if somehow a weird string got saved
      orElse: () => DistractionType.distracted,
    );
  }

  String getLocalizedName(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    switch (this) {
      case DistractionType.distracted:
        return l10n.distractionDistracted;
      case DistractionType.hydrated:
        return l10n.distractionHydrated;
      case DistractionType.stretched:
        return l10n.distractionStretched;
      case DistractionType.snack:
        return l10n.distractionSnack;
      case DistractionType.restroom:
        return l10n.distractionRestroom;
    }
  }
}

class TargetTypeStat {
  final FocusTargetType type;
  final String label;
  final Color color;
  final IconData icon;
  final int minutes;

  const TargetTypeStat({
    required this.type,
    required this.label,
    required this.color,
    required this.icon,
    required this.minutes,
  });
}

class DistractionEntry {
  final Distraction distraction;
  final String targetName;

  DistractionEntry({required this.distraction, required this.targetName});
}