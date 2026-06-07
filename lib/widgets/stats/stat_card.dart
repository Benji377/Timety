import 'package:flutter/material.dart';
import '../../../theme/app_theme.dart';

/// Enum for different stat card layout styles
enum StatCardStyle {
  /// Full-size card with icon, value, and title stacked vertically
  kpi,

  /// Compact fixed-size card (105x90) with vertical layout
  compactVertical,

  /// Compact header card (140x100) with horizontal icon + title and value below
  compactHeader,
}

/// Unified stat card widget supporting multiple layout styles
class StatCard extends StatelessWidget {
  final String title;
  final String value;
  final IconData icon;
  final Color color;
  final StatCardStyle style;

  const StatCard({
    super.key,
    required this.title,
    required this.value,
    required this.icon,
    required this.color,
    this.style = StatCardStyle.kpi,
  });

  @override
  Widget build(BuildContext context) {
    final card = _buildCardContent(context);

    // Apply size constraints if needed
    if (style == StatCardStyle.compactVertical) {
      return SizedBox(width: 105, height: 90, child: card);
    } else if (style == StatCardStyle.compactHeader) {
      return SizedBox(width: 140, height: 100, child: card);
    }

    return card;
  }

  Widget _buildCardContent(BuildContext context) {
    return Card(
      elevation: 0,
      shape: RoundedRectangleBorder(
        side: BorderSide(color: color, width: AppTheme.neoBorderWidth),
        borderRadius: AppTheme.brNeo,
      ),
      color: color.withValues(alpha: 0.08),
      child: Padding(padding: _getPadding(), child: _buildContent(context)),
    );
  }

  EdgeInsetsGeometry _getPadding() {
    return switch (style) {
      StatCardStyle.kpi => const EdgeInsets.all(16.0),
      StatCardStyle.compactVertical => const EdgeInsets.all(
        AppTheme.spaceSmall,
      ),
      StatCardStyle.compactHeader => const EdgeInsets.all(AppTheme.spaceMedium),
    };
  }

  Widget _buildContent(BuildContext context) {
    return switch (style) {
      StatCardStyle.kpi => _buildKpiContent(context),
      StatCardStyle.compactVertical => _buildCompactVerticalContent(context),
      StatCardStyle.compactHeader => _buildCompactHeaderContent(context),
    };
  }

  Widget _buildKpiContent(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Icon(icon, color: color),
        const SizedBox(height: 12),
        Text(
          value,
          style: const TextStyle(fontSize: 24, fontWeight: FontWeight.bold),
        ),
        Text(
          title,
          style: TextStyle(
            fontSize: 12,
            color: Theme.of(context).colorScheme.onSurfaceVariant,
          ),
        ),
      ],
    );
  }

  Widget _buildCompactVerticalContent(BuildContext context) {
    return Column(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        Icon(icon, color: color, size: AppTheme.iconSizeSmall),
        const Spacer(),
        Text(
          value,
          style: const TextStyle(
            fontSize: AppTheme.fsHeadingMedium,
            fontWeight: AppTheme.fwExtraBold,
          ),
        ),
        Text(
          title,
          textAlign: TextAlign.center,
          style: TextStyle(
            fontSize: AppTheme.fsCaption,
            color: Theme.of(context).colorScheme.onSurfaceVariant,
            fontWeight: AppTheme.fwBold,
          ),
        ),
      ],
    );
  }

  Widget _buildCompactHeaderContent(BuildContext context) {
    return Column(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(icon, color: color, size: AppTheme.iconSizeSmall),
            const SizedBox(width: AppTheme.spaceXSmall),
            Text(
              title,
              style: TextStyle(
                fontSize: AppTheme.fsCaption,
                color: Theme.of(context).colorScheme.onSurfaceVariant,
                fontWeight: AppTheme.fwBold,
              ),
            ),
          ],
        ),
        const Spacer(),
        Text(
          value,
          style: const TextStyle(
            fontSize: AppTheme.fsHeadingMedium,
            fontWeight: AppTheme.fwExtraBold,
          ),
        ),
      ],
    );
  }
}
