import 'package:timety/commons.dart';

class FocusPieChart extends StatelessWidget {
  final Map<String, double> dataMap;
  const FocusPieChart({super.key, required this.dataMap});

  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: Stack(
          children: [
            Center(
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text("This should be center"),
                  ],
                )
            ),
            Center(
              child: RadialGauge(
                radiusFactor: 1,
                valueBar: [
                  RadialValueBar(
                    value: 100,
                    color: Colors.green.withValues(alpha: 0.7),
                    valueBarThickness: 2,
                    radialOffset: 60,
                  ),
                  const RadialValueBar(
                    value: 10,
                    color: Colors.green,
                    valueBarThickness: 13,
                    radialOffset: 60,
                  ),
                  RadialValueBar(
                    value: 100,
                    color: Colors.blue.withValues(alpha: 0.7),
                    valueBarThickness: 2,
                    radialOffset: 30,
                  ),
                  const RadialValueBar(
                    value: 30,
                    color: Colors.blue,
                    valueBarThickness: 12,
                    radialOffset: 30,
                  ),
                ],
                track: const RadialTrack(
                  color: Colors.red,
                  startAngle: 90,
                  hideLabels: true,
                  endAngle: 450,
                  steps: 10,
                  trackStyle: TrackStyle(
                    primaryRulersHeight: 10,
                    secondaryRulersWidth: 1,
                    showLabel: false,
                    showPrimaryRulers: false,
                    showSecondaryRulers: false,
                  ),
                  hideTrack: true,
                  // hideStartLabel: ,
                  start: 0,
                  thickness: 10,
                  end: 100,
                ),
              ),
            ),
          ],
      ),
    );
  }
}