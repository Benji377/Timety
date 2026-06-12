import 'dart:io';
import 'package:flutter_test/flutter_test.dart';
import 'package:hive/hive.dart';
import 'package:package_info_plus/package_info_plus.dart';
import 'package:shared_preferences/shared_preferences.dart';

// Import your app files here
import 'package:timety/data/task/task.dart';
import 'package:timety/data/task/task_repository_hive.dart';
import 'package:timety/data/habit/habit_models.dart';
import 'package:timety/data/habit/habit_repository_hive.dart';
import 'package:timety/data/user/user.dart';
import 'package:timety/data/user/user_repository_hive.dart';
import 'package:timety/data/focus/focus_models.dart';
import 'package:timety/services/backup_service.dart';

void main() {
  setUpAll(() async {
    // 1. Initialize a temporary directory for Hive testing
    final tempDir = await Directory.systemTemp.createTemp('hive_testing');
    Hive.init(tempDir.path);

    // 2. Register your Hive Adapters
    Hive.registerAdapter(TaskAdapter());
    Hive.registerAdapter(PriorityAdapter());
    Hive.registerAdapter(SizeAdapter());
    Hive.registerAdapter(SubtaskAdapter());
    Hive.registerAdapter(HabitAdapter());
    Hive.registerAdapter(HabitFrequencyAdapter());
    Hive.registerAdapter(UserProfileAdapter());

    // Focus adapters
    Hive.registerAdapter(FocusModeAdapter());
    Hive.registerAdapter(FocusModeTypeAdapter());
    Hive.registerAdapter(SessionPhaseAdapter());
    Hive.registerAdapter(PhaseTypeAdapter());
    Hive.registerAdapter(FocusSessionAdapter());
    Hive.registerAdapter(DistractionAdapter());
    Hive.registerAdapter(FocusTagAdapter());
    Hive.registerAdapter(FocusTargetTypeAdapter());

    // 3. Mock SharedPreferences
    SharedPreferences.setMockInitialValues({
      'themeMode': 'dark',
      'use24HourFormat': true,
    });

    // 4. Mock PackageInfo (Required for your payload metadata)
    PackageInfo.setMockInitialValues(
      appName: 'Timety',
      packageName: 'com.benji377.timety',
      version: '1.2.0',
      buildNumber: '10',
      buildSignature: '',
    );
  });

  tearDownAll(() async {
    await Hive.deleteFromDisk();
  });

  test('BackupService perfectly exports and imports data', () async {
    // ==========================================
    // PHASE 1: Populate the database
    // ==========================================

    // Create a User
    final userBox = await Hive.openBox<UserProfile>(HiveUserRepository.boxName);
    await userBox.add(
      UserProfile(
        name: 'TestUser',
        totalXp: 500,
        accountCreated: DateTime(2026),
      ),
    );

    // Create a Task
    final taskBox = await Hive.openBox<Task>(HiveTaskRepository.boxName);
    await taskBox.put(
      'task-1',
      Task(
        id: 'task-1',
        title: 'Important Test Task',
        priority: Priority.high,
        createdAt: DateTime(2026),
      ),
    );

    // Create a Habit
    final habitBox = await Hive.openBox<Habit>(HiveHabitRepository.boxName);
    await habitBox.put(
      'habit-1',
      Habit(
        id: 'habit-1',
        name: 'Drink Water',
        frequency: HabitFrequency.daily,
        createdAt: DateTime(2026),
      ),
    );

    // ==========================================
    // PHASE 2: Export Data
    // ==========================================

    // Generate the JSON payload
    final exportPayload = await BackupService.buildPayloadForTest();

    // Verify metadata was generated correctly
    expect(exportPayload['payloadType'], 'user_data');
    expect(exportPayload['appVersion'], '1.2.0');
    expect(exportPayload['tasks'], hasLength(1));
    expect(exportPayload['habits'], hasLength(1));

    // ==========================================
    // PHASE 3: Wipe the Database (Simulate app reinstall)
    // ==========================================
    await taskBox.clear();
    await habitBox.clear();
    await userBox.clear();
    final prefs = await SharedPreferences.getInstance();
    await prefs.clear();

    expect(taskBox.isEmpty, isTrue);
    expect(prefs.getBool('use24HourFormat'), isNull);

    // ==========================================
    // PHASE 4: Import Data
    // ==========================================

    // Restore from the JSON payload we generated in Phase 2
    await BackupService.restorePayloadForTest(exportPayload);

    // ==========================================
    // PHASE 5: Verify Data Integrity
    // ==========================================

    // 1. Check Preferences
    final restoredPrefs = await SharedPreferences.getInstance();
    expect(restoredPrefs.getString('themeMode'), 'dark');
    expect(restoredPrefs.getBool('use24HourFormat'), isTrue);

    // 2. Check Tasks
    expect(taskBox.length, 1);
    expect(taskBox.get('task-1')?.title, 'Important Test Task');
    expect(taskBox.get('task-1')?.priority, Priority.high);

    // 3. Check Habits
    expect(habitBox.length, 1);
    expect(habitBox.get('habit-1')?.name, 'Drink Water');

    // 4. Check User
    expect(userBox.length, 1);
    expect(userBox.getAt(0)?.name, 'TestUser');
    expect(userBox.getAt(0)?.totalXp, 500);
  });
}
