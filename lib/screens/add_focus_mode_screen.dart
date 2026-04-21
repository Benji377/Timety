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
    final isValid = _titleController.text.isNotEmpty && _steps.isNotEmpty;

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
                    'New Focus Mode',
                    style: Theme.of(context).textTheme.headlineSmall,
                  ),
                  IconButton(
                    onPressed: Navigator.of(context).pop,
                    icon: const Icon(Icons.close),
                  ),
                ],
              ),
            ),

            // Form
            Expanded(
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    TextField(
                      controller: _titleController,
                      decoration: const InputDecoration(
                        labelText: 'Mode Title *',
                        border: OutlineInputBorder(),
                        prefixIcon: Icon(Icons.label),
                      ),
                      onChanged: (_) => setState(() {}),
                    ),
                    const SizedBox(height: 24),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Text(
                          'Steps',
                          style: Theme.of(context).textTheme.titleMedium,
                        ),
                        ElevatedButton.icon(
                          onPressed: () => setState(
                            () => _steps.add(
                              FocusStep(
                                durationMins: 25,
                                type: FocusStepType.focus,
                              ),
                            ),
                          ),
                          icon: const Icon(Icons.add),
                          label: const Text('Add'),
                        ),
                      ],
                    ),
                    const SizedBox(height: 16),
                    Expanded(
                      child: _steps.isEmpty
                          ? Center(
                              child: Text(
                                'No steps added yet\nTap Add to create steps',
                                textAlign: TextAlign.center,
                                style: Theme.of(context).textTheme.bodyMedium
                                    ?.copyWith(
                                      color: Theme.of(
                                        context,
                                      ).colorScheme.outline,
                                    ),
                              ),
                            )
                          : ListView.builder(
                              itemCount: _steps.length,
                              itemBuilder: (context, index) {
                                return _StepItem(
                                  step: _steps[index],
                                  onDelete: () =>
                                      setState(() => _steps.removeAt(index)),
                                  onUpdate: (updated) =>
                                      setState(() => _steps[index] = updated),
                                );
                              },
                            ),
                    ),
                  ],
                ),
              ),
            ),

            // Action Buttons
            Padding(
              padding: const EdgeInsets.all(16),
              child: ElevatedButton(
                onPressed: isValid ? _saveMode : null,
                style: ElevatedButton.styleFrom(
                  minimumSize: const Size.fromHeight(48),
                ),
                child: const Text('Save Mode'),
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

  const _StepItem({
    required this.step,
    required this.onDelete,
    required this.onUpdate,
  });

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Chip(
                  label: Text(step.type.name.toUpperCase()),
                  avatar: CircleAvatar(
                    child: Text(step.type.name[0].toUpperCase()),
                  ),
                ),
                const Spacer(),
                IconButton(
                  icon: const Icon(Icons.delete, color: Colors.red, size: 20),
                  onPressed: onDelete,
                  visualDensity: VisualDensity.compact,
                ),
              ],
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                Expanded(
                  child: DropdownButtonFormField<FocusStepType>(
                    initialValue: step.type,
                    decoration: const InputDecoration(
                      labelText: 'Type',
                      border: OutlineInputBorder(),
                      isDense: true,
                    ),
                    items: FocusStepType.values
                        .map(
                          (t) =>
                              DropdownMenuItem(value: t, child: Text(t.name)),
                        )
                        .toList(),
                    onChanged: (val) => onUpdate(
                      FocusStep(
                        durationMins: step.durationMins,
                        type: val!,
                        behavior: step.behavior,
                      ),
                    ),
                  ),
                ),
                const SizedBox(width: 12),
                SizedBox(
                  width: 90,
                  child: TextFormField(
                    initialValue: step.durationMins.toString(),
                    keyboardType: TextInputType.number,
                    decoration: const InputDecoration(
                      labelText: 'Minutes',
                      border: OutlineInputBorder(),
                      isDense: true,
                    ),
                    onChanged: (val) => onUpdate(
                      FocusStep(
                        durationMins: int.tryParse(val) ?? 0,
                        type: step.type,
                        behavior: step.behavior,
                      ),
                    ),
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
