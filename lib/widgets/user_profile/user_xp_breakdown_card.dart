import 'package:flutter/material.dart';

import '../../theme/app_theme.dart';
import '../../utils/logic/xp_utils.dart';

import '../../data/user/xp_source_row_data.dart';
import './xp_source_row.dart';

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

    final sources = <XpSourceRowData>[
      const XpSourceRowData(
        icon: Icons.task_alt,
        label: 'Tasks',
        description: 'Each completed task adds',
        value: '+${ExperienceEngine.xpPerTask} XP',
        color: AppTheme.taskColor,
      ),
      const XpSourceRowData(
        icon: Icons.favorite_outline,
        label: 'Habits',
        description: 'Each completed habit adds',
        value: '+${ExperienceEngine.xpPerHabit} XP',
        color: AppTheme.habitColor,
      ),
      const XpSourceRowData(
        icon: Icons.timer,
        label: 'Focus',
        description: 'Every focus minute adds',
        value: '+${ExperienceEngine.xpPerFocusMin} XP',
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
                        'Level ${widget.currentLevel} | ${widget.levelTitle}',
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
                        '${widget.totalXp} XP total | $xpToNextLevel XP to next level',
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
                      'How XP is earned',
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
                            XpSourceRow(data: sources[index]),
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

