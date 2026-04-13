# Timety UI Layout & Navigation

This document outlines the visual structure of the app, built using **Jetpack Compose**. The app utilizes a primary `Scaffold` with a bottom navigation bar (`NavigationBar`) to switch between the main functional areas.

## Main Navigation Flow
* **Bottom Navigation Bar:** Contains tabs for `Home`, `Focus`, `Tasks`, and `Stats`.
* **Top App Bar (Home Only):** Contains a gear icon to navigate to `Settings`.

## Screens

### 1. HomeScreen (`/home`)
The dashboard and greeting page.
* **Top Bar:** Greeting text ("Good morning, [Name]") + Gear Icon (navigates to SettingsScreen).
* **Radial Graph Component:** A circular progress indicator showing `Focus time today` vs. `dailyFocusTarget`.
* **Mini-Timer Component:** A quick-start button that routes to the FocusScreen.
* **Today's Tasks List:** A `LazyColumn` showing a simplified view of tasks due today.

### 2. FocusScreen (`/focus`)
The core timer engine.
* **Header:** Toggle between `Pomodoro` and `Stopwatch` modes.
* **Context Selectors:** * Category Dropdown.
    * Link-to-Task Selector (Optional).
* **TimerRing Component:** The main interactive UI element displaying remaining/elapsed time.
* **Controls:** Start, Pause, Stop buttons.
* **Ambient Sound Row:** Toggle buttons for White Noise, Rain, Cafe.
* **Review Modal (Popup):** Appears when a session ends, asking for a rating (Great/Okay/Distracted) and an optional note.

### 3. TasksScreen (`/tasks`)
The to-do management hub.
* **Calendar Component:** An expandable calendar view (collapses to a weekly row, expands to full month).
* **Task List:** A `LazyColumn` of `TaskCard` components.
* **Filter/Sort Row:** Chips to sort by Date, Name, or Reminder.
* **FAB (Floating Action Button):** Opens the "Add Task" modal/bottom sheet.

### 4. TaskDetailScreen (`/task/{id}`)
Reached by tapping a `TaskCard` on the Home or Tasks screens.
* **Editable Fields:** Title, description, due date, reminders.
* **Session History:** A list of past `FocusSessions` linked to this specific task.

### 5. StatsScreen (`/stats`)
Data visualization and gamification profile.
* **Profile Header:** User name, Title, `XPBar` Component, and current Streak flame icon.
* **Data Insights Component:** Text-based cards (e.g., "You are 40% more productive on Tuesdays!").
* **Charts:** Line graphs showing focus hours per day/week (filterable by Category).

### 6. SettingsScreen (`/settings`)
Reached via the Home screen gear icon.
* **Gamification:** Edit user name, reset stats.
* **Categories:** Add/remove/edit custom categories and colors.
* **Data Management:** "Sync to Google Drive" button, manual JSON Export/Import.
* **About:** Open-source licenses, version info.

## Reusable Components (`@Composable`)
* `TaskCard`: Takes a `Task` object. Uses a dynamic border modifier (`Modifier.border()`) based on status: Blue (Todo), Green (Done), Red (Overdue).
* `TimerRing`: A custom canvas/SVG drawing that animates time progress.
* `XPBar`: A horizontal progress bar filling based on `(currentXP / xpForNextLevel)`.