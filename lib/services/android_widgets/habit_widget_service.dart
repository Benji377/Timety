import 'dart:ui' as ui;
import 'package:flutter/material.dart';
import 'package:home_widget/home_widget.dart';
import '../../data/habit/habit_models.dart';
import '../../providers/habit_provider.dart';
import '../../utils/habit_utils.dart';
import '../../widgets/android_widgets/habit_widget_view.dart';
import '../../l10n/app_localizations.dart';

class HabitWidgetService {
  static const String _groupId = 'io.github.benji377.timety';
  static const String _androidWidgetName = 'HabitWidgetProvider';

  static Future<void> updateHabitWidget(
    List<Habit> allHabits,
    HabitProvider provider,
    Locale userLocale,
  ) async {
    try {
      await HomeWidget.setAppGroupId(_groupId);

      final now = DateTime.now();
      final todayHabits = provider.getHabitsForDay(now);
      final l10n = lookupAppLocalizations(userLocale);

      final completionStatus = <String, bool>{};
      final stackCompletions = <String, int>{};
      final stackTotals = <String, int>{};

      for (var habit in todayHabits) {
        final isDone = provider.isCompletedOn(habit, now);
        completionStatus[habit.id] = isDone;
        if (habit.stackName != null && habit.stackName!.isNotEmpty) {
          final stackName = habit.stackName!.trim();
          stackTotals[stackName] = (stackTotals[stackName] ?? 0) + 1;
          if (isDone) {
            stackCompletions[stackName] =
                (stackCompletions[stackName] ?? 0) + 1;
          }
        }
      }

      final grouped = <String, List<Habit>>{};
      final standalone = <Habit>[];
      for (var h in todayHabits) {
        if (h.stackName != null && h.stackName!.trim().isNotEmpty) {
          grouped.putIfAbsent(h.stackName!.trim(), () => []).add(h);
        } else {
          standalone.add(h);
        }
      }

      // 1. Render Header
      final headerPath = await HomeWidget.renderFlutterWidget(
        _wrap(HabitWidgetHeaderView(habitCount: todayHabits.length)),
        key: 'habit_widget_header',
        logicalSize: const ui.Size(400, 56),
        pixelRatio: 2.0,
      );
      await HomeWidget.saveWidgetData('habit_widget_header', headerPath);

      // 2. Prepare Items to render
      final List<Widget> itemsToRender = [];

      // Add Stacks
      for (var entry in grouped.entries) {
        final stackHabits = entry.value;
        stackHabits.sort((a, b) {
          final aDone = completionStatus[a.id] ?? false;
          final bDone = completionStatus[b.id] ?? false;
          if (aDone != bDone) return aDone ? 1 : -1;
          return (a.stackOrder ?? 99).compareTo(b.stackOrder ?? 99);
        });

        // Recalculate locks for this sorted list
        final Map<String, bool> stackLocks = {};
        for (var i = 0; i < stackHabits.length; i++) {
          final habit = stackHabits[i];
          if (i == 0) {
            stackLocks[habit.id] = false;
          } else {
            final prevDone = completionStatus[stackHabits[i - 1].id] ?? false;
            stackLocks[habit.id] =
                !prevDone && !(completionStatus[habit.id] ?? false);
          }
        }

        itemsToRender.add(
          Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              HabitStackHeaderView(
                name: entry.key,
                completed: stackCompletions[entry.key] ?? 0,
                total: stackTotals[entry.key] ?? 0,
              ),
              for (var h in stackHabits)
                HabitWidgetItemView(
                  habit: h,
                  isDone: completionStatus[h.id] ?? false,
                  isLocked: stackLocks[h.id] ?? false,
                  frequency: HabitUtils.buildHabitSubtitle(h, l10n, provider.getCompletionsThisWeek(h)),
                  isStacked: true,
                ),
              const HabitStackFooterView(),
            ],
          ),
        );
      }

      // Add Standalone
      for (var h in standalone) {
        itemsToRender.add(
          HabitWidgetItemView(
            habit: h,
            isDone: completionStatus[h.id] ?? false,
            isLocked: false,
            frequency: HabitUtils.buildHabitSubtitle(h, l10n, provider.getCompletionsThisWeek(h)),
          ),
        );
      }

      // 3. Render Items
      for (var i = 0; i < itemsToRender.length; i++) {
        final item = itemsToRender[i];
        double height = 38; // standalone height

        if (item is Column) {
          // It's a stack: Header (36) + (N * 26) + Footer (6)
          final habitCount = item.children
              .whereType<HabitWidgetItemView>()
              .length;
          height = 36.0 + (habitCount * 26.0) + 6.0;
        }

        final itemPath = await HomeWidget.renderFlutterWidget(
          _wrap(item),
          key: 'habit_item_$i',
          logicalSize: ui.Size(400, height),
          pixelRatio: 2.0,
        );
        await HomeWidget.saveWidgetData('habit_item_$i', itemPath);
      }

      await HomeWidget.saveWidgetData('habit_item_count', itemsToRender.length);

      await HomeWidget.updateWidget(
        androidName: _androidWidgetName,
        qualifiedAndroidName: 'io.github.benji377.timety.HabitWidgetProvider',
      );
    } catch (e) {
      final errorStr = e.toString();
      if (!errorStr.contains('MissingPluginException') &&
          !errorStr.contains('Binding has not yet been initialized')) {
        debugPrint('Error updating habit widget: $e');
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
