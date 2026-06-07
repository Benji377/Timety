import 'dart:ui' as ui;
import 'package:flutter/material.dart';
import 'package:home_widget/home_widget.dart';
import '../../data/task/task.dart';
import '../../widgets/android_widgets/task/task_widget_header_view.dart';
import '../../widgets/android_widgets/task/task_widget_item_view.dart';
import '../../l10n/app_localizations.dart';

class TaskWidgetService {
  static const String _groupId = 'io.github.benji377.timety';
  static const String _androidWidgetName = 'TaskWidgetProvider';

  static Future<void> updateTaskWidget(List<Task> tasks, Locale userLocale,) async {
    try {
      await HomeWidget.setAppGroupId(_groupId);

      final today = DateTime.now();
      final todayDate = DateTime(today.year, today.month, today.day);
      final l10n = lookupAppLocalizations(userLocale);

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

      // Render Header
      final headerPath = await HomeWidget.renderFlutterWidget(
        _wrap(TaskWidgetHeaderView(taskCount: urgentTasks.length, title: l10n.widgetTasksDue(urgentTasks.length))),
        key: 'task_widget_header',
        logicalSize: const ui.Size(400, 60),
        pixelRatio: 2.0,
      );
      await HomeWidget.saveWidgetData('task_widget_header', headerPath);

      // Render Items
      for (var i = 0; i < urgentTasks.length; i++) {
        final itemPath = await HomeWidget.renderFlutterWidget(
          _wrap(TaskWidgetItemView(task: urgentTasks[i])),
          key: 'task_item_$i',
          logicalSize: const ui.Size(400, 65),
          pixelRatio: 2.0,
        );
        await HomeWidget.saveWidgetData('task_item_$i', itemPath);
      }

      // Save count for the native factory
      await HomeWidget.saveWidgetData('task_item_count', urgentTasks.length);

      await HomeWidget.updateWidget(
        androidName: _androidWidgetName,
        qualifiedAndroidName: 'io.github.benji377.timety.TaskWidgetProvider',
      );
    } catch (e) {
      final errorStr = e.toString();
      if (!errorStr.contains('MissingPluginException') &&
          !errorStr.contains('Binding has not yet been initialized')) {
        debugPrint('Error updating home widget: $e');
      }
    }
  }

  static Widget _wrap(Widget child) {
    return MediaQuery(
      data: const MediaQueryData(),
      child: Directionality(
        textDirection: TextDirection.ltr,
        child: Material(type: MaterialType.transparency, child: child),
      ),
    );
  }
}
