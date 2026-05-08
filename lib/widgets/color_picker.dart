import 'package:flutter/material.dart';

/// A reusable color picker widget
///
/// Displays a row of selectable color circles and calls [onColorSelected] when a color is tapped.
class ColorPicker extends StatelessWidget {
  final Color selectedColor;
  final ValueChanged<Color> onColorSelected;
  final List<Color> colorOptions;
  final double size;
  final double spacing;

  const ColorPicker({
    super.key,
    required this.selectedColor,
    required this.onColorSelected,
    required this.colorOptions,
    this.size = 40,
    this.spacing = 16,
  });

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return Wrap(
      spacing: spacing,
      children: colorOptions.map((color) {
        final isSelected = selectedColor.toARGB32() == color.toARGB32();
        return GestureDetector(
          onTap: () => onColorSelected(color),
          child: Container(
            width: size,
            height: size,
            decoration: BoxDecoration(
              color: color,
              shape: BoxShape.circle,
              border: isSelected
                  ? Border.all(
                      color: isDark ? Colors.white : Colors.black,
                      width: 3,
                    )
                  : null,
            ),
          ),
        );
      }).toList(),
    );
  }
}
