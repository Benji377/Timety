import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../data/habit/habit_models.dart';
import '../../l10n/app_localizations.dart';
import '../../providers/habit_provider.dart';
import '../../providers/settings_provider.dart';
import '../../theme/app_theme.dart';
import '../../utils/datetime/date_time_pickers.dart';
import '../../utils/habit/habit_icons.dart';
import '../../widgets/common/app_dialogs.dart';

class HabitDetailScreen extends StatefulWidget {
  final Habit? habit;
  final bool isEditing;

  const HabitDetailScreen({super.key, this.habit, this.isEditing = false});

  @override
  State<HabitDetailScreen> createState() => _HabitDetailScreenState();
}

class _HabitDetailScreenState extends State<HabitDetailScreen> {
  late bool _isEditing;
  late bool _isNewHabit;

  final _formKey = GlobalKey<FormState>();
  late TextEditingController _nameController;
  late TextEditingController _notesController;
  late TextEditingController _stackController;

  late HabitFrequency _frequency;
  late IconData _selectedIcon;
  late Color _selectedColor;
  TimeOfDay? _targetTime;
  int? _stackOrder;

  late int _targetDaysPerWeek;
  late Set<int> _selectedWeekdays;

  @override
  void initState() {
    super.initState();
    _isNewHabit = widget.habit == null;
    _isEditing = _isNewHabit || widget.isEditing;

    _nameController = TextEditingController(text: widget.habit?.name ?? '');
    _notesController = TextEditingController(text: widget.habit?.notes ?? '');
    _stackController = TextEditingController(
      text: widget.habit?.stackName ?? '',
    );

    _frequency = widget.habit?.frequency ?? HabitFrequency.daily;
    _targetTime = widget.habit?.targetTime;
    _stackOrder = widget.habit?.stackOrder;
    _targetDaysPerWeek = widget.habit?.targetDaysPerWeek ?? 3;

    _selectedWeekdays = widget.habit?.targetWeekdays?.toSet() ?? {1, 3, 5};

    _selectedIcon = widget.habit != null
        ? _getIconFromCodePoint(widget.habit!.iconCodePoint) ?? Icons.circle
        : Icons.circle;

    _selectedColor = widget.habit != null
        ? Color(widget.habit!.colorValue)
        : AppTheme.habitColor;
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
    final theme = Theme.of(context);
    final disabledBorderColor = theme.dividerColor.withValues(alpha: 0.6);

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

    final String appBarTitle = _isNewHabit
        ? l10n.habitDetailTitleNew
        : (_isEditing ? l10n.habitDetailTitleEdit : l10n.habitDetailTitleView);

    return Scaffold(
      appBar: AppBar(
        title: Text(appBarTitle),
        actions: [
          if (!_isEditing && !_isNewHabit) ...[
            IconButton(
              icon: const Icon(
                Icons.delete_outline,
                color: AppTheme.errorColor,
              ),
              onPressed: _confirmAndDelete,
            ),
            IconButton(
              icon: const Icon(Icons.edit),
              onPressed: () => setState(() => _isEditing = true),
            ),
          ] else ...[
            IconButton(
              icon: const Icon(Icons.check),
              onPressed: _saveHabit,
              tooltip: l10n.commonLabelSave,
            ),
          ],
        ],
      ),
      body: Form(
        key: _formKey,
        child: ListView(
          padding: AppTheme.paddingScreenVertical,
          children: [
            // --- HABIT NAME ---
            TextFormField(
              controller: _nameController,
              enabled: _isEditing,
              decoration: InputDecoration(
                labelText: l10n.habitDetailLabelName,
                hintText: l10n.habitDetailLabelNameHint,
                prefixIcon: Icon(Icons.stars, color: _selectedColor),
                disabledBorder: OutlineInputBorder(
                  borderSide: BorderSide(color: disabledBorderColor),
                ),
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
                  child: _isEditing
                      ? Autocomplete<String>(
                          initialValue: TextEditingValue(
                            text: _stackController.text,
                          ),
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
                              (
                                context,
                                controller,
                                focusNode,
                                onEditingComplete,
                              ) {
                                return TextFormField(
                                  controller: controller,
                                  focusNode: focusNode,
                                  onChanged: (val) =>
                                      _stackController.text = val,
                                  decoration: InputDecoration(
                                    labelText: l10n.habitDetailLabelStack,
                                    hintText: l10n.habitDetailLabelStackHint,
                                    prefixIcon: const Icon(Icons.layers),
                                  ),
                                );
                              },
                        )
                      : TextFormField(
                          initialValue: _stackController.text.isEmpty
                              ? "-"
                              : _stackController.text,
                          enabled: false,
                          decoration: InputDecoration(
                            labelText: l10n.habitDetailLabelStack,
                            prefixIcon: const Icon(Icons.layers),
                            disabledBorder: OutlineInputBorder(
                              borderSide: BorderSide(
                                color: disabledBorderColor,
                              ),
                            ),
                          ),
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
                      disabledBorder: OutlineInputBorder(
                        borderSide: BorderSide(color: disabledBorderColor),
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
                    onChanged: _isEditing
                        ? (val) => setState(() => _stackOrder = val)
                        : null,
                  ),
                ),
              ],
            ),
            const SizedBox(height: AppTheme.spaceLarge),

            // --- APPEARANCE (ICON & COLOR) ---
            Row(
              children: [
                Expanded(
                  child: _isEditing
                      ? InkWell(
                          onTap: _pickIcon,
                          borderRadius: const BorderRadius.all(
                            Radius.circular(4.0),
                          ),
                          child: InputDecorator(
                            decoration: InputDecoration(
                              labelText: l10n.habitDetailLabelIcon,
                              border: const OutlineInputBorder(),
                              filled: true,
                            ),
                            child: Icon(_selectedIcon, color: _selectedColor),
                          ),
                        )
                      : ListTile(
                          title: Text(
                            l10n.habitDetailLabelIcon,
                            style: const TextStyle(
                              fontSize: AppTheme.fsBodySmall,
                            ),
                          ),
                          leading: Icon(_selectedIcon, color: _selectedColor),
                          contentPadding: EdgeInsets.zero,
                        ),
                ),
                const SizedBox(width: AppTheme.spaceLarge),
                Expanded(
                  child: _isEditing
                      ? InkWell(
                          onTap: _pickColor,
                          borderRadius: const BorderRadius.all(
                            Radius.circular(4.0),
                          ),
                          child: InputDecorator(
                            decoration: InputDecoration(
                              labelText: l10n.habitDetailLabelColor,
                              border: const OutlineInputBorder(),
                              filled: true,
                            ),
                            child: Container(
                              height: 24,
                              decoration: BoxDecoration(
                                color: _selectedColor,
                                borderRadius: BorderRadius.circular(4),
                              ),
                            ),
                          ),
                        )
                      : ListTile(
                          title: Text(
                            l10n.habitDetailLabelColor,
                            style: const TextStyle(
                              fontSize: AppTheme.fsBodySmall,
                            ),
                          ),
                          leading: Container(
                            width: 24,
                            height: 24,
                            decoration: BoxDecoration(
                              color: _selectedColor,
                              shape: BoxShape.circle,
                            ),
                          ),
                          contentPadding: EdgeInsets.zero,
                        ),
                ),
              ],
            ),
            const SizedBox(height: AppTheme.spaceLarge),

            // --- NOTES ---
            TextFormField(
              controller: _notesController,
              enabled: _isEditing,
              decoration: InputDecoration(
                labelText: l10n.habitDetailLabelNotes,
                hintText: l10n.habitDetailLabelNotesHint,
                prefixIcon: const Icon(Icons.notes),
                disabledBorder: OutlineInputBorder(
                  borderSide: BorderSide(color: disabledBorderColor),
                ),
              ),
              maxLines: 2,
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
              onSelectionChanged: _isEditing
                  ? (set) => setState(() => _frequency = set.first)
                  : null,
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
                        activeColor: _selectedColor,
                        onChanged: _isEditing
                            ? (val) => setState(
                                () => _targetDaysPerWeek = val.toInt(),
                              )
                            : null,
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
                      final dayLabels = [
                        l10n.calendarHeaderMon,
                        l10n.calendarHeaderTue,
                        l10n.calendarHeaderWed,
                        l10n.calendarHeaderThu,
                        l10n.calendarHeaderFri,
                        l10n.calendarHeaderSat,
                        l10n.calendarHeaderSun,
                      ];
                      return ChoiceChip(
                        label: Text(dayLabels[day - 1]),
                        selected: isSelected,
                        selectedColor: _selectedColor.withValues(alpha: 0.3),
                        onSelected: _isEditing
                            ? (selected) {
                                setState(() {
                                  if (selected) {
                                    _selectedWeekdays.add(day);
                                  } else {
                                    _selectedWeekdays.remove(day);
                                  }
                                });
                              }
                            : null,
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
            _isEditing
                ? InkWell(
                    onTap: () async {
                      final time = await AppDatePickers.pickTime(
                        context: context,
                        initialTime:
                            _targetTime ?? const TimeOfDay(hour: 8, minute: 0),
                      );
                      if (time != null) setState(() => _targetTime = time);
                    },
                    borderRadius: const BorderRadius.all(Radius.circular(4.0)),
                    child: InputDecorator(
                      isEmpty: _targetTime == null,
                      decoration: const InputDecoration(
                        prefixIcon: Icon(Icons.notifications_active),
                        suffixIcon: Icon(Icons.edit),
                        border: OutlineInputBorder(),
                        filled: true,
                      ),
                      child: Text(
                        _targetTime != null
                            ? context.read<SettingsProvider>().getFormattedTime(
                                DateTime(
                                  2000,
                                  1,
                                  1,
                                  _targetTime!.hour,
                                  _targetTime!.minute,
                                ),
                              )
                            : l10n.habitDetailLabelReminderNoTime,
                      ),
                    ),
                  )
                : ListTile(
                    contentPadding: EdgeInsets.zero,
                    leading: const Icon(Icons.notifications_active),
                    title: Text(
                      _targetTime != null
                          ? context.read<SettingsProvider>().getFormattedTime(
                              DateTime(
                                2000,
                                1,
                                1,
                                _targetTime!.hour,
                                _targetTime!.minute,
                              ),
                            )
                          : l10n.habitDetailLabelReminderNoTime,
                    ),
                  ),

            const SizedBox(height: AppTheme.space3XLarge),
          ],
        ),
      ),
    );
  }

  // --- POPUP DIALOGS ---
  Future<void> _pickIcon() async {
    final l10n = AppLocalizations.of(context)!;
    final IconData? picked = await showDialog<IconData>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(l10n.habitDetailLabelIcon),
        content: SizedBox(
          width: double.maxFinite,
          child: GridView.count(
            crossAxisCount: 5,
            mainAxisSpacing: 16,
            crossAxisSpacing: 16,
            shrinkWrap: true,
            children: HabitIcons.availableIcons.map((icon) {
              final isSelected = icon.codePoint == _selectedIcon.codePoint;
              return InkWell(
                onTap: () => Navigator.pop(context, icon),
                child: Container(
                  decoration: BoxDecoration(
                    color: isSelected
                        ? _selectedColor.withValues(alpha: 0.2)
                        : Colors.transparent,
                    shape: BoxShape.circle,
                  ),
                  child: Icon(icon, color: isSelected ? _selectedColor : null),
                ),
              );
            }).toList(),
          ),
        ),
      ),
    );

    if (picked != null && mounted) {
      setState(() => _selectedIcon = picked);
    }
  }

  Future<void> _pickColor() async {
    final l10n = AppLocalizations.of(context)!;

    final List<Color> colors = [
      AppTheme.habitColor,
      Colors.red,
      Colors.pink,
      Colors.amber,
      Colors.orange,
      Colors.green,
      Colors.lightGreen,
      Colors.teal,
      Colors.blue,
      Colors.cyan,
      Colors.indigo,
      Colors.purple,
      Colors.deepPurple,
      Colors.brown,
      Colors.blueGrey,
    ];

    final Color? picked = await showDialog<Color>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(l10n.habitDetailLabelColorPicker),
        content: SizedBox(
          width: double.maxFinite,
          child: GridView.count(
            crossAxisCount: 4,
            mainAxisSpacing: 16,
            crossAxisSpacing: 16,
            shrinkWrap: true,
            children: colors.map((color) {
              final isSelected = color == _selectedColor;
              return InkWell(
                onTap: () => Navigator.pop(context, color),
                child: Container(
                  decoration: BoxDecoration(
                    color: color,
                    shape: BoxShape.circle,
                    border: isSelected
                        ? Border.all(
                            color: Theme.of(context).colorScheme.onSurface,
                            width: 3,
                          )
                        : null,
                  ),
                ),
              );
            }).toList(),
          ),
        ),
      ),
    );

    if (picked != null && mounted) {
      setState(() => _selectedColor = picked);
    }
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
      colorValue: _selectedColor.toARGB32(), // Saves your chosen color!
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

    if (_isNewHabit) {
      Navigator.pop(context);
    } else {
      setState(() => _isEditing = false);
    }
  }

  Future<void> _confirmAndDelete() async {
    final l10n = AppLocalizations.of(context)!;

    final confirm = await AppDialogs.showConfirmation(
      context: context,
      title: l10n.habitDeleteTitle,
      content: l10n.habitDeleteContent,
    );

    if (confirm == true && mounted) {
      if (widget.habit != null) {
        context.read<HabitProvider>().removeHabit(widget.habit!.id);
      }
      Navigator.pop(context);
    }
  }
}
