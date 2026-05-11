# Timety

<p align="center">
  <img src="assets/banner_small.png" alt="Timety banner">
  <p align="center">
    <a href="https://github.com/Benji377/Timety/actions/workflows/lint.yml">
      <img src="https://img.shields.io/github/actions/workflow/status/Benji377/Timety/lint.yml?label=Lint&logo=flutter&style=for-the-badge&labelColor=555555" alt="lint">
    </a>
    <a href="https://github.com/Benji377/Timety/actions/workflows/test.yml">
      <img src="https://img.shields.io/github/actions/workflow/status/Benji377/Timety/test.yml?label=Tests&logo=githubactions&style=for-the-badge&labelColor=555555" alt="test">
    </a>
  </p>
</p>

Timety is a comprehensive, offline-first productivity and time-management application built with Flutter. It seamlessly integrates Task Management, Focus Sessions, and Habit Tracking into one beautiful, gamified experience.

<p align="center">
  <img src="screenshots/output/01_home_screen.png" width="32%" alt="Home Screen">
  <img src="screenshots/output/02_focus_screen.png" width="32%" alt="Focus Screen">
  <img src="screenshots/output/03_tasks_screen.png" width="32%" alt="Habits Screen">
</p>
<p align="center">
  <em>View the <a href="screenshots/output/">full screenshot gallery</a> to see the calendar, analytics, profile, and more!</em>
</p>

## ✨ Features

*   **Intelligent Task Management:** Organize one-off tasks with due dates, priority levels, and effort sizes.
*   **Focus Engine:** A robust stopwatch and timer system tailored for deep work. Includes support for tags, custom modes, and short/long break phases.
*   **Habit Tracker:** Build lasting routines with flexible frequencies (daily, specific weekdays, or flexible weekly goals).
*   **Rich Analytics:** Visualize your productivity with interactive charts, weekly velocity metrics, and time-of-day habit analysis.
*   **Gamification:** Track your active days, build streaks, and level up your productivity profile.
*   **Smart Notifications:** Local reminders for tasks, specific habit times, and dynamic daily motivation.
*   **Unified Calendar:** A custom micro-dot calendar view to see your tasks, focus sessions, and habits at a glance.

## 📥 Download & Installation

Choose your preferred way to stay productive.

<p align="left">
  <img src="https://img.shields.io/badge/Get_it_on-555555?style=for-the-badge" alt="Get it on">
  <a href="https://apkpure.com/p/io.github.benji377.timety">
    <img src="https://img.shields.io/badge/APKPure-24D668?style=for-the-badge&logo=android&logoColor=white" alt="APKPure">
  </a>
  <a href="#">
    <img src="https://img.shields.io/badge/Uptodown-0096C7?style=for-the-badge&logo=spreadshirt&logoColor=white" alt="Uptodown">
  </a>
  <a href="#-f-droid-coming-soon">
    <img src="https://img.shields.io/badge/F--Droid-ffffff?style=for-the-badge&logo=f-droid&logoColor=black" alt="F-Droid">
  </a>
</p>

> [!NOTE]
> **APKPure** and **Uptodown** versions are uploaded manually and may take a few days to sync with the latest GitHub releases.

### 📦 Manual Installation (GitHub Releases)
For the most up-to-date version, download the APK directly for your device architecture:

* **[arm64-v8a](https://github.com/Benji377/Timety/releases/download/v1.2.0/timety-v1.2.0-arm64-v8a.apk)** – Most modern Android phones (64-bit). **(Recommended)**
* **[armeabi-v7a](https://github.com/Benji377/Timety/releases/download/v1.2.0/timety-v1.2.0-armeabi-v7a.apk)** – Older Android devices (32-bit).
* **[x86_64](https://github.com/Benji377/Timety/releases/download/v1.2.0/timety-v1.2.0-x86_64.apk)** – Android emulators and some tablets.
* **[Universal Bundle (.aab)](https://github.com/Benji377/Timety/releases/download/v1.2.0/timety-v1.2.0.aab)** – Best for manual side-loading via App Bundle installers.

**How to install:**
1. Download the `.apk` file corresponding to your device.
2. Open the file on your Android device.
3. If prompted, enable "Install from Unknown Sources" in your settings.
4. Follow the on-screen instructions to finish.

## 🛠️ Tech Stack

*   **Framework:** [Flutter](https://flutter.dev/) (Dart)
*   **Local Storage:** [Hive](https://pub.dev/packages/hive) (Fast, NoSQL local database)
*   **State Management:** [Provider](https://pub.dev/packages/provider)
*   **Charts:** [fl_chart](https://pub.dev/packages/fl_chart)
*   **Notifications:** [flutter_local_notifications](https://pub.dev/packages/flutter_local_notifications)

## 🚀 Getting Started

### Prerequisites
*   Flutter SDK (Stable channel)
*   Dart SDK

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/Benji377/Timety.git
   ```
2. Navigate to the project directory:
   ```bash
   cd Timety
   ```
3. Install dependencies:
   ```bash
   flutter pub get
   ```
4. **Crucial Step:** Generate the Hive database adapters:
   ```bash
   dart run build_runner build --delete-conflicting-outputs
   ```
5. Run the app:
   ```bash
   flutter run
   ```

## 🗺️ Development & Roadmap

Want to see what's coming next or contribute to Timety?

Check out the **[Timety GitHub Project Board](https://github.com/users/Benji377/projects/7)**! There, you can track the live status of all planned features, bugs, and enhancements, complete with priority tags and issue sizing.

## 🔒 Privacy
Timety is proudly **Offline-First**. All of your tasks, habits, and focus history are stored securely on your local device using Hive. No accounts, no cloud sync, no tracking.
