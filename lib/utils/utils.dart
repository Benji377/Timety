import 'package:flutter/material.dart';

import '../data/task/task.dart';

class AppUtils {
  Icon getPriorityIcon(Priority p) {
    switch (p) {
      case Priority.low:
        return const Icon(Icons.keyboard_arrow_down, color: Colors.blue);
      case Priority.medium:
        return const Icon(Icons.drag_handle, color: Colors.orange);
      case Priority.high:
        return const Icon(Icons.keyboard_arrow_up, color: Colors.red);
      case Priority.veryHigh:
        return const Icon(
          Icons.keyboard_double_arrow_up,
          color: Colors.redAccent,
        );
    }
  }

  String getSizeEmoji(Size s) {
    switch (s) {
      case Size.small:
        return '🐁';
      case Size.medium:
        return '🐕';
      case Size.large:
        return '🐎';
      case Size.veryLarge:
        return '🐳';
    }
  }
}
