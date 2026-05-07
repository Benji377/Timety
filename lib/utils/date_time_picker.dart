import 'package:flutter/material.dart';

class AppDatePickers {
  /// Opens a DatePicker followed by a TimePicker and returns the combined DateTime.
  static Future<DateTime?> pickDateTime({
    required BuildContext context,
    DateTime? initialDate,
    DateTime? firstDate,
    DateTime? lastDate,
    TimeOfDay? initialTime,
    TimeOfDay? fallbackTime, // Used if the user skips the time picker
  }) async {
    final now = DateTime.now();

    // 1. Pick the Date
    final pickedDate = await showDatePicker(
      context: context,
      initialDate: initialDate ?? now,
      firstDate: firstDate ?? now,
      lastDate: lastDate ?? DateTime(2100),
    );

    // Stop if user canceled or context was unmounted
    if (pickedDate == null || !context.mounted) return null;

    // 2. Pick the Time (forced 24h format)
    final pickedTime = await showTimePicker(
      context: context,
      initialTime: initialTime ?? const TimeOfDay(hour: 12, minute: 0),
      builder: (context, child) {
        return MediaQuery(
          data: MediaQuery.of(context).copyWith(alwaysUse24HourFormat: true),
          child: child!,
        );
      },
    );

    // 3. Merge them together
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
}
