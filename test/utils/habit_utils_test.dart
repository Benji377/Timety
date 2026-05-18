import 'package:flutter_test/flutter_test.dart';

import 'package:timety/data/habit/habit_models.dart';
import 'package:timety/providers/habit_provider.dart';
import 'package:timety/utils/date_utils.dart';
import 'package:timety/utils/habit_utils.dart';

import '../test_support/fakes.dart';

void main() {
  late FakeHabitRepository habitRepository;
  late HabitProvider habitProvider;

  setUp(() async {
    habitRepository = FakeHabitRepository(
      initialHabits: [
        buildDailyHabit(id: 'daily', name: 'Daily habit'),
        Habit(
          id: 'weekly',
          name: 'Weekly habit',
          frequency: HabitFrequency.weeklyFlexible,
          targetDaysPerWeek: 3,
          completions: [
            DateTime.now(),
            DateTime.now().subtract(const Duration(days: 1)),
          ],
        ),
      ],
    );
    habitProvider = HabitProvider(repository: habitRepository);
    await drainEventQueue();
  });

  group('HabitUtils', () {
    test('buildHabitSubtitle handles daily, exact, and flexible habits', () {
      final dailyHabit = buildDailyHabit(id: 'daily', name: 'Daily habit');
      final exactHabit = Habit(
        id: 'exact',
        name: 'Exact habit',
        frequency: HabitFrequency.weeklyExact,
        targetWeekdays: [DateTime.monday, DateTime.wednesday],
      );
      final flexibleHabit = Habit(
        id: 'flex',
        name: 'Flexible habit',
        frequency: HabitFrequency.weeklyFlexible,
        targetDaysPerWeek: 4,
      );

      expect(HabitUtils.buildHabitSubtitle(dailyHabit, habitProvider), 'Daily');
      expect(
        HabitUtils.buildHabitSubtitle(exactHabit, habitProvider),
        'Weekly on ${AppDateUtils.weekdayToStringShort(DateTime.monday)}, ${AppDateUtils.weekdayToStringShort(DateTime.wednesday)}',
      );
      expect(
        HabitUtils.buildHabitSubtitle(flexibleHabit, habitProvider),
        '0 / 4 this week',
      );
    });

    test(
      'isHabitLocked only locks later stacked habits that are not ready',
      () {
        expect(
          HabitUtils.isHabitLocked(
            index: 0,
            isCurrentHabitDone: false,
            isPreviousHabitDone: false,
          ),
          isFalse,
        );
        expect(
          HabitUtils.isHabitLocked(
            index: 1,
            isCurrentHabitDone: false,
            isPreviousHabitDone: false,
          ),
          isTrue,
        );
        expect(
          HabitUtils.isHabitLocked(
            index: 1,
            isCurrentHabitDone: true,
            isPreviousHabitDone: false,
          ),
          isFalse,
        );
        expect(
          HabitUtils.isHabitLocked(
            index: 1,
            isCurrentHabitDone: false,
            isPreviousHabitDone: true,
          ),
          isFalse,
        );
      },
    );

    test('stack helpers inspect provider completion state', () {
      final today = DateTime.now();
      final stackHabits = [
        buildDailyHabit(id: 'stack1', name: 'Stack one', completions: [today]),
        buildDailyHabit(id: 'stack2', name: 'Stack two', completions: []),
      ];
      stackHabits[0].stackName = 'Morning';
      stackHabits[1].stackName = 'Morning';

      habitProvider.habits.addAll(stackHabits);

      expect(
        HabitUtils.isStackFullyCompleted(
          stackHabits: stackHabits,
          provider: habitProvider,
          date: today,
        ),
        isFalse,
      );
      expect(
        HabitUtils.getStackCompletionCount(
          stackHabits: stackHabits,
          provider: habitProvider,
          date: today,
        ),
        1,
      );

      stackHabits[1].completions = [today];

      expect(
        HabitUtils.getStackCompletionCount(
          stackHabits: stackHabits,
          provider: habitProvider,
          date: today,
        ),
        2,
      );
      expect(
        HabitUtils.isStackFullyCompleted(
          stackHabits: stackHabits,
          provider: habitProvider,
          date: today,
        ),
        isTrue,
      );
    });
  });
}
