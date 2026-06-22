import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../theme/app_theme.dart';
import '../../utils/datetime/date_utils.dart';
import '../../l10n/app_localizations.dart';
import '../../providers/settings_provider.dart';

class UserStreakTimelineCard extends StatefulWidget {
  final List<DateTime> activityDates;
  final List<DateTime> taskDates;
  final List<DateTime> focusDates;
  final List<DateTime> habitDates;
  final int currentStreak;
  final int highestStreak;

  const UserStreakTimelineCard({
    super.key,
    required this.activityDates,
    required this.taskDates,
    required this.focusDates,
    required this.habitDates,
    required this.currentStreak,
    required this.highestStreak,
  });

  @override
  State<UserStreakTimelineCard> createState() => _UserStreakTimelineCardState();
}

class _UserStreakTimelineCardState extends State<UserStreakTimelineCard> {
  final ScrollController _scrollController = ScrollController();
  bool _didAutoScroll = false;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (_didAutoScroll) return;

    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted || !_scrollController.hasClients) return;
      _scrollController.jumpTo(_scrollController.position.maxScrollExtent);
      _didAutoScroll = true;
    });
  }

  @override
  void dispose() {
    _scrollController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final today = DateTime.now();
    final currentStreakKeys = _buildCurrentStreakDayKeys(widget.activityDates);
    final l10n = AppLocalizations.of(context)!;

    final taskKeys = widget.taskDates.map(AppDateUtils.dayKey).toSet();
    final focusKeys = widget.focusDates.map(AppDateUtils.dayKey).toSet();
    final habitKeys = widget.habitDates.map(AppDateUtils.dayKey).toSet();

    final days = List.generate(7, (index) {
      final day = DateTime(
        today.year,
        today.month,
        today.day,
      ).subtract(Duration(days: 6 - index));
      final key = AppDateUtils.dayKey(day);

      return _StreakDayInfo(
        date: day,
        isToday: AppDateUtils.isSameDay(day, today),
        inCurrentStreak: currentStreakKeys.contains(key),
        hasTask: taskKeys.contains(key),
        hasFocus: focusKeys.contains(key),
        hasHabit: habitKeys.contains(key),
      );
    });

    final statusText = _streakStatusText(
      widget.activityDates,
      widget.currentStreak,
      l10n,
    );

    return Card(
      elevation: 0,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24)),
      clipBehavior: Clip.antiAlias,
      child: Container(
        padding: const EdgeInsets.all(20),
        decoration: BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [
              Theme.of(
                context,
              ).colorScheme.surfaceContainerHighest.withValues(alpha: 0.42),
              Theme.of(context).colorScheme.surface,
            ],
          ),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                const Icon(
                  Icons.local_fire_department,
                  color: AppTheme.warningColor,
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        l10n.streakTimelineTitle,
                        style: const TextStyle(
                          fontSize: 18,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      const SizedBox(height: 2),
                      Text(
                        l10n.streakTimelineSubtitle,
                        style: TextStyle(
                          fontSize: 12,
                          color: Theme.of(context).colorScheme.onSurfaceVariant,
                        ),
                      ),
                    ],
                  ),
                ),
                Column(
                  crossAxisAlignment: CrossAxisAlignment.end,
                  children: [
                    Text(
                      l10n.streakTimelineCurrent,
                      style: TextStyle(
                        fontSize: 11,
                        color: Theme.of(context).colorScheme.onSurfaceVariant,
                      ),
                    ),
                    Text(
                      l10n.streakTimelineDays(widget.currentStreak),
                      style: const TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ],
                ),
              ],
            ),
            const SizedBox(height: 10),
            Text(
              statusText,
              style: TextStyle(
                fontSize: 12,
                color: Theme.of(context).colorScheme.onSurfaceVariant,
              ),
            ),
            const SizedBox(height: 16),
            SizedBox(
              height: 84,
              child: ListView.separated(
                scrollDirection: Axis.horizontal,
                physics: const BouncingScrollPhysics(),
                controller: _scrollController,
                itemCount: days.length,
                separatorBuilder: (_, _) => const SizedBox(width: 8),
                itemBuilder: (context, index) {
                  return SizedBox(
                    width: 56,
                    child: _buildDayTile(context, days[index], l10n),
                  );
                },
              ),
            ),
            const SizedBox(height: 14),
            Wrap(
              spacing: 10,
              runSpacing: 10,
              children: [
                _TimelineLegendDot(
                  color: AppTheme.taskColor,
                  label: l10n.globalLabelTask,
                ),
                _TimelineLegendDot(
                  color: AppTheme.habitColor,
                  label: l10n.globalLabelHabit,
                ),
                _TimelineLegendDot(
                  color: AppTheme.focusColor,
                  label: l10n.focusTitle,
                ),
                _TimelineLegendDot(
                  color: AppTheme.warningColor,
                  label: l10n.streakLegendStreakDay,
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Set<String> _buildCurrentStreakDayKeys(List<DateTime> dates) {
    final dayKeys = dates.map(AppDateUtils.dayKey).toSet();
    if (dayKeys.isEmpty) return {};

    var checkDate = DateTime.now();
    if (!dayKeys.contains(AppDateUtils.dayKey(checkDate))) {
      final yesterday = checkDate.subtract(const Duration(days: 1));
      if (!dayKeys.contains(AppDateUtils.dayKey(yesterday))) return {};
      checkDate = yesterday;
    }

    final streakKeys = <String>{};
    while (dayKeys.contains(AppDateUtils.dayKey(checkDate))) {
      streakKeys.add(AppDateUtils.dayKey(checkDate));
      checkDate = checkDate.subtract(const Duration(days: 1));
    }
    return streakKeys;
  }

  String _streakStatusText(
    List<DateTime> activityDates,
    int currentStreak,
    AppLocalizations l10n,
  ) {
    if (activityDates.isEmpty) {
      return l10n.streakStatusNone;
    }

    final todayKey = AppDateUtils.dayKey(DateTime.now());
    final yesterdayKey = AppDateUtils.dayKey(
      DateTime.now().subtract(const Duration(days: 1)),
    );
    final dayKeys = activityDates.map(AppDateUtils.dayKey).toSet();

    if (dayKeys.contains(todayKey) && currentStreak > 0) {
      return l10n.streakStatusActive;
    }

    if (dayKeys.contains(yesterdayKey) && currentStreak > 0) {
      return l10n.streakStatusFrozen;
    }

    if (dayKeys.contains(todayKey)) {
      return l10n.streakStatusBuilding;
    }

    return l10n.streakStatusStart;
  }

  Widget _buildDayTile(
    BuildContext context,
    _StreakDayInfo info,
    AppLocalizations l10n,
  ) {
    final settings = context.watch<SettingsProvider>();
    final backgroundColor = info.isToday
        ? AppTheme.taskColor.withValues(alpha: 0.12)
        : info.inCurrentStreak
        ? AppTheme.warningColor.withValues(alpha: 0.14)
        : info.hasTask || info.hasHabit || info.hasFocus
        ? Theme.of(
            context,
          ).colorScheme.surfaceContainerHighest.withValues(alpha: 0.7)
        : Theme.of(
            context,
          ).colorScheme.surfaceContainerHighest.withValues(alpha: 0.35);

    final borderColor = info.isToday
        ? AppTheme.taskColor
        : info.inCurrentStreak
        ? AppTheme.warningColor
        : Theme.of(context).dividerColor.withValues(alpha: 0.6);

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 7),
      decoration: BoxDecoration(
        color: backgroundColor,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: borderColor, width: info.isToday ? 1.8 : 1),
      ),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(
            info.isToday
                ? l10n.streakDayToday
                : settings.getFormattedWeekday(info.date),
            textAlign: TextAlign.center,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: TextStyle(
              fontSize: 9,
              fontWeight: FontWeight.bold,
              color: info.isToday
                  ? AppTheme.taskColor
                  : Theme.of(context).colorScheme.onSurfaceVariant,
            ),
          ),
          Text(
            '${info.date.day}',
            style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
          ),
          Row(
            mainAxisSize: MainAxisSize.min,
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              _dot(info.hasTask, AppTheme.taskColor),
              const SizedBox(width: 2),
              _dot(info.hasHabit, AppTheme.habitColor),
              const SizedBox(width: 2),
              _dot(info.hasFocus, AppTheme.focusColor),
            ],
          ),
        ],
      ),
    );
  }

  Widget _dot(bool active, Color color) {
    return Container(
      width: 7,
      height: 7,
      decoration: BoxDecoration(
        color: active ? color : color.withValues(alpha: 0.12),
        shape: BoxShape.circle,
      ),
    );
  }
}

class _StreakDayInfo {
  final DateTime date;
  final bool isToday;
  final bool inCurrentStreak;
  final bool hasTask;
  final bool hasFocus;
  final bool hasHabit;

  const _StreakDayInfo({
    required this.date,
    required this.isToday,
    required this.inCurrentStreak,
    required this.hasTask,
    required this.hasFocus,
    required this.hasHabit,
  });
}

class _TimelineLegendDot extends StatelessWidget {
  final Color color;
  final String label;

  const _TimelineLegendDot({required this.color, required this.label});

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Container(
          width: 10,
          height: 10,
          decoration: BoxDecoration(color: color, shape: BoxShape.circle),
        ),
        const SizedBox(width: 6),
        Text(
          label,
          style: TextStyle(
            fontSize: 11,
            color: Theme.of(context).colorScheme.onSurfaceVariant,
          ),
        ),
      ],
    );
  }
}
