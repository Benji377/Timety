import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/focus_provider.dart';
import '../data/focus_mode.dart';

class AddFocusModeScreen extends StatefulWidget {
  const AddFocusModeScreen({super.key});

  @override
  State<AddFocusModeScreen> createState() => _AddFocusModeScreenState();
}

class _AddFocusModeScreenState extends State<AddFocusModeScreen> {
  final _titleController = TextEditingController();
  final List<FocusStep> _steps = [];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Create Focus Mode'),
        actions: [
          TextButton(
            onPressed: (_titleController.text.isNotEmpty && _steps.isNotEmpty) ? _saveMode : null,
            child: const Text('Save'),
          ),
        ],
      ),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            TextField(
              controller: _titleController,
              decoration: const InputDecoration(labelText: 'Title', border: OutlineInputBorder()),
              onChanged: (_) => setState(() {}),
            ),
            const SizedBox(height: 24),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text('Steps', style: Theme.of(context).textTheme.titleMedium),
                ElevatedButton.icon(
                  onPressed: () => setState(() => _steps.add(FocusStep(durationMins: 25, type: FocusStepType.focus))),
                  icon: const Icon(Icons.add),
                  label: const Text('Add Step'),
                ),
              ],
            ),
            const SizedBox(height: 16),
            Expanded(
              child: _steps.isEmpty
                  ? const Center(child: Text('No steps added yet.'))
                  : ListView.builder(
                      itemCount: _steps.length,
                      itemBuilder: (context, index) {
                        return _StepItem(
                          step: _steps[index],
                          onDelete: () => setState(() => _steps.removeAt(index)),
                          onUpdate: (updated) => setState(() => _steps[index] = updated),
                        );
                      },
                    ),
            ),
          ],
        ),
      ),
    );
  }

  void _saveMode() {
    final mode = FocusMode(
      title: _titleController.text,
      isCustom: true,
      steps: _steps,
    );
    context.read<FocusProvider>().addFocusMode(mode);
    Navigator.pop(context);
  }
}

class _StepItem extends StatelessWidget {
  final FocusStep step;
  final VoidCallback onDelete;
  final Function(FocusStep) onUpdate;

  const _StepItem({required this.step, required this.onDelete, required this.onUpdate});

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.only(bottom: 8),
      child: Padding(
        padding: const EdgeInsets.all(12.0),
        child: Column(
          children: [
            Row(
              children: [
                CircleAvatar(
                  radius: 16,
                  child: Text(step.type.name[0].toUpperCase()),
                ),
                const SizedBox(width: 12),
                Expanded(child: Text(step.type.name.toUpperCase(), style: const TextStyle(fontWeight: FontWeight.bold))),
                IconButton(icon: const Icon(Icons.delete, color: Colors.red, size: 20), onPressed: onDelete),
              ],
            ),
            Row(
              children: [
                Expanded(
                  child: DropdownButton<FocusStepType>(
                    value: step.type,
                    isExpanded: true,
                    items: FocusStepType.values.map((t) => DropdownMenuItem(value: t, child: Text(t.name))).toList(),
                    onChanged: (val) => onUpdate(FocusStep(durationMins: step.durationMins, type: val!, behavior: step.behavior)),
                  ),
                ),
                const SizedBox(width: 16),
                SizedBox(
                  width: 60,
                  child: TextField(
                    keyboardType: TextInputType.number,
                    decoration: const InputDecoration(labelText: 'Mins'),
                    controller: TextEditingController(text: step.durationMins.toString()),
                    onChanged: (val) => onUpdate(FocusStep(durationMins: int.tryParse(val) ?? 0, type: step.type, behavior: step.behavior)),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
