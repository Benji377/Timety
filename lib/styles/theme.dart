import 'package:timety/commons.dart';

// I would like to go with a purple-like theme
final colorScheme = ColorScheme.fromSeed(
    seedColor: Colors.pink,
    brightness: Brightness.dark,
    surface: Colors.blue,
);

ThemeData timetyTheme() {
  return ThemeData(
    useMaterial3: true,
    colorScheme: colorScheme,
    fontFamily: "Nunito",
  );
}

