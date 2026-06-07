import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../utils/datetime/date_utils.dart';
import '../../l10n/app_localizations.dart';
import '../../providers/settings_provider.dart';

class WeekNavigator extends StatelessWidget {
  final DateTime focusedDate;
  final ValueChanged<int> onShiftWeek;

  const WeekNavigator({
    super.key,
    required this.focusedDate,
    required this.onShiftWeek,
  });

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final settings = context.watch<SettingsProvider>();
    final startOfWeek = AppDateUtils.startOfWeekMonday(focusedDate);
    final endOfWeek = startOfWeek.add(
      const Duration(days: 6, hours: 23, minutes: 59, seconds: 59),
    );

    final now = DateTime.now();
    final isCurrentWeek = AppDateUtils.isWithinInclusive(
      now,
      startOfWeek,
      endOfWeek,
    );
    final weekRangeLabel =
        '${settings.getFormattedShortDate(startOfWeek)} - ${settings.getFormattedDate(endOfWeek)}';

    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        IconButton(
          icon: const Icon(Icons.chevron_left),
          onPressed: () => onShiftWeek(-7),
        ),
        Column(
          children: [
            Text(
              isCurrentWeek ? l10n.weekNavThisWeek : l10n.weekNavPastWeek,
              style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
            ),
            Text(
              weekRangeLabel,
              style: const TextStyle(color: Colors.grey, fontSize: 12),
            ),
          ],
        ),
        IconButton(
          icon: const Icon(Icons.chevron_right),
          onPressed: isCurrentWeek ? null : () => onShiftWeek(7),
        ),
      ],
    );
  }
}
