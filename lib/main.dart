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
      child: const TimetyApp(),
    ),
  );
}

class TimetyApp extends StatelessWidget {
  const TimetyApp({super.key});

  @override
  Widget build(BuildContext context) {
    final userProvider = context.watch<UserProvider>();
    
    return MaterialApp(
      title: 'Timety',
      theme: ThemeData(
        useMaterial3: true,
        colorSchemeSeed: Colors.blue,
        brightness: Brightness.light,
      ),
      darkTheme: ThemeData(
        useMaterial3: true,
        colorSchemeSeed: Colors.blue,
        brightness: Brightness.dark,
      ),
      themeMode: userProvider.isDarkMode ? ThemeMode.dark : ThemeMode.light,
      home: const HomeScreen(),
    );
  }
}
