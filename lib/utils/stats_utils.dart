class StatsUtils {
  const StatsUtils._();

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
