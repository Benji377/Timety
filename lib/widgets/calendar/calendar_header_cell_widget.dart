import 'package:flutter/material.dart';

class CalendarHeaderCell extends StatelessWidget {
  final String text;
  final Color? color;

  const CalendarHeaderCell(this.text, {super.key, this.color});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8.0),
      child: Center(
        child: Text(
          text,
          style: TextStyle(
            fontWeight: FontWeight.bold,
            color: color ?? Colors.grey,
          ),
        ),
      ),
    );
  }
}
