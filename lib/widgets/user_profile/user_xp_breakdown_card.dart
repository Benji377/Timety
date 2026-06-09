import 'package:flutter/material.dart';

import '../../theme/app_theme.dart';
import '../../utils/stats/xp_calculator.dart';
import '../../l10n/app_localizations.dart';

class UserXpBreakdownCard extends StatefulWidget {
  final int currentLevel;
  final String levelTitle;
  final int totalXp;
  final double levelProgress;

  const UserXpBreakdownCard({
    super.key,
    required this.currentLevel,
    required this.levelTitle,
    required this.totalXp,
    required this.levelProgress,
  });

  @override
  State<UserXpBreakdownCard> createState() => _UserXpBreakdownCardState();
}

class _UserXpBreakdownCardState extends State<UserXpBreakdownCard>
    with TickerProviderStateMixin {
  bool _showXpSources = false;

  @override
  Widget build(BuildContext context) {
    final nextLevelXp = ExperienceEngine.getXpForLevel(widget.currentLevel + 1);
    final xpToNextLevel = nextLevelXp - widget.totalXp;
    final l10n = AppLocalizations.of(context)!;

    final sources = <_XpSourceRowData>[
      _XpSourceRowData(
        icon: Icons.task_alt,
        label: l10n.globalLabelTasks,
        description: l10n.xpSourceTaskDesc,
        value: l10n.xpSourceValue(ExperienceEngine.xpPerTask),
        color: AppTheme.taskColor,
      ),
      _XpSourceRowData(
        icon: Icons.favorite_outline,
        label: l10n.globalLabelHabits,
        description: l10n.xpSourceHabitDesc,
        value: l10n.xpSourceValue(ExperienceEngine.xpPerHabit),
        color: AppTheme.habitColor,
      ),
      _XpSourceRowData(
        icon: Icons.timer,
        label: l10n.focusTitle,
        description: l10n.xpSourceFocusDesc,
        value: l10n.xpSourceValue(ExperienceEngine.xpPerFocusMin),
        color: AppTheme.focusColor,
      ),
    ];

    return Card(
      elevation: 0,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24)),
      clipBehavior: Clip.antiAlias,
      child: Container(
        padding: const EdgeInsets.all(20),
        decoration: BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [
              Theme.of(
                context,
              ).colorScheme.surfaceContainerHighest.withValues(alpha: 0.45),
              Theme.of(context).colorScheme.surface,
            ],
          ),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Container(
                  width: 44,
                  height: 44,
                  decoration: BoxDecoration(
                    color: ExperienceEngine.getTitleColor(
                      widget.currentLevel,
                    ).withValues(alpha: 0.12),
                    shape: BoxShape.circle,
                  ),
                  child: Icon(
                    ExperienceEngine.getTitleIcon(widget.currentLevel),
                    color: ExperienceEngine.getTitleColor(widget.currentLevel),
                  ),
                ),
                const SizedBox(width: 14),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        l10n.xpBreakdownLevel(
                          widget.currentLevel,
                          widget.levelTitle,
                        ),
                        style: TextStyle(
                          fontSize: 14,
                          fontWeight: FontWeight.bold,
                          color: ExperienceEngine.getTitleColor(
                            widget.currentLevel,
                          ),
                        ),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        l10n.xpBreakdownProgress(widget.totalXp, xpToNextLevel),
                        style: TextStyle(
                          fontSize: 12,
                          color: Theme.of(context).colorScheme.onSurfaceVariant,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: 18),
            ClipRRect(
              borderRadius: BorderRadius.circular(999),
              child: LinearProgressIndicator(
                value: widget.levelProgress.clamp(0.0, 1.0),
                minHeight: 12,
                backgroundColor: Theme.of(
                  context,
                ).colorScheme.surfaceContainerHighest,
                valueColor: const AlwaysStoppedAnimation<Color>(
                  AppTheme.taskColor,
                ),
              ),
            ),
            const SizedBox(height: 18),
            InkWell(
              borderRadius: BorderRadius.circular(12),
              onTap: () => setState(() => _showXpSources = !_showXpSources),
              child: Padding(
                padding: const EdgeInsets.symmetric(vertical: 4),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(
                      l10n.xpBreakdownHowToEarn,
                      style: TextStyle(
                        fontSize: 13,
                        fontWeight: FontWeight.bold,
                        color: Theme.of(context).colorScheme.onSurfaceVariant,
                      ),
                    ),
                    const SizedBox(width: 6),
                    AnimatedRotation(
                      turns: _showXpSources ? 0.5 : 0,
                      duration: const Duration(milliseconds: 180),
                      child: Icon(
                        Icons.keyboard_arrow_down,
                        size: 18,
                        color: Theme.of(context).colorScheme.onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              ),
            ),
            AnimatedSwitcher(
              duration: const Duration(milliseconds: 180),
              switchInCurve: Curves.easeOutCubic,
              switchOutCurve: Curves.easeInCubic,
              transitionBuilder: (child, animation) {
                final slideAnimation = Tween<Offset>(
                  begin: const Offset(0, -0.08),
                  end: Offset.zero,
                ).animate(animation);

                return ClipRect(
                  child: FadeTransition(
                    opacity: animation,
                    child: SlideTransition(
                      position: slideAnimation,
                      child: SizeTransition(
                        sizeFactor: animation,
                        alignment: Alignment.topCenter,
                        child: child,
                      ),
                    ),
                  ),
                );
              },
              child: _showXpSources
                  ? Padding(
                      key: const ValueKey('xp-sources-expanded'),
                      padding: const EdgeInsets.only(top: 10),
                      child: Column(
                        children: [
                          for (
                            var index = 0;
                            index < sources.length;
                            index++
                          ) ...[
                            _XpSourceRow(data: sources[index]),
                            if (index < sources.length - 1)
                              Divider(
                                height: 1,
                                thickness: 1,
                                indent: 16,
                                endIndent: 16,
                                color: Theme.of(
                                  context,
                                ).dividerColor.withValues(alpha: 0.45),
                              ),
                          ],
                        ],
                      ),
                    )
                  : const SizedBox(
                      key: ValueKey('xp-sources-collapsed'),
                      height: 0,
                      width: double.infinity,
                    ),
            ),
          ],
        ),
      ),
    );
  }
}

class _XpSourceRowData {
  final IconData icon;
  final String label;
  final String description;
  final String value;
  final Color color;

  const _XpSourceRowData({
    required this.icon,
    required this.label,
    required this.description,
    required this.value,
    required this.color,
  });
}

class _XpSourceRow extends StatelessWidget {
  final _XpSourceRowData data;

  const _XpSourceRow({required this.data});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
      child: Row(
        children: [
          Container(
            width: 38,
            height: 38,
            decoration: BoxDecoration(
              color: data.color.withValues(alpha: 0.12),
              shape: BoxShape.circle,
            ),
            child: Icon(data.icon, size: 18, color: data.color),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  data.label,
                  style: const TextStyle(
                    fontSize: 13,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                const SizedBox(height: 2),
                Text(
                  data.description,
                  style: TextStyle(
                    fontSize: 11,
                    color: Theme.of(context).colorScheme.onSurfaceVariant,
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(width: 12),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
            decoration: BoxDecoration(
              color: data.color.withValues(alpha: 0.1),
              borderRadius: BorderRadius.circular(999),
            ),
            child: Text(
              data.value,
              style: TextStyle(
                fontSize: 12,
                fontWeight: FontWeight.bold,
                color: data.color,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
