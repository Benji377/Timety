import 'package:flutter/material.dart';
import '../../theme/app_theme.dart';

/// A stylized header row for list sections (e.g., inside ExpansionTiles).
class ListSectionHeader extends StatelessWidget {
  final String title;
  final IconData icon;
  final Color color;
  final EdgeInsetsGeometry padding;
  final double iconSize;
  final double titleSize;

  const ListSectionHeader({
    super.key,
    required this.title,
    required this.icon,
    required this.color,
    this.padding = const EdgeInsets.symmetric(
      horizontal: AppTheme.spaceLarge,
      vertical: AppTheme.spaceSmall,
    ),
    this.iconSize = AppTheme.iconSizeSmall,
    this.titleSize = AppTheme.fsBodyLarge,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: padding,
      child: Row(
        children: [
          Icon(icon, color: color, size: iconSize),
          const SizedBox(width: AppTheme.spaceSmall),
          Text(
            title,
            style: TextStyle(
              fontSize: titleSize,
              fontWeight: AppTheme.fwBold,
              color: color,
            ),
          ),
        ],
      ),
    );
  }
}
