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
      appBar: AppBar(title: const Text('Settings')),
      body: ListView(
        children: [
          _buildSectionHeader(context, 'Account'),
          ListTile(
            title: const Text('Name'),
            subtitle: Text(user?.name ?? 'Hero'),
            leading: const Icon(Icons.person),
            onTap: () => _editName(context, userProvider),
          ),
          ListTile(
            title: const Text('Daily Focus Target'),
            subtitle: Text('${(user?.dailyFocusTarget ?? 7200000) ~/ 60000} minutes'),
            leading: const Icon(Icons.adjust),
            onTap: () => _editTarget(context, userProvider),
          ),
          _buildSectionHeader(context, 'Appearance'),
          SwitchListTile(
            title: const Text('Dark Mode'),
            value: userProvider.isDarkMode,
            onChanged: (_) => userProvider.toggleDarkMode(),
            secondary: const Icon(Icons.brightness_4),
          ),
          _buildSectionHeader(context, 'Categories'),
          ...taskProvider.categories.map((cat) => ListTile(
            title: Text(cat.name),
            leading: Icon(Icons.circle, color: Color(int.parse(cat.colorHex.replaceAll('#', '0xFF')))),
            trailing: IconButton(
              icon: const Icon(Icons.delete, size: 20),
              onPressed: () => taskProvider.deleteCategory(cat.id!),
            ),
          )),
          ListTile(
            title: const Text('Add Category', style: TextStyle(color: Colors.blue)),
            leading: const Icon(Icons.add, color: Colors.blue),
            onTap: () => _addCategory(context, taskProvider),
          ),
          _buildSectionHeader(context, 'About'),
          const ListTile(
            title: Text('Version'),
            subtitle: Text('1.0.0 (Migrated)'),
            leading: Icon(Icons.info_outline),
          ),
        ],
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
        content: TextField(controller: controller, decoration: const InputDecoration(labelText: 'Name')),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context), child: const Text('CANCEL')),
          TextButton(
            onPressed: () {
              if (controller.text.isNotEmpty) {
                provider.updateUser(provider.user!.copyWith(name: controller.text));
              }
              Navigator.pop(context);
            },
            child: const Text('SAVE'),
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
          TextButton(onPressed: () => Navigator.pop(context), child: const Text('CANCEL')),
          TextButton(
            onPressed: () {
              final mins = int.tryParse(controller.text);
              if (mins != null && mins > 0) {
                provider.updateUser(provider.user!.copyWith(dailyFocusTarget: mins * 60 * 1000));
              }
              Navigator.pop(context);
            },
            child: const Text('SAVE'),
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
        content: TextField(controller: controller, decoration: const InputDecoration(labelText: 'Category Name')),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context), child: const Text('CANCEL')),
          TextButton(
            onPressed: () {
              if (controller.text.isNotEmpty) {
                provider.addCategory(Category(name: controller.text, colorHex: '#4285F4', iconName: 'default'));
              }
              Navigator.pop(context);
            },
            child: const Text('ADD'),
          ),
        ],
      ),
    );
  }
}
