import 'package:flutter_test/flutter_test.dart';
import 'package:timety/utils/logic/xp_utils.dart';

void main() {
  group('ExperienceEngine', () {
    test('exposes the expected base xp values', () {
      expect(ExperienceEngine.xpPerTask, 15);
      expect(ExperienceEngine.xpPerHabit, 10);
      expect(ExperienceEngine.xpPerFocusMin, 1);
    });

    test('calculates levels, titles, and progression consistently', () {
      expect(ExperienceEngine.calculateLevel(0), 1);
      expect(ExperienceEngine.calculateLevel(150), 2);
      expect(ExperienceEngine.getXpForLevel(1), 0);
      expect(ExperienceEngine.getXpForLevel(4), 900);
      expect(ExperienceEngine.getTitle(1), 'Novice Planner');
      expect(ExperienceEngine.getTitle(5), 'Focus Apprentice');
      expect(ExperienceEngine.getTitle(20), 'Time Master');
      expect(ExperienceEngine.getTitle(100), 'Time God');
      expect(ExperienceEngine.getLevelProgress(0), 0.0);
      expect(ExperienceEngine.getLevelProgress(150), closeTo(0.166666, 0.0005));
    });
  });
}
