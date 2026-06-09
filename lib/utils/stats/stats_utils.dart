/// Utility functions for statistical calculations.
class StatsUtils {
  const StatsUtils._();

  /// Finds the maximum value in an iterable, with an optional minimum baseline.
  static double maxValue(Iterable<num> values, {double minimum = 0}) {
    var maxValue = minimum;
    for (final value in values) {
      if (value > maxValue) {
        maxValue = value.toDouble();
      }
    }
    return maxValue;
  }
}
