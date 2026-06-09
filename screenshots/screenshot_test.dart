import 'package:hive/hive.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:provider/provider.dart';
import 'package:timety/main.dart' as app;
import 'package:timety/data/focus/focus_models.dart';
import 'package:timety/data/focus/focus_repository_hive.dart';
import 'package:timety/data/habit/habit_models.dart';
import 'package:timety/data/habit/habit_repository_hive.dart';
import 'package:timety/data/task/task.dart';
import 'package:timety/data/task/task_repository_hive.dart';
import 'package:timety/providers/focus_provider.dart';
import 'package:timety/providers/habit_provider.dart';
import 'package:timety/providers/settings_provider.dart';
import 'package:timety/providers/task_provider.dart';
import 'package:timety/providers/user_provider.dart';
import 'package:timety/theme/app_theme.dart';
import 'package:timety/utils/stats/xp_calculator.dart';

// How to execute:
// 1. Start Emulator
// 2. Execute: flutter drive --driver=screenshots/test_driver.dart --target=screenshots/screenshot_test.dart

// --- Pump Loop ---
// Fast-forwards the clock in 50ms increments to allow animations/charts to render
Future<void> _pumpDuration(WidgetTester tester, Duration duration) async {
  final int frameCount = (duration.inMilliseconds / 50).round();
  for (int i = 0; i < frameCount; i++) {
    await tester.pump(const Duration(milliseconds: 50));
  }
}

Future<void> _waitForCondition(
  WidgetTester tester,
  bool Function() condition, {
  Duration timeout = const Duration(seconds: 30),
}) async {
  final stopwatch = Stopwatch()..start();

  while (!condition()) {
    if (stopwatch.elapsed > timeout) {
      throw Exception('Timed out while waiting for the screenshot app state.');
    }
    await tester.pump(const Duration(milliseconds: 100));
  }
}

Future<void> _tapBottomNavItem(WidgetTester tester, String label) async {
  final finder = find.descendant(
    of: find.byType(BottomNavigationBar),
    matching: find.text(label),
  );

  await tester.tap(finder.first);
  // Replaced static pump with the pump loop (1.5 seconds is perfect for page transitions)
  await _pumpDuration(tester, const Duration(milliseconds: 1500));
}

Future<void> _tapIconButton(
  WidgetTester tester,
  IconData icon, {
  int index = 0,
}) async {
  final finder = find.byIcon(icon);
  if (finder.evaluate().isEmpty) {
    throw Exception('Icon $icon not found');
  }

  await tester.tap(finder.at(index));
  // Replaced static pump with the pump loop
  await _pumpDuration(tester, const Duration(milliseconds: 1500));
}

Future<void> _tapText(WidgetTester tester, String text) async {
  final finder = find.text(text);
  if (finder.evaluate().isEmpty) {
    throw Exception('Text "$text" not found');
  }

  await tester.tap(finder.first);
  // Replaced static pump with the pump loop
  await _pumpDuration(tester, const Duration(milliseconds: 1500));
}

Future<void> _resetPersistentData() async {
  final tasksBox = await Hive.openBox<Task>(HiveTaskRepository.boxName);
  await tasksBox.clear();

  final habitsBox = await Hive.openBox<Habit>(HiveHabitRepository.boxName);
  await habitsBox.clear();

  final sessionsBox = await Hive.openBox<FocusSession>(
    HiveFocusRepository.sessionBoxName,
  );
  await sessionsBox.clear();

  final tagsBox = await Hive.openBox<FocusTag>(HiveFocusRepository.tagBoxName);
  await tagsBox.clear();
}

Future<void> _seedMockData(BuildContext context) async {
  final settings = context.read<SettingsProvider>();
  final taskProvider = context.read<TaskProvider>();
  final habitProvider = context.read<HabitProvider>();
  final focusProvider = context.read<FocusProvider>();
  final userProvider = context.read<UserProvider>();

  await userProvider.updateName("Bobert");
  await userProvider.addXp(-userProvider.totalXp);
  settings.setThemeMode(ThemeMode.light);
  settings.setDailyGoal(120);

  taskProvider.tasks.clear();
  habitProvider.habits.clear();
  focusProvider.history.clear();
  focusProvider.tags.clear();

  final today = DateTime.now();
  final dayStart = DateTime(today.year, today.month, today.day);

  await habitProvider.saveHabit(
    Habit(
      id: 'habit_water',
      name: 'Drink Water',
      frequency: HabitFrequency.daily,
      stackName: 'Morning Routine',
      stackOrder: 1,
      colorValue: AppTheme.habitColor.toARGB32(),
    ),
  );

  await habitProvider.saveHabit(
    Habit(
      id: 'habit_meditate',
      name: 'Meditation',
      frequency: HabitFrequency.daily,
      stackName: 'Morning Routine',
      stackOrder: 2,
      completions: [dayStart.add(const Duration(hours: 7, minutes: 15))],
      colorValue: AppTheme.habitColor.toARGB32(),
    ),
  );

  await habitProvider.saveHabit(
    Habit(
      id: 'habit_read',
      name: 'Read 20 Pages',
      frequency: HabitFrequency.weeklyExact,
      targetWeekdays: [today.weekday],
      notes: 'Keep the momentum going before lunch.',
      colorValue: AppTheme.habitColor.toARGB32(),
    ),
  );

  await habitProvider.saveHabit(
    Habit(
      id: 'habit_workout',
      name: 'Workout',
      frequency: HabitFrequency.weeklyFlexible,
      targetDaysPerWeek: 3,
      completions: [
        dayStart
            .subtract(const Duration(days: 3))
            .add(const Duration(hours: 18)),
        dayStart
            .subtract(const Duration(days: 2))
            .add(const Duration(hours: 18)),
        dayStart
            .subtract(const Duration(days: 1))
            .add(const Duration(hours: 18)),
      ],
      colorValue: AppTheme.habitColor.toARGB32(),
    ),
  );

  await taskProvider.addTask(
    Task(
      id: 'task_proposal',
      title: 'Finish project proposal',
      description: 'Polish the scope, milestones, and budget notes.',
      dueDate: dayStart.subtract(const Duration(hours: 3)),
      location: 'Home office',
      priority: Priority.high,
      size: Size.large,
      category: 'Work',
      createdAt: dayStart.subtract(const Duration(days: 2)),
      subtasks: [
        Subtask(id: 'task_proposal_1', title: 'Review outline'),
        Subtask(id: 'task_proposal_2', title: 'Check budget table'),
      ],
    ),
  );

  await taskProvider.addTask(
    Task(
      id: 'task_client',
      title: 'Reply to client email',
      description:
          'Answer the questions from yesterday'
          's review.',
      dueDate: dayStart.add(const Duration(hours: 23)),
      location: 'Inbox',
      priority: Priority.veryHigh,
      size: Size.small,
      category: 'Communication',
      createdAt: dayStart.subtract(const Duration(days: 1)),
      reminders: [dayStart.add(const Duration(hours: 9, minutes: 30))],
    ),
  );

  await taskProvider.addTask(
    Task(
      id: 'task_retro',
      title: 'Prepare sprint retro',
      description: 'Collect highlights and blockers for Friday.',
      dueDate: dayStart.add(const Duration(days: 2)),
      location: 'Meeting room',
      category: 'Work',
      createdAt: dayStart,
    ),
  );

  await taskProvider.addTask(
    Task(
      id: 'task_backup',
      title: 'Back up phone photos',
      description: 'Archive the last trip and mark it complete.',
      dueDate: dayStart.subtract(const Duration(days: 1)),
      location: 'Laptop',
      priority: Priority.low,
      size: Size.small,
      category: 'Personal',
      isCompleted: true,
      completedAt: dayStart.subtract(const Duration(hours: 20)),
      createdAt: dayStart.subtract(const Duration(days: 4)),
    ),
  );

  await focusProvider.createTag('Deep Work', Colors.indigo);
  final deepWorkTag = focusProvider.selectedTag!;

  final flexibleMode = focusProvider.modes.firstWhere(
    (mode) => mode.id == 'system_flexible',
  );
  focusProvider.setActiveMode(flexibleMode);
  focusProvider.setFlexibleDuration(50);

  await focusProvider.logPastSession(
    mode: flexibleMode,
    startTime: dayStart.add(const Duration(hours: 8, minutes: 15)),
    endTime: dayStart.add(const Duration(hours: 9)),
    tag: focusProvider.selectedTag,
  );

  await focusProvider.logPastSession(
    mode: flexibleMode,
    startTime: dayStart.add(const Duration(hours: 13, minutes: 30)),
    endTime: dayStart.add(const Duration(hours: 14, minutes: 10)),
    tag: deepWorkTag,
  );

  await focusProvider.logPastSession(
    mode: FocusMode.classicPomodoro(),
    startTime: dayStart
        .subtract(const Duration(days: 1))
        .add(const Duration(hours: 16)),
    endTime: dayStart
        .subtract(const Duration(days: 1))
        .add(const Duration(hours: 16, minutes: 30)),
    tag: deepWorkTag,
  );

  final completedTasks = taskProvider.tasks.where((t) => t.isCompleted).length;
  final totalHabitCompletions = habitProvider.habits.fold(
    0,
    (sum, h) => sum + h.completions.length,
  );
  final totalFocusMinutes = focusProvider.history.fold(
    0,
    (sum, s) => sum + (s.totalSecondsFocused ~/ 60),
  );

  final seededXp =
      (completedTasks * ExperienceEngine.xpPerTask) +
      (totalHabitCompletions * ExperienceEngine.xpPerHabit) +
      (totalFocusMinutes * ExperienceEngine.xpPerFocusMin);

  await userProvider.addXp(seededXp);
}

void main() {
  final binding = IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('Generate App Screenshots', (WidgetTester tester) async {
    // 1. Start the app
    app.main();

    await _waitForCondition(tester, () {
      if (find.byType(Scaffold).evaluate().isEmpty) {
        return false;
      }

      final focusProvider = Provider.of<FocusProvider>(
        tester.element(find.byType(Scaffold).first),
        listen: false,
      );
      return focusProvider.activeMode != null;
    });

    // Settle any remaining initial entry animations
    await _pumpDuration(tester, const Duration(milliseconds: 1000));

    await _resetPersistentData();

    // 2. Inject Dummy Data - capture context after async operations
    final BuildContext context = tester.element(find.byType(Scaffold).first);
    if (context.mounted) {
      await _seedMockData(context);
    }

    // Wait for the UI to visually update with the new data
    await binding.convertFlutterSurfaceToImage();
    await _pumpDuration(tester, const Duration(milliseconds: 1000));

    // 3. Capture the major app screens.
    await binding.takeScreenshot('01_home_screen');

    await _tapBottomNavItem(tester, 'Focus');
    await binding.takeScreenshot('02_focus_screen');

    // Navigate to Focus Modes from Focus screen by tapping the mode name
    await _tapText(tester, 'FLEXIBLE');
    await binding.takeScreenshot('06_focus_modes_screen');

    // Go back to Focus tab
    await tester.pageBack();
    await _pumpDuration(tester, const Duration(milliseconds: 1500));

    // Navigate to Profile screen
    await _tapBottomNavItem(tester, 'Profile');

    // Navigate to Stats from Profile screen (bar chart icon)
    await _tapIconButton(tester, Icons.bar_chart);
    await binding.takeScreenshot('07_stats_screen');

    // Go back to Profile tab
    await tester.pageBack();
    await _pumpDuration(tester, const Duration(milliseconds: 1500));

    await _tapBottomNavItem(tester, 'Tasks');
    await binding.takeScreenshot('03_tasks_screen');

    // Navigate to Calendar from Tasks
    await _tapIconButton(tester, Icons.calendar_today);
    await binding.takeScreenshot('08_calendar_screen');

    // Go back to Tasks
    await tester.pageBack();
    await _pumpDuration(tester, const Duration(milliseconds: 1500));

    // Tap on the first task to see detail view (the overdue proposal task)
    await _tapText(tester, 'Finish project proposal');
    await binding.takeScreenshot('09_task_detail_screen');

    // Go back to Tasks
    await tester.pageBack();
    await _pumpDuration(tester, const Duration(milliseconds: 1500));

    await _tapBottomNavItem(tester, 'Habits');
    await binding.takeScreenshot('04_habits_screen');

    // Tap on the first habit to see detail view
    await _tapText(tester, 'Drink Water');
    await binding.takeScreenshot('10_habit_detail_screen');

    // Go back to Habits
    await tester.pageBack();
    await _pumpDuration(tester, const Duration(milliseconds: 1500));

    await _tapBottomNavItem(tester, 'Profile');
    await binding.takeScreenshot('05_profile_screen');
  });
}
