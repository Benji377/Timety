import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:hive/hive.dart';
import 'package:timety/data/habit/habit_models.dart';
import 'package:timety/data/habit/habit_repository_hive.dart';

import '../test_support/hive_test_utils.dart';

void main() {
  late Directory hiveDir;

  setUpAll(() async {
    hiveDir = await initializeHiveTestDir();
  });

  tearDownAll(() async {
    await disposeHiveTestDir(hiveDir);
  });

  test('saves, fetches, deletes, and clears habits', () async {
    final repository = HiveHabitRepository();
    final habitA = Habit(
      id: 'habit-a',
      name: 'Habit A',
      frequency: HabitFrequency.daily,
    );
    final habitB = Habit(
      id: 'habit-b',
      name: 'Habit B',
      frequency: HabitFrequency.weeklyFlexible,
      targetDaysPerWeek: 3,
    );

    await repository.saveHabit(habitA);
    await repository.saveHabit(habitB);

    final habits = await repository.fetchHabits();
    expect(habits, hasLength(2));
    expect(
      habits.map((habit) => habit.id),
      containsAll(['habit-a', 'habit-b']),
    );

    await repository.deleteHabit('habit-a');
    final afterDelete = await repository.fetchHabits();
    expect(afterDelete, hasLength(1));
    expect(afterDelete.single.id, 'habit-b');

    await repository.clearAll();
    expect(await repository.fetchHabits(), isEmpty);

    final box = await Hive.openBox<Habit>(HiveHabitRepository.boxName);
    expect(box.isEmpty, isTrue);
  });
}
