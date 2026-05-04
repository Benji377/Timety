import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/focus_provider.dart';
import '../data/focus_models.dart';
import '../widgets/interactive_gauge.dart';

class FocusScreen extends StatelessWidget {
  const FocusScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final focusProvider = context.watch<FocusProvider>();
    final isRunning = focusProvider.isRunning;
    final isPaused = focusProvider.isPaused;
    final activeMode = focusProvider.activeMode;
    
    // Determine state for the Gauge
    bool isFlexibleMode = activeMode?.type == FocusModeType.flexible;
    bool canDrag = isFlexibleMode && !isRunning && !isPaused;
    
    int displayMinutes = 0;
    int maxMinutes = 120; // Max time for flexible dragging
    String label = "FOCUS";

    if (activeMode != null && activeMode.phases.isNotEmpty) {
      final currentPhase = activeMode.phases[focusProvider.currentPhaseIndex];
      
      if (canDrag) {
        // User is setting the time
        displayMinutes = currentPhase.durationMinutes == -1 ? 25 : currentPhase.durationMinutes;
        label = "SET TIME";
      } else if (currentPhase.durationMinutes > 0) {
        // Countdown is running
        displayMinutes = (focusProvider.secondsRemainingInPhase / 60).ceil();
        maxMinutes = currentPhase.durationMinutes;
        label = currentPhase.type == PhaseType.rest ? "REST" : "FOCUS";
      } else {
        // Stopwatch
        displayMinutes = (focusProvider.currentSecondsFocussed / 60).floor();
        maxMinutes = 0; // Tricks the gauge into drawing a full circle
        label = "STOPWATCH";
      }
    }

    return Scaffold(
      appBar: AppBar(
        title: const Text('Focus'),
        actions: [
          Padding(
            padding: const EdgeInsets.only(right: 16.0),
            child: Center(
              child: Text(
                '${focusProvider.getMinutesFocusedToday()} / ${focusProvider.dailyTargetMinutes} min',
                style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
              ),
            ),
          ),
        ],
      ),
      body: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          TextButton.icon(
            onPressed: (isRunning || isPaused) ? null : () {},
            icon: const Icon(Icons.tune),
            label: Text(activeMode?.name ?? 'Select Mode', style: const TextStyle(fontSize: 18)),
          ),
          const SizedBox(height: 40),

          // --- THE NEW INTERACTIVE GAUGE ---
          InteractiveGauge(
            maxMinutes: maxMinutes,
            initialMinutes: displayMinutes,
            isInteractive: canDrag,
            label: label,
            onChanged: (newMinutes) {
              // Update the provider live as the user drags
              if (canDrag && newMinutes > 0) {
                focusProvider.setFlexibleDuration(newMinutes);
              }
            },
          ),
          // ---------------------------------
          
          const SizedBox(height: 60),

          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              if (isRunning || isPaused)
                IconButton(
                  icon: const Icon(Icons.refresh),
                  iconSize: 32,
                  onPressed: () => focusProvider.resetSession(),
                )
              else
                IconButton(
                  icon: const Icon(Icons.history),
                  iconSize: 32,
                  onPressed: () {}, 
                ),
              
              const SizedBox(width: 24),

              FloatingActionButton.large(
                onPressed: () {
                  if (isRunning) {
                    focusProvider.stopSession();
                  } else {
                    focusProvider.startSession();
                  }
                },
                child: Icon(isRunning ? Icons.stop : Icons.play_arrow),
              ),

              const SizedBox(width: 24),

              if (isRunning || isPaused)
                IconButton(
                  icon: Icon(isPaused ? Icons.play_circle_outline : Icons.pause),
                  iconSize: 32,
                  onPressed: () {
                    if (isPaused) {
                      focusProvider.startSession();
                    } else {
                      focusProvider.pauseSession();
                    }
                  },
                )
              else
                IconButton(
                  icon: const Icon(Icons.warning_amber),
                  iconSize: 32,
                  onPressed: () {},
                ),
            ],
          ),
        ],
      ),
    );
  }
}