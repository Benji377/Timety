import 'package:flutter/material.dart';
import '../theme/app_theme.dart';
import 'list_section_header.dart';

class ExpansionSection extends StatelessWidget {
  final String title;
  final Color color;
  final List<Widget> children;
  final bool initiallyExpanded;
  final IconData icon;

  const ExpansionSection({
    super.key,
    required this.title,
    required this.color,
    required this.children,
    this.initiallyExpanded = true,
    this.icon = Icons.circle,
  });

  @override
  Widget build(BuildContext context) {
    if (children.isEmpty) return const SizedBox.shrink();

    return Theme(
      data: Theme.of(context).copyWith(dividerColor: Colors.transparent),
      child: ExpansionTile(
        initiallyExpanded: initiallyExpanded,
        iconColor: color,
        collapsedIconColor: color,
        title: ListSectionHeader(
          title: title,
          icon: icon,
          color: color,
          padding: EdgeInsets.zero,
          iconSize: AppTheme.listSectionIconSize,
          titleSize: AppTheme.listSectionTitleSize,
        ),
        children: children,
      ),
    );
  }
}
