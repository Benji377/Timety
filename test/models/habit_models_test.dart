import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:timety/data/habit/habit_models.dart';
import 'package:timety/utils/habit_icons.dart';

void main() {
  group('Habit model', () {
    test('converts target time between stored minutes and TimeOfDay', () {
      final habit = Habit(
        id: 'habit',
        name: 'Reading',
        frequency: HabitFrequency.daily,
      );

      habit.setTargetTime(const TimeOfDay(hour: 7, minute: 30));

      expect(habit.targetTimeMinutes, 450);
      expect(habit.targetTime, const TimeOfDay(hour: 7, minute: 30));
    });

    test('iconData resolves known icons and ignores unknown codes', () {
      final icon = HabitIcons.availableIcons.first;

      final habit = Habit(
        id: 'habit',
        name: 'Reading',
        frequency: HabitFrequency.daily,
        iconCodePoint: icon.codePoint,
      );

      expect(habit.iconData?.codePoint, icon.codePoint);

      final unknown = Habit(
        id: 'unknown',
        name: 'Unknown',
        frequency: HabitFrequency.daily,
        iconCodePoint: 123456,
      );

      expect(unknown.iconData, isNull);
    });
  });
}
