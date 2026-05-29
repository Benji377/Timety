import 'package:flutter/material.dart';
import '../theme/app_theme.dart';
import 'habit/habit_list_screen.dart';
import 'home_screen.dart';
import 'focus/focus_screen.dart';
import 'task/task_list_screen.dart';
import 'user_screen.dart';

class MainScreen extends StatefulWidget {
  const MainScreen({super.key});

  @override
  State<MainScreen> createState() => _MainScreenState();
}

class _MainScreenState extends State<MainScreen> {
  int _currentIndex = 0;

  // Helper method to switch tabs
  void _switchTab(int index) {
    if (!mounted) return;
    setState(() {
      _currentIndex = index;
    });
  }

  @override
  Widget build(BuildContext context) {
    // Map current tab index to its active color so labels match active icon colors
    Color selectedColor;
    switch (_currentIndex) {
      case 0:
        selectedColor = AppTheme.warningAccent;
        break;
      case 1:
        selectedColor = AppTheme.focusColor;
        break;
      case 2:
        selectedColor = AppTheme.taskColor;
        break;
      case 3:
        selectedColor = AppTheme.habitColor;
        break;
      case 4:
      default:
        selectedColor = AppTheme.userColor;
        break;
    }
    return Scaffold(
      body: IndexedStack(
        index: _currentIndex,
        children: [
          HomeScreen(onNavigateToFocus: () => _switchTab(1)),
          const FocusScreen(),
          const TaskListScreen(),
          const HabitListScreen(),
          const UserScreen(),
        ],
      ),
      bottomNavigationBar: BottomNavigationBar(
        currentIndex: _currentIndex,
        onTap: _switchTab,
        type: BottomNavigationBarType.fixed,
        selectedItemColor: selectedColor,
        unselectedItemColor: Theme.of(context).colorScheme.onSurfaceVariant,
        items: const [
          BottomNavigationBarItem(
            icon: Icon(Icons.home_outlined),
            activeIcon: Icon(Icons.home, color: AppTheme.warningAccent),
            label: 'Home',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.coffee_outlined),
            activeIcon: Icon(Icons.coffee, color: AppTheme.focusColor),
            label: 'Focus',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.task_outlined),
            activeIcon: Icon(Icons.task, color: AppTheme.taskColor),
            label: 'Tasks',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.alarm_outlined),
            activeIcon: Icon(Icons.alarm, color: AppTheme.habitColor),
            label: 'Habits',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.person_outline),
            activeIcon: Icon(Icons.person, color: AppTheme.userColor),
            label: 'Profile',
          ),
        ],
      ),
    );
  }
}
