import 'package:flutter/material.dart';
import 'screens/task_list_screen.dart';
import 'package:provider/provider.dart';
import 'providers/task_provider.dart';
import 'data/task_repository_local.dart';
import 'theme/app_theme.dart';

void main() {
  runApp(const TimetyApp());
}

class TimetyApp extends StatelessWidget {
  const TimetyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return ChangeNotifierProvider(
      create: (_) => TaskProvider(repository: LocalTaskRepository()),
      child: MaterialApp(
        title: 'Flutter Todo',
        theme: AppTheme.lightTheme,
        home: const TodoListScreen(),
      ),
    );
  }
}