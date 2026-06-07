import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../data/habit/habit_models.dart';
import '../../theme/app_theme.dart';
import '../../utils/datetime/date_time_pickers.dart';
import '../../utils/datetime/date_utils.dart';
import '../../l10n/app_localizations.dart';
import '../../providers/settings_provider.dart';

/// Reusable bottom sheet builders for habit-related UIs
class HabitBottomSheetBuilders {
  /// Shows a unified calendar and history sheet for a habit.
  ///
  /// Displays statistics, an interactive monthly calendar, and a timeline.
  /// Days with completions have a purple dot. Tapping them allows modification.
  /// Tapping empty days asks for a time and adds a completion.
  static void showUnifiedHistorySheet({
    required BuildContext context,
    required Habit habit,
    required Function(DateTime date) onDateSelected,
    required Function(DateTime date) onDateDeselected,
  }) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (context) {
        return FractionallySizedBox(
          heightFactor: 0.90, // Slightly taller to comfortably fit the timeline
          child: _UnifiedCalendarSheet(
            habit: habit,
            onDateSelected: onDateSelected,
            onDateDeselected: onDateDeselected,
          ),
        );
      },
    );
  }
}

class _UnifiedCalendarSheet extends StatefulWidget {
  final Habit habit;
  final Function(DateTime) onDateSelected;
  final Function(DateTime) onDateDeselected;

  const _UnifiedCalendarSheet({
    required this.habit,
    required this.onDateSelected,
    required this.onDateDeselected,
  });

  @override
  State<_UnifiedCalendarSheet> createState() => _UnifiedCalendarSheetState();
}

class _UnifiedCalendarSheetState extends State<_UnifiedCalendarSheet> {
  late DateTime _displayedMonth;
  late DateTime _today;

  @override
  void initState() {
    super.initState();
    _today = DateTime.now();
    _displayedMonth = DateTime(_today.year, _today.month);
  }

  void _changeMonth(int offset) {
    setState(() {
      _displayedMonth = DateTime(
        _displayedMonth.year,
        _displayedMonth.month + offset,
      );
    });
  }

  Future<void> _handleDateTap(DateTime date, bool isCompleted) async {
    final l10n = AppLocalizations.of(context)!;
    // Prevent marking future dates
    if (date.isAfter(_today) && !AppDateUtils.isSameDay(date, _today)) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text(l10n.habitHistoryEmpty)));
      return;
    }

    if (isCompleted) {
      // Find the specific completion for this day
      final exactCompletion = widget.habit.completions.firstWhere(
        (c) => AppDateUtils.isSameDay(c, date),
      );

      // Show options to modify or remove
      showDialog(
        context: context,
        builder: (ctx) => AlertDialog(
          title: Text(l10n.habitHistoryRemoveTitle),
          content: Text(
            l10n.habitHistoryRemoveCompletion(context.read<SettingsProvider>().getFormattedDate(date)),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx),
              child: Text(l10n.commonLabelCancel),
            ),
            TextButton(
              onPressed: () {
                widget.onDateDeselected(exactCompletion);
                setState(() {}); // Rebuild calendar and timeline
                Navigator.pop(ctx);
              },
              child: Text(l10n.commonLabelRemove, style: const TextStyle(color: Colors.red)),
            ),
          ],
        ),
      );
    } else {
      // Prompt user for the time of completion
      final TimeOfDay? pickedTime = await AppDatePickers.pickTime(
        context: context,
        initialTime: TimeOfDay.now(),
        helpText: l10n.habitHistoryTimePrompt.toUpperCase(),
      );

      if (pickedTime != null) {
        // Combine the tapped date with the picked time
        final selectedDateTime = DateTime(
          date.year,
          date.month,
          date.day,
          pickedTime.hour,
          pickedTime.minute,
        );
        widget.onDateSelected(selectedDateTime);
        setState(() {}); // Rebuild calendar and timeline
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final completions = widget.habit.completions.toList();
    final last30 = _today.subtract(const Duration(days: 30));
    final last90 = _today.subtract(const Duration(days: 90));

    final count30 = completions.where((c) => c.isAfter(last30)).length;
    final count90 = completions.where((c) => c.isAfter(last90)).length;
    final countAll = completions.length;

    // Calendar Math
    final int daysInMonth = DateTime(
      _displayedMonth.year,
      _displayedMonth.month + 1,
      0,
    ).day;
    final int firstWeekday = DateTime(
      _displayedMonth.year,
      _displayedMonth.month,
    ).weekday;
    final int emptySlotsPrefix = firstWeekday - 1;

    // Sort completions for the timeline (Newest first)
    completions.sort((a, b) => b.compareTo(a));

    return SafeArea(
      child: Column(
        children: [
          Padding(
            padding: const EdgeInsets.all(16.0),
            child: Text(
              l10n.habitHistoryTitle,
              style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
            ),
          ),

          // --- Statistics Row ---
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16.0),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceEvenly,
              children: [
                _buildStatCard(l10n.habitHistoryStatLast30, count30.toString()),
                _buildStatCard(l10n.habitHistoryStatLast90, count90.toString()),
                _buildStatCard(l10n.habitHistoryStatTotal, countAll.toString()),
              ],
            ),
          ),
          const Divider(height: 32),

          // --- Calendar Header ---
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16.0),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                IconButton(
                  icon: const Icon(Icons.chevron_left),
                  onPressed: () => _changeMonth(-1),
                ),
                Text(
                  context.watch<SettingsProvider>().getFormattedMonthYear(_displayedMonth),
                  style: const TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.w600,
                  ),
                ),
                IconButton(
                  icon: const Icon(Icons.chevron_right),
                  onPressed: () => _changeMonth(1),
                ),
              ],
            ),
          ),

          // --- Days of the Week ---
          Padding(
            padding: const EdgeInsets.symmetric(
              horizontal: 16.0,
              vertical: 8.0,
            ),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceAround,
              children: [
                l10n.commonWeekdayMon,
                l10n.commonWeekdayTue,
                l10n.commonWeekdayWed,
                l10n.commonWeekdayThu,
                l10n.commonWeekdayFri,
                l10n.commonWeekdaySat,
                l10n.commonWeekdaySun,
              ]
                  .map(
                    (day) => SizedBox(
                      width: 30,
                      child: Text(
                        day,
                        textAlign: TextAlign.center,
                        style: const TextStyle(
                          fontSize: 12,
                          color: Colors.grey,
                        ),
                      ),
                    ),
                  )
                  .toList(),
            ),
          ),

          // --- Calendar Grid (ShrinkWrapped) ---
          GridView.builder(
            shrinkWrap: true,
            physics: const NeverScrollableScrollPhysics(),
            padding: const EdgeInsets.symmetric(horizontal: 16.0),
            gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
              crossAxisCount: 7,
              mainAxisSpacing: 8,
              crossAxisSpacing: 8,
              childAspectRatio: 1.1,
            ),
            itemCount: daysInMonth + emptySlotsPrefix,
            itemBuilder: (context, index) {
              if (index < emptySlotsPrefix) return const SizedBox.shrink();

              final dayNumber = index - emptySlotsPrefix + 1;
              final currentDate = DateTime(
                _displayedMonth.year,
                _displayedMonth.month,
                dayNumber,
              );

              final isToday = AppDateUtils.isSameDay(currentDate, _today);
              final isCompleted = completions.any(
                (c) => AppDateUtils.isSameDay(c, currentDate),
              );
              final isFuture = currentDate.isAfter(_today) && !isToday;

              return GestureDetector(
                onTap: isFuture
                    ? null
                    : () => _handleDateTap(currentDate, isCompleted),
                child: Container(
                  decoration: BoxDecoration(
                    color: isFuture
                        ? Colors.transparent
                        : Theme.of(context).colorScheme.surfaceContainerHighest
                              .withValues(alpha: 0.5),
                    border: isToday
                        ? Border.all(color: AppTheme.habitColor, width: 2)
                        : null,
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Stack(
                    alignment: Alignment.center,
                    children: [
                      Text(
                        dayNumber.toString(),
                        style: TextStyle(
                          fontWeight: isToday
                              ? FontWeight.bold
                              : FontWeight.normal,
                          color: isFuture
                              ? Colors.grey.withValues(alpha: 0.3)
                              : null,
                        ),
                      ),
                      if (isCompleted)
                        Positioned(
                          bottom: 4,
                          child: Container(
                            width: 6,
                            height: 6,
                            decoration: const BoxDecoration(
                              color: AppTheme.habitColor, // Purple dot
                              shape: BoxShape.circle,
                            ),
                          ),
                        ),
                    ],
                  ),
                ),
              );
            },
          ),

          const SizedBox(height: 16),
          const Divider(height: 1),

          // --- Timeline View ---
          Expanded(
            child: completions.isEmpty
                ? Center(
                    child: Text(
                      l10n.habitHistoryEmpty,
                      style: const TextStyle(color: Colors.grey),
                    ),
                  )
                : ListView.builder(
                    padding: const EdgeInsets.only(top: 16, bottom: 32),
                    itemCount: completions.length,
                    itemBuilder: (context, index) {
                      final completion = completions[index];
                      final isLast = index == completions.length - 1;
                      return _buildTimelineItem(completion, isLast, l10n);
                    },
                  ),
          ),
        ],
      ),
    );
  }

  Widget _buildTimelineItem(DateTime completion, bool isLast, AppLocalizations l10n) {
    return IntrinsicHeight(
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // Left side: Timeline graphics
          SizedBox(
            width: 48,
            child: Stack(
              alignment: Alignment.topCenter,
              children: [
                // The vertical line (drawn if not the last item)
                if (!isLast)
                  Positioned.fill(
                    top: 16, // Start slightly below the dot
                    child: Align(
                      child: Container(
                        width: 2,
                        color: AppTheme.habitColor.withValues(alpha: 0.3),
                      ),
                    ),
                  ),
                // The purple dot
                Container(
                  margin: const EdgeInsets.only(top: 12),
                  width: 12,
                  height: 12,
                  decoration: const BoxDecoration(
                    color: AppTheme.habitColor,
                    shape: BoxShape.circle,
                  ),
                ),
              ],
            ),
          ),

          // Right side: Data and actions
          Expanded(
            child: Padding(
              padding: const EdgeInsets.only(bottom: 24.0, top: 8.0),
              child: Row(
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          context.watch<SettingsProvider>().getFormattedDate(completion),
                          style: const TextStyle(
                            fontWeight: FontWeight.bold,
                            fontSize: 15,
                          ),
                        ),
                        const SizedBox(height: 4),
                        Text(
                          l10n.habitHistoryCompletedAt(context.watch<SettingsProvider>().getFormattedTime(completion)),
                          style: const TextStyle(
                            color: Colors.grey,
                            fontSize: 13,
                          ),
                        ),
                      ],
                    ),
                  ),
                  // Delete button directly on the timeline item
                  IconButton(
                    icon: const Icon(
                      Icons.delete_outline,
                      size: 20,
                      color: Colors.grey,
                    ),
                    tooltip: l10n.habitHistoryRemoveTooltip,
                    onPressed: () {
                      widget.onDateDeselected(completion);
                      setState(() {});
                    },
                  ),
                  const SizedBox(width: 8),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildStatCard(String label, String count) {
    return Column(
      children: [
        Text(
          count,
          style: const TextStyle(
            fontSize: 20,
            fontWeight: FontWeight.bold,
            color: AppTheme.successColor,
          ),
        ),
        Text(label, style: const TextStyle(fontSize: 12, color: Colors.grey)),
      ],
    );
  }
}
