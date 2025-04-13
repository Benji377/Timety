import 'package:timety/commons.dart';
import 'package:timety/features/focus/application/focus_summery_provider.dart';


class HomePage extends ConsumerWidget {
  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final String username = ref.watch(userNameProvider); 
    final summary = ref.watch(focusSummeryProvider);

    return Center(
      child: Column(
        children: [
          Row(
              children: [
                Expanded(
                    child: Padding(
                      padding: const EdgeInsets.all(8.0),
                      child: Text("Hello $username!",
                      style: TextStyle(
                        fontSize: 20,
                      ),),
                    )),
                IconButton(
                  alignment: Alignment.topRight,
                    onPressed: () => Navigator.of(context).pushNamed('/settings'),
                    icon: Icon(Icons.settings)
                ),
              ]
          ),
          Column(
              children: [
                FocusPieChart(dataMap: summary.getFocusDataMap()),
              ],
            ),
        ],
      ),
    );
  }
}
