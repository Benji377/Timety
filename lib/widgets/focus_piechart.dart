import 'package:timety/commons.dart';

class FocusPieChart extends StatelessWidget {
  final Map<String, double> dataMap;
  const FocusPieChart({super.key, required this.dataMap});

  @override
  Widget build(BuildContext context) {
    return PieChart(
      dataMap: dataMap,
      chartType: ChartType.ring,
      baseChartColor: Colors.grey,
      centerTextStyle: TextStyle(
        fontSize: 25,
      ),
      centerText: "${dataMap["time"]} / ${dataMap["target"]} hours",
      legendOptions: LegendOptions(showLegends: false),
      chartValuesOptions: ChartValuesOptions(
          showChartValuesInPercentage: true,
        showChartValueBackground: false,
      ),
    );
  }
}