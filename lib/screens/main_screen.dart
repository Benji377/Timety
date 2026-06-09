import 'package:flutter/material.dart';
import '../l10n/app_localizations.dart';
import '../theme/app_theme.dart';
import 'habit/habit_list_screen.dart';
import 'home_screen.dart';
import 'focus/focus_screen.dart';
import 'task/task_list_screen.dart';
import 'user_screen.dart';

/// The root navigation screen containing the BottomNavigationBar.
class MainScreen extends StatefulWidget {
  const MainScreen({super.key});

  @override
  State<MainScreen> createState() => _MainScreenState();
}

class _MainScreenState extends State<MainScreen> {
  int _currentIndex = 0;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
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
      // --- MAIN CONTENT AREA ---
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
      // --- BOTTOM NAVIGATION BAR ---
      bottomNavigationBar: BottomNavigationBar(
        currentIndex: _currentIndex,
        onTap: _switchTab,
        type: BottomNavigationBarType.fixed,
        selectedItemColor: selectedColor,
        unselectedItemColor: Theme.of(context).colorScheme.onSurfaceVariant,
        items: [
          BottomNavigationBarItem(
            icon: const Icon(Icons.home_outlined),
            activeIcon: const Icon(Icons.home, color: AppTheme.warningAccent),
            label: l10n.navigationHome,
          ),
          BottomNavigationBarItem(
            icon: const Icon(Icons.coffee_outlined),
            activeIcon: const Icon(Icons.coffee, color: AppTheme.focusColor),
            label: l10n.navigationFocus,
          ),
          BottomNavigationBarItem(
            icon: const Icon(Icons.task_outlined),
            activeIcon: const Icon(Icons.task, color: AppTheme.taskColor),
            label: l10n.navigationTasks,
          ),
          BottomNavigationBarItem(
            icon: const Icon(Icons.alarm_outlined),
            activeIcon: const Icon(Icons.alarm, color: AppTheme.habitColor),
            label: l10n.navigationHabits,
          ),
          BottomNavigationBarItem(
            icon: const Icon(Icons.person_outline),
            activeIcon: const Icon(Icons.person, color: AppTheme.userColor),
            label: l10n.navigationProfile,
          ),
        ],
      ),
    );
  }

  // Helper method to switch tabs
  void _switchTab(int index) {
    if (!mounted) return;
    setState(() {
      _currentIndex = index;
    });
  }
}
