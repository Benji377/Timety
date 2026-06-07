import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../providers/settings_provider.dart';

class AppDatePickers {
  // Opens a DatePicker followed by a TimePicker and returns the combined DateTime.
  static Future<DateTime?> pickDateTime({
    required BuildContext context,
    DateTime? initialDate,
    DateTime? firstDate,
    DateTime? lastDate,
    TimeOfDay? initialTime,
    TimeOfDay? fallbackTime, // Used if the user skips the time picker
  }) async {
    final now = DateTime.now();

    // Pick the Date
    final pickedDate = await showDatePicker(
      context: context,
      initialDate: initialDate ?? now,
      firstDate: firstDate ?? now,
      lastDate: lastDate ?? DateTime(2100),
    );

    // Stop if user canceled or context was unmounted
    if (pickedDate == null || !context.mounted) return null;

    final use24Hour = context.read<SettingsProvider>().use24HourFormat;
    // Pick the Time
    final pickedTime = await showTimePicker(
      context: context,
      initialTime: initialTime ?? const TimeOfDay(hour: 12, minute: 0),
      builder: (BuildContext context, Widget? child) {
        return MediaQuery(
          data: MediaQuery.of(
            context,
          ).copyWith(alwaysUse24HourFormat: use24Hour),
          child: child!,
        );
      },
    );

    // Merge them together
    final finalTime = pickedTime ?? fallbackTime;

    // If the user skipped the time and no fallback was provided, cancel the whole operation
    if (finalTime == null) return null;

    return DateTime(
      pickedDate.year,
      pickedDate.month,
      pickedDate.day,
      finalTime.hour,
      finalTime.minute,
    );
  }

  static Future<TimeOfDay?> pickTime({
    required BuildContext context,
    TimeOfDay? initialTime,
    String? helpText,
  }) async {
    // Read the user's 24-hour setting
    final use24Hour = context.read<SettingsProvider>().use24HourFormat;

    // Open the time picker with the injected MediaQuery
    return await showTimePicker(
      context: context,
      initialTime: initialTime ?? const TimeOfDay(hour: 12, minute: 0),
      helpText: helpText,
      builder: (BuildContext context, Widget? child) {
        return MediaQuery(
          data: MediaQuery.of(
            context,
          ).copyWith(alwaysUse24HourFormat: use24Hour),
          child: child!,
        );
      },
    );
  }
}
