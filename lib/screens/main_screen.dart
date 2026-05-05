import 'package:flutter/material.dart';
import 'home_screen.dart';
import 'focus_screen.dart';
import 'task_list_screen.dart';
import 'settings_screen.dart';

class MainScreen extends StatefulWidget {
  const MainScreen({super.key});

  @override
  State<MainScreen> createState() => _MainScreenState();
}

class _MainScreenState extends State<MainScreen> {
  int _currentIndex = 0;

  // Helper method to switch tabs
  void _switchTab(int index) {
    setState(() {
      _currentIndex = index;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      // Move the pages directly into the IndexedStack
      body: IndexedStack(
        index: _currentIndex, 
        children: [
          // Pass the tab-switching function down to the HomeScreen
          HomeScreen(onNavigateToFocus: () => _switchTab(1)), 
          const FocusScreen(),
          const TaskListScreen(),
          const SettingsScreen(),
        ]
      ),
      bottomNavigationBar: BottomNavigationBar(
        currentIndex: _currentIndex,
        onTap: _switchTab, // Simplify the onTap call
        type: BottomNavigationBarType.fixed,
        selectedItemColor: Theme.of(context).colorScheme.primary,
        unselectedItemColor: Colors.grey,
        items: const [
          BottomNavigationBarItem(
            icon: Icon(Icons.home_outlined),
            activeIcon: Icon(Icons.home),
            label: 'Home',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.coffee_outlined),
            activeIcon: Icon(Icons.coffee),
            label: 'Focus',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.task_outlined),
            activeIcon: Icon(Icons.task),
            label: 'Tasks',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.settings_outlined),
            activeIcon: Icon(Icons.settings),
            label: 'Settings',
          ),
        ],
      ),
    );
  }
}