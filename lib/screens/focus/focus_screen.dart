import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:timety/screens/statistics_screen.dart';
import 'package:timety/utils/date_format_utils.dart';
import '../../theme/app_theme.dart';
import '../../providers/focus_provider.dart';
import '../../providers/user_provider.dart';
import '../../providers/settings_provider.dart';
import '../../data/focus/focus_models.dart';
import '../../widgets/focus_mode_timeline.dart';
import '../../widgets/interactive_gauge.dart';
import '../../widgets/dialogs.dart';
import '../../widgets/focus_bottom_sheet_builders.dart';
import '../calendar_screen.dart';
import 'focus_modes_screen.dart';
import '../settings_screen.dart';

class FocusScreen extends StatelessWidget {
  const FocusScreen({super.key});

  // --- BOTTOM SHEETS & ALERTS ---
  void _showDistractionSheet(BuildContext context, FocusProvider provider) {
    FocusBottomSheetBuilders.showDistractionSheet(
      context: context,
      onEventSelected: (eventName) {
        provider.logDistraction(eventName);
      },
    );
  }

  // --- TAG MANAGEMENT ---
  void _showTagSelector(BuildContext context, FocusProvider provider) {
    FocusBottomSheetBuilders.showTagSelector(
      context: context,
      tags: provider.tags,
      selectedTagId: provider.selectedTag?.id,
      onTagSelected: (tag) {
        provider.setSelectedTag(tag);
      },
      onCreateNewTag: () {
        _showCreateTagDialog(context, provider);
      },
    );
  }

  void _showCreateTagDialog(BuildContext context, FocusProvider provider) {
    const Color selectedColor = AppTheme.taskColor;

    FocusBottomSheetBuilders.showCreateTagDialog(
      context: context,
      onTagCreated: (tagName) {
        provider.createTag(tagName, selectedColor);
      },
    );
  }

  Future<void> _confirmStopSession(
    BuildContext context,
    FocusProvider focusProvider,
  ) async {
    final bool wasRunning = focusProvider.isRunning;
    if (!wasRunning && !focusProvider.awaitingPhaseContinue) return;

    var pausedByThisCall = false;
    if (wasRunning) {
      focusProvider.pauseSession();
      pausedByThisCall = true;
    }

    final bool confirmed =
        await AppDialogs.showConfirmation(
          context: context,
          title: 'Stop focus session?',
          content:
              'The timer is paused while you decide. Confirm to stop and save the session, or cancel to continue.',
        ) ==
        true;

    if (!context.mounted) return;

    if (confirmed) {
      focusProvider.stopSession(userProvider: context.read<UserProvider>());
    } else if (pausedByThisCall) {
      // only resume if this call paused the timer
      focusProvider.startSession();
    }
  }

  Future<void> _confirmResetSession(
    BuildContext context,
    FocusProvider focusProvider,
  ) async {
    final bool wasRunning = focusProvider.isRunning;
    if (!wasRunning && !focusProvider.awaitingPhaseContinue) return;

    var pausedByThisCall = false;
    if (wasRunning) {
      focusProvider.pauseSession();
      pausedByThisCall = true;
    }

    final bool confirmed =
        await AppDialogs.showConfirmation(
          context: context,
          title: 'Reset focus session?',
          content:
              'The timer is paused while you decide. Confirm to reset the session, or cancel to continue.',
        ) ==
        true;

    if (!context.mounted) return;

    if (confirmed) {
      focusProvider.resetSession();
    } else if (pausedByThisCall) {
      focusProvider.startSession();
    }
  }

  @override
  Widget build(BuildContext context) {
    final focusProvider = context.watch<FocusProvider>();
    focusProvider.attachUserProvider(context.read<UserProvider>());
    final isRunning = focusProvider.isRunning;
    final isPaused = focusProvider.isPaused;
    final activeMode = focusProvider.activeMode;
    final modes = focusProvider.modes;

    final bool isFlexibleMode = activeMode?.type == FocusModeType.flexible;
    final bool canDrag = isFlexibleMode && !isRunning && !isPaused;

    double gaugeProgress = 1.0;
    bool isStopwatchMode = false;
    String label = "FOCUS";
    String centerText = "25:00";
    bool isResting = false;

    if (activeMode != null && activeMode.phases.isNotEmpty) {
      final currentPhase = activeMode.phases[focusProvider.currentPhaseIndex];

      if (canDrag) {
        final int currentMinutes = focusProvider.flexibleDurationMinutes;
        gaugeProgress = currentMinutes / 120.0;
        label = "SET TIME";
        centerText = AppDateFormatUtils.formatDuration(currentMinutes * 60);
      } else if (currentPhase.durationMinutes > 0 ||
          currentPhase.durationMinutes == -1) {
        int totalPhaseSeconds = currentPhase.durationMinutes > 0
            ? currentPhase.durationMinutes * 60
            : 25 * 60;
        if (totalPhaseSeconds == 0) totalPhaseSeconds = 1;

        gaugeProgress =
            focusProvider.secondsRemainingInPhase / totalPhaseSeconds;
        label = currentPhase.type == PhaseType.rest ? "REST" : "FOCUS";
        isResting = currentPhase.type == PhaseType.rest;
        centerText = AppDateFormatUtils.formatDuration(
          focusProvider.secondsRemainingInPhase,
        );
      } else {
        isStopwatchMode = isRunning;
        gaugeProgress = 0.0;
        label = "STOPWATCH";
        centerText = AppDateFormatUtils.formatDuration(
          focusProvider.currentSecondsFocussed,
        );
      }
    }

    void cycleMode(int direction) {
      if (isRunning || isPaused || activeMode == null) return;
      final int currentIndex = modes.indexWhere((m) => m.id == activeMode.id);
      int nextIndex = (currentIndex + direction) % modes.length;
      if (nextIndex < 0) nextIndex = modes.length - 1;
      focusProvider.setActiveMode(modes[nextIndex]);
    }

    return Scaffold(
      appBar: AppBar(
        title: const Text('Focus'),
        actions: [
          IconButton(
            icon: const Icon(Icons.bar_chart),
            onPressed: () {
              Navigator.push(
                context,
                MaterialPageRoute(
                  builder: (context) =>
                      const StatisticsScreen(initialTabIndex: 2),
                ),
              );
            },
          ),
          IconButton(
            icon: const Icon(Icons.calendar_today),
            tooltip: 'Calendar View',
            onPressed: () {
              Navigator.push(
                context,
                MaterialPageRoute(builder: (context) => const CalendarScreen()),
              );
            },
          ),
          const SizedBox(width: 8),
        ],
      ),
      body: Column(
        children: [
          const SizedBox(height: 10),

          // --- MODE SELECTOR ---
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              IconButton(
                icon: const Icon(
                  Icons.arrow_back_ios,
                  size: 20,
                  color: Colors.grey,
                ),
                onPressed: (isRunning || isPaused) ? null : () => cycleMode(-1),
              ),
              SizedBox(
                width: 180,
                child: InkWell(
                  onTap: (isRunning || isPaused)
                      ? null
                      : () {
                          Navigator.push(
                            context,
                            MaterialPageRoute(
                              builder: (context) => const FocusModesScreen(),
                            ),
                          );
                        },
                  borderRadius: AppTheme.brMedium,
                  child: Padding(
                    padding: const EdgeInsets.symmetric(vertical: 8.0),
                    child: Text(
                      activeMode?.name.toUpperCase() ?? 'SELECT MODE',
                      textAlign: TextAlign.center,
                      style: TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.bold,
                        letterSpacing: 1.2,
                        color: (isRunning || isPaused)
                            ? Theme.of(context).colorScheme.onSurfaceVariant
                            : AppTheme.focusColor,
                      ),
                    ),
                  ),
                ),
              ),
              IconButton(
                icon: const Icon(
                  Icons.arrow_forward_ios,
                  size: 20,
                  color: Colors.grey,
                ),
                onPressed: (isRunning || isPaused) ? null : () => cycleMode(1),
              ),
            ],
          ),

          const Spacer(),

          // --- DAILY GOAL BADGE ---
          Align(
            alignment: Alignment.topCenter,
            child: Padding(
              padding: const EdgeInsets.only(bottom: 12.0),
              child: GestureDetector(
                onTap: () {
                  Navigator.push(
                    context,
                    MaterialPageRoute(
                      builder: (context) => const SettingsScreen(),
                    ),
                  );
                },
                child: Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 16,
                    vertical: 8,
                  ),
                  decoration: BoxDecoration(
                    color: AppTheme.focusColor.withValues(alpha: 0.12),
                    borderRadius: BorderRadius.circular(20),
                    border: Border.all(
                      color: AppTheme.focusColor.withValues(alpha: 0.3),
                    ),
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      const Icon(
                        Icons.track_changes,
                        size: 18,
                        color: AppTheme.focusColor,
                      ),
                      const SizedBox(width: 8),
                      Text(
                        '${focusProvider.getMinutesFocusedToday()} / ${context.watch<SettingsProvider>().dailyGoalMins} m',
                        style: const TextStyle(
                          fontWeight: FontWeight.w900,
                          fontSize: 15,
                          color: AppTheme.focusColor,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ),

          // --- GAUGE & SIDE BUTTONS ---
          SizedBox(
            height: 320,
            width: double.infinity,
            child: Stack(
              alignment: Alignment.center,
              children: [
                InteractiveGauge(
                  key: ValueKey(activeMode?.id),
                  progress: gaugeProgress,
                  isStopwatch: isStopwatchMode,
                  color: isResting
                      ? AppTheme.warningColor
                      : AppTheme.focusColor,
                  labelColor: isResting
                      ? AppTheme.warningColor
                      : AppTheme.focusColor,
                  centerTextColor: AppTheme.focusColor,
                  centerText: centerText,
                  bottomText: focusProvider.selectedTag?.name ?? "No Tag",
                  bottomTextColor: focusProvider.selectedTag != null
                      ? Color(focusProvider.selectedTag!.colorValue)
                      : AppTheme.focusColor,
                  onBottomTextTapped: (isRunning || isPaused)
                      ? null
                      : () => _showTagSelector(context, focusProvider),
                  isInteractive: canDrag,
                  label: label,
                  onChanged: (newProgress) {
                    if (canDrag) {
                      int newMins = (newProgress * 120).round();
                      if (newMins < 1) newMins = 1;
                      focusProvider.setFlexibleDuration(newMins);
                    }
                  },
                ),

                // TIME MACHINE BUTTON
                Positioned(
                  top: 16,
                  left: 16,
                  child: IconButton.filledTonal(
                    icon: const Icon(Icons.history),
                    tooltip: "Log Past Session",
                    iconSize: 28,
                    padding: const EdgeInsets.all(12),
                    onPressed: (isRunning || isPaused)
                        ? null
                        : () => AppDialogs.showTimeMachineDialog(
                            context,
                            focusProvider,
                          ),
                  ),
                ),

                // DISTRACTIONS BUTTON
                Positioned(
                  top: 16,
                  right: 16,
                  child: IconButton.filledTonal(
                    icon: const Icon(Icons.warning_amber),
                    tooltip: "Log Distraction/Event",
                    iconSize: 28,
                    padding: const EdgeInsets.all(12),
                    onPressed: (isRunning || isPaused)
                        ? () => _showDistractionSheet(context, focusProvider)
                        : null,
                  ),
                ),
              ],
            ),
          ),

          ModeTimeline(
            phases: activeMode?.phases ?? [],
            currentPhaseIndex: focusProvider.currentPhaseIndex,
            isRunning: isRunning || isPaused,
            awaitingContinue: focusProvider.awaitingPhaseContinue,
          ),

          const Spacer(),

          // --- BOTTOM CONTROLS ---
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Opacity(
                opacity:
                    (isRunning ||
                        isPaused ||
                        focusProvider.awaitingPhaseContinue)
                    ? 1.0
                    : 0.0,
                child: IconButton(
                  icon: const Icon(Icons.refresh),
                  iconSize: 32,
                  color: Colors.grey.shade600,
                  onPressed:
                      (isRunning ||
                          isPaused ||
                          focusProvider.awaitingPhaseContinue)
                      ? () => _confirmResetSession(context, focusProvider)
                      : null,
                ),
              ),
              const SizedBox(width: 32),
              SizedBox(
                height: 80,
                width: 80,
                child: FloatingActionButton(
                  heroTag: "focus_main_button",
                  shape: const CircleBorder(),
                  elevation: 4,
                  backgroundColor: isRunning
                      ? AppTheme.errorColor
                      : AppTheme.focusColor,
                  foregroundColor: Colors.white,
                  onPressed: () {
                    if (isRunning) {
                      _confirmStopSession(context, focusProvider);
                    } else if (focusProvider.awaitingPhaseContinue) {
                      focusProvider.continueToNextPhase();
                    } else {
                      focusProvider.startSession();
                    }
                  },
                  child: Icon(
                    isRunning
                        ? Icons.stop_rounded
                        : (focusProvider.awaitingPhaseContinue
                              ? Icons.fast_forward_rounded
                              : Icons.play_arrow_rounded),
                    size: 40,
                  ),
                ),
              ),
              const SizedBox(width: 32),
              Opacity(
                opacity:
                    (isRunning ||
                        isPaused ||
                        focusProvider.awaitingPhaseContinue)
                    ? 1.0
                    : 0.0,
                child: IconButton(
                  icon: focusProvider.awaitingPhaseContinue
                      ? const Icon(Icons.stop)
                      : Icon(isPaused ? Icons.play_circle_fill : Icons.pause),
                  iconSize: 32,
                  color: Colors.grey.shade600,
                  onPressed:
                      (isRunning ||
                          isPaused ||
                          focusProvider.awaitingPhaseContinue)
                      ? () {
                          if (focusProvider.awaitingPhaseContinue) {
                            _confirmStopSession(context, focusProvider);
                          } else if (isPaused) {
                            focusProvider.startSession();
                          } else {
                            focusProvider.pauseSession();
                          }
                        }
                      : null,
                ),
              ),
            ],
          ),

          const SizedBox(height: 40),
        ],
      ),
    );
  }
}
