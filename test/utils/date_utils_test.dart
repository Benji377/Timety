import 'package:flutter_test/flutter_test.dart';
import 'package:timety/utils/date_utils.dart';

void main() {
  group('AppDateUtils', () {
    test('isSameDay ignores time and handles nulls', () {
      final first = DateTime(2026, 5, 11, 8, 15);
      final second = DateTime(2026, 5, 11, 23, 59);

      expect(AppDateUtils.isSameDay(first, second), isTrue);
      expect(AppDateUtils.isSameDay(first, null), isFalse);
      expect(AppDateUtils.isSameDay(null, second), isFalse);
    });

    test('startOfWeekMonday returns the Monday of the current week', () {
      final wednesday = DateTime(2026, 5, 13, 14, 30);

      expect(AppDateUtils.startOfWeekMonday(wednesday), DateTime(2026, 5, 11));
    });

    test('isWithinInclusive respects both boundaries', () {
      final start = DateTime(2026, 5, 10);
      final end = DateTime(2026, 5, 20);

      expect(
        AppDateUtils.isWithinInclusive(DateTime(2026, 5, 10), start, end),
        isTrue,
      );
      expect(
        AppDateUtils.isWithinInclusive(DateTime(2026, 5, 15), start, end),
        isTrue,
      );
      expect(
        AppDateUtils.isWithinInclusive(DateTime(2026, 5, 20), start, end),
        isTrue,
      );
      expect(
        AppDateUtils.isWithinInclusive(DateTime(2026, 5, 9), start, end),
        isFalse,
      );
      expect(
        AppDateUtils.isWithinInclusive(DateTime(2026, 5, 21), start, end),
        isFalse,
      );
    });

    test('dayKey zero pads month and day', () {
      expect(AppDateUtils.dayKey(DateTime(2026, 1, 5)), '2026-01-05');
    });
  });
}
