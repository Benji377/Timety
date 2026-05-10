import 'dart:math';

class ExperienceEngine {
  static const int xpPerTask = 15;
  static const int xpPerHabit = 10;
  static const int xpPerFocusMin = 1;

  // Formula: Level = floor(sqrt(XP / 100)) + 1
  static int calculateLevel(int totalXp) {
    return (sqrt(totalXp / 100)).floor() + 1;
  }

  // Formula: XP = 100 * (Level - 1)^2
  static int getXpForLevel(int level) {
    return 100 * pow(level - 1, 2).toInt();
  }

  // Infinite Title Generator
  static String getTitle(int level) {
    if (level < 5) return "Novice Planner";
    if (level < 10) return "Focus Apprentice";
    if (level < 20) return "Deep Work Adept";
    if (level < 35) return "Time Master";
    if (level < 50) return "Productivity Lord";
    if (level < 100) return "Timety Legend";
    return "Time God"; // For the absolute madmen who reach level 100+
  }

  // Returns progress to the NEXT level as a double (0.0 to 1.0)
  static double getLevelProgress(int totalXp) {
    final int currentLevel = calculateLevel(totalXp);
    final int currentTierXp = getXpForLevel(currentLevel);
    final int nextTierXp = getXpForLevel(currentLevel + 1);

    final int xpIntoLevel = totalXp - currentTierXp;
    final int xpNeededForNext = nextTierXp - currentTierXp;

    return xpIntoLevel / xpNeededForNext;
  }
}
