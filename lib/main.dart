import 'package:flutter/material.dart';
import 'package:hive_flutter/hive_flutter.dart';
import 'data/task.dart';
import 'screens/task_list_screen.dart';
import 'package:provider/provider.dart';
import 'providers/task_provider.dart';
import 'data/task_repository_hive.dart';
import 'services/notification_service.dart';
import 'theme/app_theme.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  await Hive.initFlutter();
  
  // 2. Register our generated Adapters
  Hive.registerAdapter(TaskAdapter());
  Hive.registerAdapter(PriorityAdapter());
  Hive.registerAdapter(SizeAdapter());


  await NotificationService.instance.init();

  runApp(const TimetyApp());
}

class TimetyApp extends StatelessWidget {
  const TimetyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return ChangeNotifierProvider(
      create: (_) => TaskProvider(repository: HiveTaskRepository()),
      child: MaterialApp(
        title: 'Timety',
        theme: AppTheme.lightTheme,
        home: const TodoListScreen(),
      ),
    );
  }
}