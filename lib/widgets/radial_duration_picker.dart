import 'package:flutter/material.dart';
import 'package:syncfusion_flutter_gauges/gauges.dart';

import '../theme/app_theme.dart';

class RadialDurationPicker extends StatefulWidget {
  final int initialMinutes;
  final int minMinutes;
  final int maxMinutes;

  const RadialDurationPicker({
    super.key,
    required this.initialMinutes,
    this.minMinutes = 5,
    this.maxMinutes = 180,
  });

  @override
  State<RadialDurationPicker> createState() => _RadialDurationPickerState();
}

class _RadialDurationPickerState extends State<RadialDurationPicker> {
  late int _selectedMinutes;

  @override
  void initState() {
    super.initState();
    _selectedMinutes = widget.initialMinutes.clamp(
      widget.minMinutes,
      widget.maxMinutes,
    );
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final semantic = theme.extension<TimetySemanticColors>()!;

    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(20, 12, 20, 24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              width: 44,
              height: 5,
              decoration: BoxDecoration(
                color: theme.colorScheme.outlineVariant,
                borderRadius: BorderRadius.circular(999),
              ),
            ),
            const SizedBox(height: 20),
            Text('Set focus time', style: theme.textTheme.titleLarge),
            const SizedBox(height: 4),
            Text(
              'Drag the dial to choose a session length',
              style: theme.textTheme.bodyMedium?.copyWith(
                color: theme.colorScheme.onSurfaceVariant,
              ),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 20),
            SizedBox(
              height: 320,
              child: Stack(
                alignment: Alignment.center,
                children: [
                  SfRadialGauge(
                    enableLoadingAnimation: true,
                    animationDuration: 900,
                    axes: [
                      RadialAxis(
                        startAngle: 140,
                        endAngle: 40,
                        minimum: widget.minMinutes.toDouble(),
                        maximum: widget.maxMinutes.toDouble(),
                        interval: 30,
                        showLabels: true,
                        showTicks: true,
                        labelOffset: 10,
                        radiusFactor: 0.92,
                        axisLineStyle: AxisLineStyle(
                          thickness: 0.12,
                          thicknessUnit: GaugeSizeUnit.factor,
                          color: theme.colorScheme.surfaceContainerHighest,
                        ),
                        majorTickStyle: MajorTickStyle(
                          length: 0.06,
                          thickness: 2,
                          lengthUnit: GaugeSizeUnit.factor,
                          color: theme.colorScheme.outlineVariant,
                        ),
                        minorTickStyle: MinorTickStyle(
                          length: 0.03,
                          thickness: 1,
                          lengthUnit: GaugeSizeUnit.factor,
                          color: theme.colorScheme.outlineVariant.withValues(
                            alpha: 0.65,
                          ),
                        ),
                        pointers: [
                          RangePointer(
                            value: _selectedMinutes.toDouble(),
                            enableDragging: true,
                            color: semantic.focus,
                            width: 14,
                            onValueChanged: (value) {
                              setState(() {
                                _selectedMinutes = value.round().clamp(
                                  widget.minMinutes,
                                  widget.maxMinutes,
                                );
                              });
                            },
                          ),
                        ],
                      ),
                    ],
                  ),
                  Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text(
                        '$_selectedMinutes',
                        style: theme.textTheme.displayMedium?.copyWith(
                          fontWeight: FontWeight.w800,
                          color: theme.colorScheme.onSurface,
                        ),
                      ),
                      Text(
                        'minutes',
                        style: theme.textTheme.titleMedium?.copyWith(
                          color: theme.colorScheme.onSurfaceVariant,
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
            const SizedBox(height: 8),
            Row(
              children: [
                Expanded(
                  child: OutlinedButton(
                    onPressed: () => Navigator.pop(context),
                    child: const Text('Cancel'),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: FilledButton(
                    onPressed: () => Navigator.pop(context, _selectedMinutes),
                    child: const Text('Use time'),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
