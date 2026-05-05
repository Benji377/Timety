import 'package:flutter/material.dart';
import 'package:hive_flutter/hive_flutter.dart';
import 'package:provider/provider.dart';

import 'package:timety/screens/main_screen.dart';
import 'providers/settings_provider.dart';
import 'theme/app_theme.dart';
import 'services/notification_service.dart';

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

  // Register Focus Adapters
  Hive.registerAdapter(FocusModeTypeAdapter());
  Hive.registerAdapter(FocusModeAdapter());
  Hive.registerAdapter(DistractionAdapter());
  Hive.registerAdapter(FocusSessionAdapter());
  Hive.registerAdapter(PhaseTypeAdapter());
  Hive.registerAdapter(SessionPhaseAdapter());
  Hive.registerAdapter(FocusTagAdapter());

  await NotificationService.instance.init();
  await NotificationService.instance.scheduleDailyMotivation();

  runApp(const TimetyApp());
}

class TimetyApp extends StatelessWidget {
  const TimetyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MultiProvider(
      providers: [
        ChangeNotifierProvider(
          create: (_) => TaskProvider(repository: HiveTaskRepository()),
        ),
        ChangeNotifierProvider(
          create: (_) => FocusProvider(repository: HiveFocusRepository()),
        ),
        ChangeNotifierProvider(create: (_) => SettingsProvider()),
      ],
      child: Consumer<SettingsProvider>(
        builder: (context, settings, _) {
          return MaterialApp(
            title: 'Timety',
            theme: AppTheme.buildTheme(
              seedColor: settings.seedColor,
              brightness: Brightness.light,
            ),
            darkTheme: AppTheme.buildTheme(
              seedColor: settings.seedColor,
              brightness: Brightness.dark,
            ),
            themeMode: settings.themeMode,
            home: const MainScreen(),
          );
        },
      ),
    );
  }
}
