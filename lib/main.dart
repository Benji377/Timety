import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:timezone/data/latest.dart' as tz;
import 'utils/notification_helper.dart';
import 'data/main_repository.dart';
import 'providers/user_provider.dart';
import 'providers/task_provider.dart';
import 'providers/focus_provider.dart';
import 'providers/stats_provider.dart';
import 'screens/home_screen.dart';
import 'theme/app_theme.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  tz.initializeTimeZones();
  await NotificationHelper.init();

  final repository = MainRepository();

  runApp(
    MultiProvider(
      providers: [
        Provider<MainRepository>.value(value: repository),
        ChangeNotifierProvider(create: (_) => UserProvider(repository)),
        ChangeNotifierProvider(create: (_) => TaskProvider(repository)),
        ChangeNotifierProvider(create: (_) => FocusProvider(repository)),
        ChangeNotifierProvider(create: (_) => StatsProvider(repository)),
      ],
      child: const TimetyRoot(),
    ),
  );
}

class TimetyRoot extends StatelessWidget {
  const TimetyRoot({super.key});

  @override
  Widget build(BuildContext context) {
    return Consumer<UserProvider>(
      builder: (context, userProvider, _) {
        return TimetyApp(
          themeMode: userProvider.isDarkMode ? ThemeMode.dark : ThemeMode.light,
          home: const HomeScreen(),
        );
      },
    );
  }
}

class TimetyApp extends StatelessWidget {
  final ThemeMode themeMode;
  final Widget home;

  const TimetyApp({
    super.key,
    this.themeMode = ThemeMode.system,
    required this.home,
  });

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Timety',
      theme: TimetyTheme.light(),
      darkTheme: TimetyTheme.dark(),
      themeMode: themeMode,
      home: home,
    );
  }
}
