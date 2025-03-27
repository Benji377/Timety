import 'package:timety/commons.dart';

// I would like to go with a purple-like theme
final colorScheme = ColorScheme.fromSeed(
    seedColor: Colors.purple,
    brightness: Brightness.dark
);

ThemeData timetyTheme() {
  return ThemeData(
    useMaterial3: true,
    colorScheme: colorScheme,
    brightness: Brightness.dark,
  );
}

