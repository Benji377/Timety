import 'commons.dart';

void main() {
  runApp(ProviderScope(
      child:MyApp()
    ));
}



class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {

      return MaterialApp(
        title: 'Timety',
        theme: timetyTheme(),
        home: MainPage(),
        routes: {
          '/home': (context) => HomePage(),
          '/settings': (context) => SettingsPage(),
          '/tasks': (context) => TasksPage(),
          '/focus': (context) => FocusPage(),
          '/stats': (context) => StatisticsPage(),
        },
      );
      
      
    
  }
}












