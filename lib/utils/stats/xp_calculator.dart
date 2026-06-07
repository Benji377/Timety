import 'dart:math';

import 'package:flutter/material.dart';

import '../../theme/app_theme.dart';

/// Engine for calculating user experience points (XP), levels, and titles.
class ExperienceEngine {
  static const int xpPerTask = 15;
  static const int xpPerHabit = 10;
  static const int xpPerFocusMin = 1;

  /// Calculates the user level based on total XP.
  static int calculateLevel(int totalXp) {
    return (sqrt(totalXp / 100)).floor() + 1;
  }

  /// Calculates the required XP to reach a specific level.
  static int getXpForLevel(int level) {
    return 100 * pow(level - 1, 2).toInt();
  }

  /// Generates a descriptive title based on the user level.
  static String getTitle(int level) {
    if (level < 5) return "Novice Planner";
    if (level < 10) return "Focus Apprentice";
    if (level < 20) return "Deep Work Adept";
    if (level < 35) return "Time Master";
    if (level < 50) return "Productivity Lord";
    if (level < 100) return "Timety Legend";
    return "Time God"; // For the absolute madmen who reach level 100+
  }

  /// Returns the corresponding icon for the user level.
  static IconData getTitleIcon(int level) {
    if (level < 5) return Icons.emoji_events_outlined;
    if (level < 10) return Icons.auto_awesome;
    if (level < 20) return Icons.bolt;
    if (level < 35) return Icons.local_fire_department;
    if (level < 50) return Icons.workspace_premium_outlined;
    if (level < 100) return Icons.rocket_launch_outlined;
    return Icons.star_rounded;
  }

  /// Returns the corresponding color theme for the user level.
  static Color getTitleColor(int level) {
    if (level < 5) return AppTheme.warningColor;
    if (level < 10) return const Color(0xFF8E6CFF);
    if (level < 20) return AppTheme.taskColor;
    if (level < 35) return const Color(0xFFFF7A45);
    if (level < 50) return const Color(0xFF4E9F3D);
    if (level < 100) return const Color(0xFFB23A48);
    return const Color(0xFF1E88E5);
  }

  /// Returns progress to the NEXT level as a double (0.0 to 1.0).
  static double getLevelProgress(int totalXp) {
    final int currentLevel = calculateLevel(totalXp);
    final int currentTierXp = getXpForLevel(currentLevel);
    final int nextTierXp = getXpForLevel(currentLevel + 1);

    final int xpIntoLevel = totalXp - currentTierXp;
    final int xpNeededForNext = nextTierXp - currentTierXp;

    return xpIntoLevel / xpNeededForNext;
  }
}
