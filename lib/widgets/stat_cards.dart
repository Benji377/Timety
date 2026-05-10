import 'package:flutter/material.dart';
import '../theme/app_theme.dart';

class KpiStatCard extends StatelessWidget {
  final String title;
  final String value;
  final IconData icon;
  final Color color;

  const KpiStatCard({
    super.key,
    required this.title,
    required this.value,
    required this.icon,
    required this.color,
  });

  @override
  Widget build(BuildContext context) {
    return Card(
      elevation: 0,
      shape: RoundedRectangleBorder(
        side: BorderSide(color: color, width: AppTheme.neoBorderWidth),
        borderRadius: AppTheme.brNeo,
      ),
      color: color.withValues(alpha: 0.08),
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
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
        ),
      ),
    );
  }
}

class CompactVerticalStatCard extends StatelessWidget {
  final String title;
  final String value;
  final IconData icon;
  final Color color;

  const CompactVerticalStatCard({
    super.key,
    required this.title,
    required this.value,
    required this.icon,
    required this.color,
  });

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: 105,
      height: 90,
      child: Card(
        elevation: 0,
        shape: RoundedRectangleBorder(
          side: BorderSide(color: color, width: AppTheme.neoBorderWidth),
          borderRadius: AppTheme.brNeo,
        ),
        color: color.withValues(alpha: 0.08),
        child: Padding(
          padding: const EdgeInsets.all(AppTheme.spaceSmall),
          child: Column(
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
          ),
        ),
      ),
    );
  }
}

class CompactHeaderStatCard extends StatelessWidget {
  final String title;
  final String value;
  final IconData icon;
  final Color color;

  const CompactHeaderStatCard({
    super.key,
    required this.title,
    required this.value,
    required this.icon,
    required this.color,
  });

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: 140,
      height: 100,
      child: Card(
        elevation: 0,
        shape: RoundedRectangleBorder(
          side: BorderSide(color: color, width: AppTheme.neoBorderWidth),
          borderRadius: AppTheme.brNeo,
        ),
        color: color.withValues(alpha: 0.08),
        child: Padding(
          padding: const EdgeInsets.all(AppTheme.spaceMedium),
          child: Column(
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
          ),
        ),
      ),
    );
  }
}
