import 'package:timety/commons.dart';
import 'package:timety/core/providers/navigation_provider.dart';
import '../../app_pages.dart';



class MainPage extends ConsumerWidget {

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final _selectedIndex = ref.watch(bottomNavIndexProvider);
    
    final _screens = [
      HomePage(),
      FocusPage(),
      TasksPage(),
      StatisticsPage(),
    ];

    return Scaffold(
      body: Column(
        children: [
          Expanded(
            child: SafeArea(
              child: Container(
                color: Theme.of(context).colorScheme.primaryContainer,
                child: _screens[_selectedIndex],
              ),
            ),
          ),
        ],
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _selectedIndex,
        onDestinationSelected: (index) => ref.read(bottomNavIndexProvider.notifier).state = index,
        destinations: [
          NavigationDestination(
            icon: Icon(Icons.home),
            label: "HOME",
          ),
          NavigationDestination(
            icon: Icon(Icons.coffee),
            label: "FOCUS",
          ),
          NavigationDestination(
            icon: Icon(Icons.check_circle_outline),
            label: "TASKS",
          ),
          NavigationDestination(
            icon: Icon(Icons.pie_chart),
            label: "STATS",
          ),
        ],
      ),
    );
        
  }
}
