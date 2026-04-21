import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/user_provider.dart';
import '../providers/task_provider.dart';
import '../data/category.dart';

class SettingsScreen extends StatelessWidget {
  const SettingsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final userProvider = context.watch<UserProvider>();
    final taskProvider = context.watch<TaskProvider>();
    final user = userProvider.user;

    return Scaffold(
      body: SafeArea(
        child: Column(
          children: [
            // Header
            Padding(
              padding: const EdgeInsets.all(16),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    'Settings',
                    style: Theme.of(context).textTheme.headlineSmall,
                  ),
                  IconButton(
                    onPressed: Navigator.of(context).pop,
                    icon: const Icon(Icons.close),
                  ),
                ],
              ),
            ),
            // Content
            Expanded(
              child: ListView(
                children: [
                  _buildSectionHeader(context, 'Account'),
                  ListTile(
                    title: const Text('Name'),
                    subtitle: Text(user?.name ?? 'User'),
                    leading: const Icon(Icons.person),
                    onTap: () => _editName(context, userProvider),
                  ),
                  ListTile(
                    title: const Text('Daily Focus Target'),
                    subtitle: Text(
                      '${(user?.dailyFocusTarget ?? 7200000) ~/ 60000} minutes',
                    ),
                    leading: const Icon(Icons.adjust),
                    onTap: () => _editTarget(context, userProvider),
                  ),
                  const Divider(),

                  _buildSectionHeader(context, 'Streak & Focus'),
                  ListTile(
                    title: const Text('Minimum Daily Streak'),
                    subtitle: Text(
                      '${user?.minStreakMinutes ?? 1} minute(s) of focus required',
                    ),
                    leading: const Icon(Icons.local_fire_department),
                    onTap: () => _editMinStreak(context, userProvider),
                  ),
                  ListTile(
                    title: const Text('Max Focus Duration'),
                    subtitle: Text(
                      '${user?.maxFocusSessionDuration ?? 120} minutes',
                    ),
                    leading: const Icon(Icons.timer),
                    onTap: () => _editMaxDuration(context, userProvider),
                  ),
                  const Divider(),

                  _buildSectionHeader(context, 'Appearance'),
                  SwitchListTile(
                    title: const Text('Dark Mode'),
                    value: userProvider.isDarkMode,
                    onChanged: (_) => userProvider.toggleDarkMode(),
                    secondary: const Icon(Icons.brightness_4),
                  ),
                  const Divider(),

                  _buildSectionHeader(context, 'Categories'),
                  ...taskProvider.categories.map(
                    (cat) => ListTile(
                      title: Text(cat.name),
                      leading: Icon(
                        Icons.circle,
                        color: Color(
                          int.parse(cat.colorHex.replaceAll('#', '0xFF')),
                        ),
                      ),
                      trailing: IconButton(
                        icon: const Icon(Icons.delete, size: 20),
                        onPressed: () => taskProvider.deleteCategory(cat.id!),
                      ),
                    ),
                  ),
                  ListTile(
                    title: const Text('Add Category'),
                    leading: const Icon(Icons.add),
                    onTap: () => _addCategory(context, taskProvider),
                  ),
                  const Divider(),

                  _buildSectionHeader(context, 'About'),
                  const ListTile(
                    title: Text('Version'),
                    subtitle: Text('1.0.0'),
                    leading: Icon(Icons.info_outline),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildSectionHeader(BuildContext context, String title) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 24, 16, 8),
      child: Text(
        title.toUpperCase(),
        style: Theme.of(context).textTheme.labelSmall?.copyWith(
          color: Theme.of(context).colorScheme.primary,
          fontWeight: FontWeight.bold,
        ),
      ),
    );
  }

  void _editName(BuildContext context, UserProvider provider) {
    final controller = TextEditingController(text: provider.user?.name);
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Edit Name'),
        content: TextField(
          controller: controller,
          decoration: const InputDecoration(labelText: 'Name'),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Cancel'),
          ),
          TextButton(
            onPressed: () {
              if (controller.text.isNotEmpty) {
                provider.updateUser(
                  provider.user!.copyWith(name: controller.text),
                );
              }
              Navigator.pop(context);
            },
            child: const Text('Save'),
          ),
        ],
      ),
    );
  }

  void _editTarget(BuildContext context, UserProvider provider) {
    final currentMins = (provider.user?.dailyFocusTarget ?? 7200000) ~/ 60000;
    final controller = TextEditingController(text: currentMins.toString());
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Daily Focus Target (minutes)'),
        content: TextField(
          controller: controller,
          keyboardType: TextInputType.number,
          decoration: const InputDecoration(labelText: 'Minutes'),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Cancel'),
          ),
          TextButton(
            onPressed: () {
              final mins = int.tryParse(controller.text);
              if (mins != null && mins > 0) {
                provider.updateUser(
                  provider.user!.copyWith(dailyFocusTarget: mins * 60 * 1000),
                );
              }
              Navigator.pop(context);
            },
            child: const Text('Save'),
          ),
        ],
      ),
    );
  }

  void _editMinStreak(BuildContext context, UserProvider provider) {
    final controller = TextEditingController(
      text: (provider.user?.minStreakMinutes ?? 1).toString(),
    );
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Minimum Daily Streak'),
        content: TextField(
          controller: controller,
          keyboardType: TextInputType.number,
          decoration: const InputDecoration(
            labelText: 'Minutes of focus required',
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Cancel'),
          ),
          TextButton(
            onPressed: () {
              final mins = int.tryParse(controller.text);
              if (mins != null && mins > 0) {
                provider.updateUser(
                  provider.user!.copyWith(minStreakMinutes: mins),
                );
              }
              Navigator.pop(context);
            },
            child: const Text('Save'),
          ),
        ],
      ),
    );
  }

  void _editMaxDuration(BuildContext context, UserProvider provider) {
    final controller = TextEditingController(
      text: (provider.user?.maxFocusSessionDuration ?? 120).toString(),
    );
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Max Focus Session Duration'),
        content: TextField(
          controller: controller,
          keyboardType: TextInputType.number,
          decoration: const InputDecoration(labelText: 'Minutes'),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Cancel'),
          ),
          TextButton(
            onPressed: () {
              final mins = int.tryParse(controller.text);
              if (mins != null && mins > 0) {
                provider.updateUser(
                  provider.user!.copyWith(maxFocusSessionDuration: mins),
                );
              }
              Navigator.pop(context);
            },
            child: const Text('Save'),
          ),
        ],
      ),
    );
  }

  void _addCategory(BuildContext context, TaskProvider provider) {
    final controller = TextEditingController();
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('New Category'),
        content: TextField(
          controller: controller,
          decoration: const InputDecoration(labelText: 'Category Name'),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Cancel'),
          ),
          TextButton(
            onPressed: () {
              if (controller.text.isNotEmpty) {
                provider.addCategory(
                  Category(
                    name: controller.text,
                    colorHex: '#4285F4',
                    iconName: 'default',
                  ),
                );
              }
              Navigator.pop(context);
            },
            child: const Text('Add'),
          ),
        ],
      ),
    );
  }
}
