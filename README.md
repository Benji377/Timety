# Timety ⏳🎮

Timety is an open-source, privacy-first Android application that combines powerful time management with the dopamine-driven gamification of an RPG. Track your to-dos, crush your Pomodoro sessions, and level up your productivity—all while keeping your data strictly on your device.

## 🚀 Features

### Task Management & Calendar
* **Detailed To-Dos:** Manage tasks with icons, descriptions, due dates, and reminders.
* **Visual States:** Instantly know a task's status via color-coded borders (Blue = To-do, Green = Done, Red = Overdue).
* **Expandable Calendar:** Seamlessly view and schedule tasks by day or month.

### The Focus Engine
* **Dual Modes:** Choose between strict Pomodoro sessions or an open-ended Stopwatch.
* **Task-Timer Linking:** Attach a focus session directly to a specific to-do item to track exactly how long it took.
* **Ambient Sounds:** Built-in offline audio (White noise, rain, cafe) to help you zone in.
* **Micro-Journaling:** Quickly rate your focus sessions (Great, Okay, Distracted) when the timer rings.

### Gamification & Insights
* **Level Up:** Earn XP by completing tasks and finishing focus blocks.
* **Streaks:** Maintain a daily streak to unlock special titles.
* **Smart Statistics:** View line graphs of your weekly focus, accompanied by automatic text-based insights (e.g., "You study best on Tuesday mornings!").

### Privacy First
* **100% Offline:** Timety requires no account and no server. All data is saved directly to your phone using an encrypted SQLite database.
* **Optional Cloud Sync:** Manually back up your encrypted data to your personal Google Drive.

## 🛠️ Tech Stack

Timety is built as a modern, native Android application:
* **Language:** Kotlin
* **UI Toolkit:** Jetpack Compose (Declarative UI)
* **Local Database:** Room (SQLite)
* **Background Processing:** Android Foreground Services & AlarmManager (ensuring timers never die in the background)
* **Architecture:** MVVM (Model-View-ViewModel) with Kotlin Coroutines & StateFlow

## 🔮 Phase 2 (Future Roadmap)
While Timety is currently a local-first companion app, future updates (Timety+) may introduce opt-in server features including:
* Global XP Leaderboards
* "Focus Parties" (Location-based XP boosts at libraries/cafes)
* Co-op studying with friends

## 🤝 Contributing
Timety is fully open-source! We welcome pull requests, bug reports, and feature suggestions. (Contribution guidelines coming soon).