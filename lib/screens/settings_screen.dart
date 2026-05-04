import 'package:flutter/material.dart';

class SettingsScreen extends StatelessWidget {
  const SettingsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Settings')),
      body: ListView(
        children: [
          const ListTile(
            title: Text('Data Management'),
            subtitle: Text('Export and backup your tasks'),
          ),
          ListTile(
            leading: const Icon(Icons.download),
            title: const Text('Export to CSV (Coming Soon)'),
            onTap: () {},
          ),
          ListTile(
            leading: const Icon(Icons.code),
            title: const Text('Export to JSON (Coming Soon)'),
            onTap: () {},
          ),
        ],
      ),
    );
  }
}