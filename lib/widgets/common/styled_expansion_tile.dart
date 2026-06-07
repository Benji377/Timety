import 'package:flutter/material.dart';
import '../../theme/app_theme.dart';

/// A consistently styled expansion tile used across the app.
class StyledExpansionTile extends StatelessWidget {
  final Widget title;
  final Color iconColor;
  final List<Widget> children;
  final bool initiallyExpanded;

  const StyledExpansionTile({
    super.key,
    required this.title,
    required this.iconColor,
    required this.children,
    this.initiallyExpanded = false,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: AppTheme.spaceMedium),
      child: Theme(
        data: Theme.of(context).copyWith(dividerColor: Colors.transparent),
        child: ExpansionTile(
          initiallyExpanded: initiallyExpanded,
          title: title,
          iconColor: iconColor,
          collapsedIconColor: iconColor,
          children: children,
        ),
      ),
    );
  }
}
