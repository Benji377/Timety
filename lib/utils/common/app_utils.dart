import 'package:flutter/material.dart';

import '../../data/task/task.dart';
import '../../theme/app_theme.dart';

/// General utility functions for the application.
class AppUtils {
  /// Returns the corresponding Icon for a given [Priority].
  Icon getPriorityIcon(Priority p) {
    switch (p) {
      case Priority.low:
        return const Icon(Icons.keyboard_arrow_down, color: AppTheme.taskColor);
      case Priority.medium:
        return const Icon(Icons.drag_handle, color: AppTheme.warningColor);
      case Priority.high:
        return const Icon(Icons.keyboard_arrow_up, color: AppTheme.errorColor);
      case Priority.veryHigh:
        return const Icon(
          Icons.keyboard_double_arrow_up,
          color: AppTheme.errorColor,
        );
    }
  }

  /// Returns a short string representation (emoji/letters) for a given task [Size].
  String getSizeEmoji(Size s) {
    switch (s) {
      case Size.small:
        return 'S';
      case Size.medium:
        return 'M';
      case Size.large:
        return 'L';
      case Size.veryLarge:
        return 'XL';
    }
  }
}
