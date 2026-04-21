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
                    'Focus Modes',
                    style: Theme.of(context).textTheme.headlineSmall,
                  ),
                  IconButton(
                    onPressed: Navigator.of(context).pop,
                    icon: const Icon(Icons.close),
                  ),
                ],
              ),
            ),

            // Modes List
            Expanded(
              child: ListView.builder(
                padding: const EdgeInsets.symmetric(horizontal: 16),
                itemCount: focusModes.length,
                itemBuilder: (context, index) {
                  final mode = focusModes[index];
                  final totalDuration = mode.steps.fold<int>(
                    0,
                    (sum, step) => sum + step.durationMins,
                  );

                  return Card(
                    margin: const EdgeInsets.only(bottom: 12),
                    child: ListTile(
                      title: Text(
                        mode.title,
                        style: const TextStyle(fontWeight: FontWeight.bold),
                      ),
                      subtitle: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            '${mode.steps.length} steps • ${totalDuration} min total',
                            style: Theme.of(context).textTheme.bodySmall,
                          ),
                          const SizedBox(height: 4),
                          Wrap(
                            spacing: 4,
                            children: mode.steps.map((step) {
                              return Chip(
                                label: Text(
                                  step.type.name,
                                  style: const TextStyle(fontSize: 10),
                                ),
                                padding: EdgeInsets.zero,
                              );
                            }).toList(),
                          ),
                        ],
                      ),
                      trailing: mode.isCustom
                          ? IconButton(
                              icon: const Icon(Icons.delete, color: Colors.red),
                              onPressed: () => showDialog(
                                context: context,
                                builder: (context) => AlertDialog(
                                  title: const Text('Delete Mode'),
                                  content: const Text(
                                    'Are you sure you want to delete this focus mode?',
                                  ),
                                  actions: [
                                    TextButton(
                                      onPressed: Navigator.of(context).pop,
                                      child: const Text('Cancel'),
                                    ),
                                    TextButton(
                                      onPressed: () {
                                        focusProvider.deleteFocusMode(mode.id!);
                                        Navigator.of(context).pop();
                                      },
                                      child: const Text('Delete'),
                                    ),
                                  ],
                                ),
                              ),
                            )
                          : null,
                    ),
                  );
                },
              ),
            ),

            // Add Mode Button
            Padding(
              padding: const EdgeInsets.all(16),
              child: ElevatedButton.icon(
                onPressed: () => Navigator.push(
                  context,
                  MaterialPageRoute(builder: (_) => const AddFocusModeScreen()),
                ),
                icon: const Icon(Icons.add),
                label: const Text('Add Custom Mode'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
