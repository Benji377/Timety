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

  HabitFrequency _frequency = HabitFrequency.daily;
  Color _selectedColor = AppTheme.typeHabitColor;
  TimeOfDay? _targetTime;
  int? _selectedIconCode;

  // For Weekly Flexible
  int _targetDaysPerWeek = 3;

  // For Weekly Exact (1 = Mon, 7 = Sun)
  Set<int> _selectedWeekdays = {1, 3, 5};

  final List<Color> _colorOptions = [
    AppTheme.infoColor,
    AppTheme.successColor,
    AppTheme.warningColor,
    AppTheme.errorColor,
    Colors.purple,
    Colors.teal,
  ];

  @override
  void initState() {
    super.initState();
    _nameController = TextEditingController(text: widget.habit?.name ?? '');
    _notesController = TextEditingController(text: widget.habit?.notes ?? '');

    if (widget.habit != null) {
      _frequency = widget.habit!.frequency;
      _selectedColor = Color(widget.habit!.colorValue);
      _targetTime = widget.habit!.targetTime;
      _selectedIconCode = widget.habit!.iconCodePoint;
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
      colorValue: _selectedColor.toARGB32(),
      notes: _notesController.text.trim().isEmpty
          ? null
          : _notesController.text.trim(),
      iconCodePoint: _selectedIconCode,
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
    final isDark = Theme.of(context).brightness == Brightness.dark;

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

            // --- ICON PICKER ---
            const Text(
              "Habit Icon",
              style: TextStyle(fontWeight: AppTheme.fwBold),
            ),
            const SizedBox(height: AppTheme.spaceSmall),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children:
                  [
                    Icons.fitness_center,
                    Icons.menu_book,
                    Icons.water_drop,
                    Icons.bed,
                    Icons.attach_money,
                    Icons.self_improvement,
                    Icons.directions_run,
                    Icons.laptop_mac,
                    Icons.favorite,
                  ].map((icon) {
                    final isSelected = _selectedIconCode == icon.codePoint;
                    return ChoiceChip(
                      label: Icon(
                        icon,
                        color: isSelected ? _selectedColor : Colors.grey,
                      ),
                      selected: isSelected,
                      selectedColor: _selectedColor.withValues(alpha: 0.2),
                      onSelected: (selected) => setState(
                        () => _selectedIconCode = selected
                            ? icon.codePoint
                            : null,
                      ),
                    );
                  }).toList(),
            ),

            const SizedBox(height: AppTheme.spaceXLarge),

            // --- COLOR PICKER ---
            const Text(
              "Habit Color",
              style: TextStyle(fontWeight: AppTheme.fwBold),
            ),
            const SizedBox(height: AppTheme.spaceSmall),
            Wrap(
              spacing: AppTheme.spaceMedium,
              children: _colorOptions.map((color) {
                final isSelected =
                    _selectedColor.toARGB32() == color.toARGB32();
                return GestureDetector(
                  onTap: () => setState(() => _selectedColor = color),
                  child: Container(
                    width: 40,
                    height: 40,
                    decoration: BoxDecoration(
                      color: color,
                      shape: BoxShape.circle,
                      border: isSelected
                          ? Border.all(
                              color: isDark ? Colors.white : Colors.black,
                              width: 3,
                            )
                          : null,
                    ),
                  ),
                );
              }).toList(),
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
