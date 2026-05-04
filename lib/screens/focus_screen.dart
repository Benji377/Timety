import 'package:flutter/material.dart';

class FocusScreen extends StatelessWidget {
  const FocusScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Focus')),
      body: const Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.coffee, size: 64, color: Colors.grey),
            SizedBox(height: 16),
            Text('Pomodoro & Stopwatch coming soon!'),
          ],
        ),
      ),
    );
  }
}