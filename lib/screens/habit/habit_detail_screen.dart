import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../data/habit/habit_models.dart';
import '../../providers/habit_provider.dart';
import '../../theme/app_theme.dart';

class HabitDetailScreen extends StatefulWidget {
  final Habit? habit;
  final bool isEditing;

  const HabitDetailScreen({super.key, this.habit, this.isEditing = false});

  @override
  State<HabitDetailScreen> createState() => _HabitDetailScreenState();
}

class _HabitDetailScreenState extends State<HabitDetailScreen> {
  final _formKey = GlobalKey<FormState>();
  late TextEditingController _nameController;
  late TextEditingController _notesController;
  late TextEditingController _stackController;

  HabitFrequency _frequency = HabitFrequency.daily;
  Color _selectedColor = AppTheme.habitColor;
  TimeOfDay? _targetTime;
  int? _stackOrder;

  // For Weekly Flexible
  int _targetDaysPerWeek = 3;

  // For Weekly Exact (1 = Mon, 7 = Sun)
  Set<int> _selectedWeekdays = {1, 3, 5};

  @override
  void initState() {
    super.initState();
    _nameController = TextEditingController(text: widget.habit?.name ?? '');
    _notesController = TextEditingController(text: widget.habit?.notes ?? '');
    _stackController = TextEditingController(
      text: widget.habit?.stackName ?? '',
    );

    if (widget.habit != null) {
      _frequency = widget.habit!.frequency;
      _selectedColor = AppTheme.habitColor;
      _targetTime = widget.habit!.targetTime;
      _stackOrder = widget.habit!.stackOrder; // Init stack order
      _targetDaysPerWeek = widget.habit!.targetDaysPerWeek ?? 3;
      if (widget.habit!.targetWeekdays != null) {
        _selectedWeekdays = widget.habit!.targetWeekdays!.toSet();
      }
    }
  }

  @override
  void dispose() {
    _nameController.dispose();
    _notesController.dispose();
    _stackController.dispose();
    super.dispose();
  }

  void _saveHabit() {
    if (!_formKey.currentState!.validate()) return;

    if (_frequency == HabitFrequency.weeklyExact && _selectedWeekdays.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Please select at least one day.')),
      );
      return;
    }

    final newHabit = Habit(
      id: widget.habit?.id ?? DateTime.now().toString(),
      name: _nameController.text.trim(),
      frequency: _frequency,
      colorValue: AppTheme.habitColor.toARGB32(),
      notes: _notesController.text.trim().isEmpty
          ? null
          : _notesController.text.trim(),
      stackName: _stackController.text.trim().isEmpty
          ? null
          : _stackController.text.trim(),
      stackOrder: _stackOrder,
      iconCodePoint: Icons.circle.codePoint,
      targetDaysPerWeek: _frequency == HabitFrequency.weeklyFlexible
          ? _targetDaysPerWeek
          : null,
      targetWeekdays: _frequency == HabitFrequency.weeklyExact
          ? _selectedWeekdays.toList()
          : null,
      completions: widget.habit?.completions,
      createdAt: widget.habit?.createdAt,
    )..setTargetTime(_targetTime);

    context.read<HabitProvider>().saveHabit(newHabit);
    Navigator.pop(context);
  }

  @override
  Widget build(BuildContext context) {
    // Extract existing stack names for autocomplete
    final existingStacks = context
        .read<HabitProvider>()
        .habits
        .map((h) => h.stackName?.trim())
        .whereType<String>()
        .where((s) => s.isNotEmpty)
        .toSet()
        .toList();

    return Scaffold(
      appBar: AppBar(
        title: Text(widget.isEditing ? 'Edit Habit' : 'New Habit'),
      ),
      body: Form(
        key: _formKey,
        child: ListView(
          padding: AppTheme.paddingScreenVertical,
          children: [
            // --- HABIT NAME ---
            TextFormField(
              controller: _nameController,
              decoration: const InputDecoration(
                labelText: 'Habit Name',
                hintText: 'e.g., Read 10 pages, Workout...',
                prefixIcon: Icon(Icons.stars),
              ),
              validator: (val) => val == null || val.trim().isEmpty
                  ? 'Please enter a name'
                  : null,
            ),
            const SizedBox(height: AppTheme.spaceLarge),

            // --- HABIT STACKING (AUTOCOMPLETE & ORDER) ---
            Row(
              children: [
                Expanded(
                  flex: 3,
                  child: Autocomplete<String>(
                    initialValue: TextEditingValue(text: _stackController.text),
                    optionsBuilder: (TextEditingValue textEditingValue) {
                      // FIX 1: Show all existing stacks when the field is empty!
                      if (textEditingValue.text.isEmpty) {
                        return existingStacks;
                      }
                      return existingStacks.where((String option) {
                        return option.toLowerCase().contains(
                          textEditingValue.text.toLowerCase(),
                        );
                      });
                    },
                    onSelected: (String selection) {
                      _stackController.text = selection;
                    },
                    fieldViewBuilder:
                        (context, controller, focusNode, onEditingComplete) {
                          return TextFormField(
                            controller: controller,
                            focusNode: focusNode,
                            // FIX 2: Use onChanged to safely sync the text without memory leaks
                            onChanged: (val) => _stackController.text = val,
                            decoration: const InputDecoration(
                              labelText: 'Habit Stack (Optional)',
                              hintText: 'e.g., Morning Routine',
                              prefixIcon: Icon(Icons.layers),
                            ),
                          );
                        },
                  ),
                ),
                const SizedBox(width: AppTheme.spaceMedium),
                Expanded(
                  flex: 1,
                  child: DropdownButtonFormField<int>(
                    initialValue:
                        _stackOrder, // Use value instead of initialValue for safe rebuilds
                    decoration: const InputDecoration(
                      labelText: 'Order',
                      contentPadding: EdgeInsets.symmetric(horizontal: 10),
                    ),
                    items: [
                      const DropdownMenuItem<int>(
                        value: null,
                        child: Text("-"),
                      ),
                      ...List.generate(10, (index) => index + 1).map(
                        (order) => DropdownMenuItem(
                          value: order,
                          child: Text(order.toString()),
                        ),
                      ),
                    ],
                    onChanged: (val) => setState(() => _stackOrder = val),
                  ),
                ),
              ],
            ),
            const SizedBox(height: AppTheme.spaceLarge),

            // --- NOTES ---
            TextFormField(
              controller: _notesController,
              decoration: const InputDecoration(
                labelText: 'Notes (Optional)',
                hintText: 'Why are you building this habit?',
                prefixIcon: Icon(Icons.notes),
              ),
              maxLines: 2,
            ),
            const SizedBox(height: AppTheme.spaceXLarge),

            // --- COLOR PICKER ---
            const Text(
              "Habit Color",
              style: TextStyle(fontWeight: AppTheme.fwBold),
            ),
            const SizedBox(height: AppTheme.spaceSmall),
            Card(
              child: ListTile(
                leading: const CircleAvatar(
                  backgroundColor: AppTheme.habitColor,
                ),
                title: const Text('Habit color is standardized'),
                subtitle: const Text(
                  'Purple is used for habits across the app.',
                ),
              ),
            ),
            const SizedBox(height: AppTheme.spaceXLarge),

            // --- FREQUENCY ---
            const Text(
              "Frequency",
              style: TextStyle(fontWeight: AppTheme.fwBold),
            ),
            const SizedBox(height: AppTheme.spaceSmall),
            SegmentedButton<HabitFrequency>(
              segments: const [
                ButtonSegment(
                  value: HabitFrequency.daily,
                  label: Text("Daily"),
                ),
                ButtonSegment(
                  value: HabitFrequency.weeklyFlexible,
                  label: Text("Flexible"),
                ),
                ButtonSegment(
                  value: HabitFrequency.weeklyExact,
                  label: Text("Specific"),
                ),
              ],
              selected: {_frequency},
              onSelectionChanged: (set) =>
                  setState(() => _frequency = set.first),
            ),
            const SizedBox(height: AppTheme.spaceLarge),

            // --- DYNAMIC FREQUENCY INPUTS ---
            if (_frequency == HabitFrequency.weeklyFlexible) ...[
              Card(
                child: Padding(
                  padding: AppTheme.paddingCard,
                  child: Column(
                    children: [
                      Text(
                        "Goal: $_targetDaysPerWeek days a week",
                        style: const TextStyle(fontWeight: AppTheme.fwBold),
                      ),
                      Slider(
                        value: _targetDaysPerWeek.toDouble(),
                        min: 1,
                        max: 7,
                        divisions: 7,
                        activeColor: _selectedColor,
                        onChanged: (val) =>
                            setState(() => _targetDaysPerWeek = val.toInt()),
                      ),
                    ],
                  ),
                ),
              ),
            ] else if (_frequency == HabitFrequency.weeklyExact) ...[
              Card(
                child: Padding(
                  padding: AppTheme.paddingCard,
                  child: Wrap(
                    spacing: AppTheme.spaceSmall,
                    children: [1, 2, 3, 4, 5, 6, 7].map((day) {
                      final isSelected = _selectedWeekdays.contains(day);
                      final dayLabels = ['M', 'T', 'W', 'T', 'F', 'S', 'S'];
                      return ChoiceChip(
                        label: Text(dayLabels[day - 1]),
                        selected: isSelected,
                        selectedColor: _selectedColor.withValues(alpha: 0.3),
                        onSelected: (selected) {
                          setState(() {
                            if (selected) {
                              _selectedWeekdays.add(day);
                            } else {
                              _selectedWeekdays.remove(day);
                            }
                          });
                        },
                      );
                    }).toList(),
                  ),
                ),
              ),
            ],

            const SizedBox(height: AppTheme.spaceXLarge),

            // --- TIME REMINDER ---
            const Text(
              "Reminder Time (Optional)",
              style: TextStyle(fontWeight: AppTheme.fwBold),
            ),
            const SizedBox(height: AppTheme.spaceSmall),
            Card(
              child: ListTile(
                leading: Icon(
                  Icons.notifications_active,
                  color: _selectedColor,
                ),
                title: Text(
                  _targetTime != null
                      ? _targetTime!.format(context)
                      : "No specific time",
                ),
                trailing: _targetTime != null
                    ? IconButton(
                        icon: const Icon(Icons.clear),
                        onPressed: () => setState(() => _targetTime = null),
                      )
                    : const Icon(Icons.edit),
                onTap: () async {
                  final time = await showTimePicker(
                    context: context,
                    initialTime:
                        _targetTime ?? const TimeOfDay(hour: 8, minute: 0),
                  );
                  if (time != null) setState(() => _targetTime = time);
                },
              ),
            ),

            const SizedBox(height: AppTheme.space3XLarge),
          ],
        ),
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: _saveHabit,
        backgroundColor: _selectedColor,
        icon: const Icon(Icons.save),
        label: const Text("Save Habit"),
      ),
    );
  }
}
