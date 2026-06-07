import 'package:flutter/material.dart';
import 'package:hive_flutter/hive_flutter.dart';
import 'package:provider/provider.dart';
import 'l10n/app_localizations.dart';

import 'screens/main_screen.dart';
import 'data/user/user.dart';
import 'data/user/user_repository_hive.dart';
import 'providers/settings_provider.dart';
import 'providers/user_provider.dart';
import 'theme/app_theme.dart';
import 'services/notification_service.dart';

// Habits
import 'data/habit/habit_models.dart';
import 'data/habit/habit_repository_hive.dart';
import 'providers/habit_provider.dart';

// Tasks
import 'data/task/task.dart';
import 'data/task/task_repository_hive.dart';
import 'providers/task_provider.dart';

// Focus
import 'data/focus/focus_models.dart';
import 'data/focus/focus_repository_hive.dart';
import 'providers/focus_provider.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  await Hive.initFlutter();

  // Register Task Adapters
  Hive.registerAdapter(TaskAdapter());
  Hive.registerAdapter(PriorityAdapter());
  Hive.registerAdapter(SizeAdapter());
  Hive.registerAdapter(SubtaskAdapter());

  // Register Focus Adapters
  Hive.registerAdapter(FocusModeTypeAdapter());
  Hive.registerAdapter(FocusModeAdapter());
  Hive.registerAdapter(DistractionAdapter());
  Hive.registerAdapter(FocusSessionAdapter());
  Hive.registerAdapter(PhaseTypeAdapter());
  Hive.registerAdapter(SessionPhaseAdapter());
  Hive.registerAdapter(FocusTagAdapter());
  Hive.registerAdapter(FocusTargetTypeAdapter());

  // Register Habit Adapters
  Hive.registerAdapter(HabitFrequencyAdapter());
  Hive.registerAdapter(HabitAdapter());

  // Register User Adapters
  Hive.registerAdapter(UserProfileAdapter());

  await NotificationService.instance.init();

  runApp(const TimetyApp());
}

class TimetyApp extends StatelessWidget {
  const TimetyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MultiProvider(
      providers: [
        ChangeNotifierProvider(create: (_) => SettingsProvider()),
        ChangeNotifierProxyProvider<SettingsProvider, HabitProvider>(
          create: (_) =>
              HabitProvider(repository: HiveHabitRepository())..loadHabits(),
          update: (_, settings, habitProvider) {
            // Whenever settings change, pass them into the Habit Provider
            habitProvider?.updateSettings(settings);
            return habitProvider!;
          },
        ),
        ChangeNotifierProxyProvider<SettingsProvider, TaskProvider>(
          create: (_) =>
              TaskProvider(repository: HiveTaskRepository())..loadTasks(),
          update: (_, settings, taskProvider) {
            taskProvider?.updateSettings(settings);
            return taskProvider!;
          },
        ),
        ChangeNotifierProvider(
          create: (_) => FocusProvider(repository: HiveFocusRepository()),
        ),
        ChangeNotifierProvider(
          create: (_) => UserProvider(repository: HiveUserRepository()),
        ),
      ],
      child: Consumer<SettingsProvider>(
        builder: (context, settings, _) {
          return MaterialApp(
            debugShowCheckedModeBanner: false,
            title: 'Timety',
            theme: AppTheme.buildTheme(brightness: Brightness.light),
            darkTheme: AppTheme.buildTheme(brightness: Brightness.dark),
            themeMode: settings.themeMode,
            localizationsDelegates: AppLocalizations.localizationsDelegates,
            supportedLocales: AppLocalizations.supportedLocales,
            locale: settings.appLocale,
            home: const MainScreen(),
          );
        },
      ),
    );
  }
}
