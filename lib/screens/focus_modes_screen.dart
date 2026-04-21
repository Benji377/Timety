import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/focus_provider.dart';
import 'add_focus_mode_screen.dart';

class FocusModesScreen extends StatelessWidget {
  const FocusModesScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final focusProvider = context.watch<FocusProvider>();
    final focusModes = focusProvider.focusModes;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Focus Modes'),
        actions: [
          IconButton(
            icon: const Icon(Icons.add),
            onPressed: () => Navigator.push(
              context,
              MaterialPageRoute(builder: (_) => const AddFocusModeScreen()),
            ),
          ),
        ],
      ),
      body: ListView.builder(
        padding: const EdgeInsets.all(16),
        itemCount: focusModes.length,
        itemBuilder: (context, index) {
          final mode = focusModes[index];
          return Card(
            margin: const EdgeInsets.only(bottom: 12),
            child: ListTile(
              title: Text(mode.title, style: const TextStyle(fontWeight: FontWeight.bold)),
              subtitle: Text('${mode.steps.length} steps: ${mode.steps.map((s) => s.type.name).join(' → ')}'),
              trailing: mode.isCustom ? IconButton(
                icon: const Icon(Icons.delete, color: Colors.red),
                onPressed: () => focusProvider.deleteFocusMode(mode.id!),
              ) : null,
              onTap: () {
                // Return selected mode to previous screen if needed, 
                // but usually we just manage them here
              },
            ),
          );
        },
      ),
    );
  }
}
