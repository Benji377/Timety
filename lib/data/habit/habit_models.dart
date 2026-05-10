import 'package:flutter/material.dart';
import 'package:hive/hive.dart';

import '../../theme/app_theme.dart';

part 'habit_models.g.dart';

@HiveType(typeId: 31)
enum HabitFrequency {
  @HiveField(0)
  daily,
  @HiveField(1)
  weeklyExact,
  @HiveField(2)
  weeklyFlexible,
}

@HiveType(typeId: 30)
class Habit {
  @HiveField(0)
  final String id;
  @HiveField(1)
  String name;
  @HiveField(2)
  HabitFrequency frequency;

  @HiveField(3)
  int? targetDaysPerWeek;
  @HiveField(4)
  List<int>? targetWeekdays;

  // Stored as minutes from midnight (e.g., 08:00 = 480). Null means anytime today.
  @HiveField(5)
  int? targetTimeMinutes;

  @HiveField(6)
  List<DateTime> completions;

  @HiveField(7)
  final DateTime createdAt;
  @HiveField(8)
  int colorValue;
  @HiveField(9)
  String? notes;
  @HiveField(10)
  int? iconCodePoint;
  @HiveField(11)
  String? stackName;
  @HiveField(12)
  int? stackOrder;

  Habit({
    required this.id,
    required this.name,
    required this.frequency,
    this.targetDaysPerWeek,
    this.targetWeekdays,
    this.targetTimeMinutes,
    this.notes,
    this.iconCodePoint,
    this.stackName,
    this.stackOrder,
    List<DateTime>? completions,
    DateTime? createdAt,
    int? colorValue,
  }) : completions = completions ?? [],
       createdAt = createdAt ?? DateTime.now(),
       colorValue = colorValue ?? AppTheme.habitColor.toARGB32();

  // Helper to get Flutter's TimeOfDay from the stored minutes
  TimeOfDay? get targetTime {
    if (targetTimeMinutes == null) return null;
    return TimeOfDay(
      hour: targetTimeMinutes! ~/ 60,
      minute: targetTimeMinutes! % 60,
    );
  }

  // Helper to safely set TimeOfDay
  void setTargetTime(TimeOfDay? time) {
    if (time == null) {
      targetTimeMinutes = null;
    } else {
      targetTimeMinutes = time.hour * 60 + time.minute;
    }
  }

  // Keep persisted data compatible while rendering the saved Material icon.
  IconData? get iconData => iconCodePoint != null
      ? IconData(iconCodePoint!, fontFamily: 'MaterialIcons')
      : null;
}
