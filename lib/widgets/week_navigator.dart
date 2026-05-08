import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../utils/date_utils.dart';

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
        '${DateFormat('MMM d').format(startOfWeek)} - ${DateFormat('MMM d, yyyy').format(endOfWeek)}';

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
              isCurrentWeek ? 'This Week' : 'Past Week',
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
