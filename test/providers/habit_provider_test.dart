import 'package:flutter_test/flutter_test.dart';
import 'package:timety/data/habit/habit_models.dart';
import 'package:timety/providers/habit_provider.dart';

import '../test_support/fakes.dart';

void main() {
  test('loads habits, filters by day, and toggles completion', () async {
    final today = DateTime.now();
    final startOfWeek = DateTime(
      today.year,
      today.month,
      today.day,
    ).subtract(Duration(days: today.weekday - 1));
    var completionDate = today.subtract(const Duration(days: 1));
    if (completionDate.isBefore(startOfWeek)) {
      completionDate = startOfWeek.add(const Duration(days: 1));
    }

    final dailyHabit = Habit(
      id: 'daily',
      name: 'Daily habit',
      frequency: HabitFrequency.daily,
      targetDaysPerWeek: 3,
      completions: [],
    );
    final exactHabit = Habit(
      id: 'exact',
      name: 'Exact habit',
      frequency: HabitFrequency.weeklyExact,
      targetWeekdays: [today.weekday],
    );
    final flexibleHabit = Habit(
      id: 'flexible',
      name: 'Flexible habit',
      frequency: HabitFrequency.weeklyFlexible,
      targetDaysPerWeek: 4,
      completions: [completionDate],
    );
    final repository = FakeHabitRepository(
      initialHabits: [dailyHabit, exactHabit, flexibleHabit],
    );
    final provider = HabitProvider(repository: repository);
    await provider.loadHabits();

    await drainEventQueue();

    expect(provider.habits, hasLength(3));
    expect(provider.getHabitsForDay(today), hasLength(3));
    expect(provider.isCompletedOn(flexibleHabit, today), isFalse);
    expect(provider.getCompletionsThisWeek(flexibleHabit), 1);

    await provider.toggleCompletionToday(dailyHabit);
    expect(provider.isCompletedOn(dailyHabit, today), isTrue);
    expect(dailyHabit.completions, hasLength(1));

    await provider.toggleCompletionToday(dailyHabit);
    expect(provider.isCompletedOn(dailyHabit, today), isFalse);
    expect(dailyHabit.completions, isEmpty);

    await provider.saveHabit(
      Habit(id: 'new', name: 'New habit', frequency: HabitFrequency.daily),
    );
    expect(provider.habits, hasLength(4));

    await provider.deleteHabit('new');
    expect(provider.habits, hasLength(3));
  });
}
