import 'dart:ui' as ui;
import 'package:flutter/material.dart';
import 'package:home_widget/home_widget.dart';
import '../data/task/task.dart';
import '../widgets/home_widget_view.dart';

class HomeWidgetService {
  static const String _groupId = 'io.github.benji377.timety';
  static const String _androidWidgetName = 'TaskWidgetProvider';

  static Future<void> updateTaskWidget(List<Task> tasks) async {
    try {
      // Set the group ID first to ensure consistent SharedPreferences access
      await HomeWidget.setAppGroupId(_groupId);

      // Filter urgent tasks (due today or overdue)
      final today = DateTime.now();
      final todayDate = DateTime(today.year, today.month, today.day);
      
      final urgentTasks = tasks.where((task) {
        if (task.isCompleted || task.dueDate == null) return false;
        final dueDay = DateTime(
          task.dueDate!.year,
          task.dueDate!.month,
          task.dueDate!.day,
        );
        return dueDay.isBefore(todayDate) || dueDay.isAtSameMomentAs(todayDate);
      }).toList();
      urgentTasks.sort((a, b) => a.dueDate!.compareTo(b.dueDate!));

      // Render the widget to an image
      final path = await HomeWidget.renderFlutterWidget(
        MediaQuery(
          data: const MediaQueryData(),
          child: Directionality(
            textDirection: TextDirection.ltr,
            child: Material(
              type: MaterialType.transparency,
              child: HomeWidgetView(tasks: urgentTasks),
            ),
          ),
        ),
        key: 'task_widget_image',
        logicalSize: const ui.Size(400, 250),
        pixelRatio: 2.0,
      );

      // Explicitly save the path so the native side can find it
      await HomeWidget.saveWidgetData('task_widget_image', path);

      // Update the widget
      await HomeWidget.updateWidget(
        androidName: _androidWidgetName,
        qualifiedAndroidName: 'io.github.benji377.timety.TaskWidgetProvider',
      );
    } catch (e) {
      debugPrint('Error updating home widget: $e');
    }
  }
}
