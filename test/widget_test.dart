import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:timety/main.dart';
import 'package:timety/theme/app_theme.dart';

void main() {
  testWidgets('TimetyApp renders provided home widget', (
    WidgetTester tester,
  ) async {
    await tester.pumpWidget(
      const TimetyApp(
        home: Scaffold(body: Center(child: Text('Timety'))),
      ),
    );

    expect(find.text('Timety'), findsOneWidget);
  });

  testWidgets('TimetyApp applies project theme configuration', (
    WidgetTester tester,
  ) async {
    await tester.pumpWidget(
      const TimetyApp(
        themeMode: ThemeMode.dark,
        home: Scaffold(body: SizedBox.shrink()),
      ),
    );

    final materialApp = tester.widget<MaterialApp>(find.byType(MaterialApp));
    final semanticColors = materialApp.theme?.extension<TimetySemanticColors>();

    expect(materialApp.themeMode, ThemeMode.dark);
    expect(materialApp.theme?.useMaterial3, isTrue);
    expect(semanticColors, isNotNull);
  });
}
