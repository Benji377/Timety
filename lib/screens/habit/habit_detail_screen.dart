import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../data/habit/habit_models.dart';
import '../../l10n/app_localizations.dart';
import '../../providers/habit_provider.dart';
import '../../theme/app_theme.dart';
import '../../utils/habit_icons.dart';

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
  IconData _selectedIcon = Icons.circle;
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
      _selectedIcon =
          _getIconFromCodePoint(widget.habit!.iconCodePoint) ?? Icons.circle;
      _targetTime = widget.habit!.targetTime;
      _stackOrder = widget.habit!.stackOrder;
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
    final l10n = AppLocalizations.of(context)!;

    return Scaffold(
      appBar: AppBar(
        title: Text(
          widget.isEditing
              ? l10n.habitDetailTitleEdit
              : l10n.habitDetailTitleNew,
        ),
      ),
      body: Form(
        key: _formKey,
        child: ListView(
          padding: AppTheme.paddingScreenVertical,
          children: [
            // --- HABIT NAME ---
            TextFormField(
              controller: _nameController,
              decoration: InputDecoration(
                labelText: l10n.habitDetailLabelName,
                hintText: l10n.habitDetailLabelNameHint,
                prefixIcon: const Icon(Icons.stars),
              ),
              validator: (val) => val == null || val.trim().isEmpty
                  ? l10n.habitDetailLabelNameRequest
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
                            onChanged: (val) => _stackController.text = val,
                            decoration: InputDecoration(
                              labelText: l10n.habitDetailLabelStack,
                              hintText: l10n.habitDetailLabelStackHint,
                              prefixIcon: const Icon(Icons.layers),
                            ),
                          );
                        },
                  ),
                ),
                const SizedBox(width: AppTheme.spaceMedium),
                Expanded(
                  child: DropdownButtonFormField<int>(
                    initialValue: _stackOrder,
                    decoration: InputDecoration(
                      labelText: l10n.habitDetailLabelStackOrder,
                      contentPadding: const EdgeInsets.symmetric(
                        horizontal: 10,
                      ),
                    ),
                    items: [
                      const DropdownMenuItem<int>(child: Text("-")),
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
              decoration: InputDecoration(
                labelText: l10n.habitDetailLabelNotes,
                hintText: l10n.habitDetailLabelNotesHint,
                prefixIcon: const Icon(Icons.notes),
              ),
              maxLines: 2,
            ),
            const SizedBox(height: AppTheme.spaceLarge),

            // --- ICON SELECTOR ---
            Text(
              l10n.habitDetailLabelIcon,
              style: const TextStyle(fontWeight: AppTheme.fwBold),
            ),
            const SizedBox(height: AppTheme.spaceSmall),
            SizedBox(
              height: 140,
              child: GridView.count(
                crossAxisCount: 6,
                mainAxisSpacing: 12,
                crossAxisSpacing: 12,
                children: HabitIcons.availableIcons.map((icon) {
                  final isSelected = icon.codePoint == _selectedIcon.codePoint;
                  return GestureDetector(
                    onTap: () => setState(() => _selectedIcon = icon),
                    child: Container(
                      decoration: BoxDecoration(
                        border: Border.all(
                          color: isSelected
                              ? AppTheme.taskColor
                              : Colors.transparent,
                          width: 2,
                        ),
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: Icon(
                        icon,
                        color: isSelected ? AppTheme.taskColor : null,
                      ),
                    ),
                  );
                }).toList(),
              ),
            ),
            const SizedBox(height: AppTheme.spaceXLarge),

            // --- FREQUENCY ---
            Text(
              l10n.habitDetailLabelFrequency,
              style: const TextStyle(fontWeight: AppTheme.fwBold),
            ),
            const SizedBox(height: AppTheme.spaceSmall),
            SegmentedButton<HabitFrequency>(
              segments: [
                ButtonSegment(
                  value: HabitFrequency.daily,
                  label: Text(l10n.habitDetailLabelFrequencyDaily),
                ),
                ButtonSegment(
                  value: HabitFrequency.weeklyFlexible,
                  label: Text(l10n.habitDetailLabelFrequencyFlexible),
                ),
                ButtonSegment(
                  value: HabitFrequency.weeklyExact,
                  label: Text(l10n.habitDetailLabelFrequencyExact),
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
                        l10n.habitDetailLabelGoal(_targetDaysPerWeek),
                        style: const TextStyle(fontWeight: AppTheme.fwBold),
                      ),
                      Slider(
                        value: _targetDaysPerWeek.toDouble(),
                        min: 1,
                        max: 7,
                        divisions: 7,
                        activeColor: AppTheme.habitColor,
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
                        selectedColor: AppTheme.habitColor.withValues(
                          alpha: 0.3,
                        ),
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
            Text(
              l10n.habitDetailLabelReminder,
              style: const TextStyle(fontWeight: AppTheme.fwBold),
            ),
            const SizedBox(height: AppTheme.spaceSmall),
            Card(
              child: ListTile(
                leading: const Icon(
                  Icons.notifications_active,
                  color: AppTheme.habitColor,
                ),
                title: Text(
                  _targetTime != null
                      ? _targetTime!.format(context)
                      : l10n.habitDetailLabelReminderNoTime,
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
        backgroundColor: AppTheme.habitColor,
        icon: const Icon(Icons.save),
        label: Text(l10n.commonLabelSave),
      ),
    );
  }

  // Convert a codePoint back to an IconData object
  IconData? _getIconFromCodePoint(int? codePoint) {
    if (codePoint == null) return null;
    try {
      return HabitIcons.availableIcons.firstWhere(
        (icon) => icon.codePoint == codePoint,
        orElse: () => Icons.circle,
      );
    } catch (e) {
      return Icons.circle;
    }
  }

  void _saveHabit() {
    if (!_formKey.currentState!.validate()) return;

    if (_frequency == HabitFrequency.weeklyExact && _selectedWeekdays.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            AppLocalizations.of(context)!.habitDetailSnackbarSaveNoDay,
          ),
        ),
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
      iconCodePoint: _selectedIcon.codePoint,
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
}
