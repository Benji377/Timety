import 'package:flutter/material.dart';

import '../../data/task/task.dart';
import '../../theme/app_theme.dart';

class AppUtils {
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
