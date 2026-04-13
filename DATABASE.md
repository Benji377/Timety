# Timety Database Architecture

This document outlines the local SQLite database schema for Timety. Since the app is built natively for Android, we use **Room** (Google's official ORM) to manage the database. All timestamps should be stored as Unix epoch milliseconds (`Long` in Kotlin).

## Entities (Tables)

### 1. `User` (Singleton Table)
Stores the user's gamification progress and global settings. Usually restricted to a single row (ID = 1).
| Column Name | Kotlin Type | Description |
| :--- | :--- | :--- |
| `id` | `Int` | Primary Key (Always 1) |
| `name` | `String` | User's display name |
| `xp` | `Int` | Total experience points |
| `level` | `Int` | Current user level |
| `currentStreak` | `Int` | Number of consecutive days with a completed task/focus |
| `highestStreak` | `Int` | Longest streak achieved |
| `dailyFocusTarget` | `Long` | Target focus time in milliseconds |
| `lastActiveDate` | `Long` | Used to calculate if the streak should reset |

### 2. `Task`
Stores the to-do items.
| Column Name | Kotlin Type | Description |
| :--- | :--- | :--- |
| `id` | `Int` | Primary Key (Auto-generate) |
| `title` | `String` | The name of the task |
| `description` | `String?` | Optional details |
| `iconName` | `String` | Identifier for the Compose material icon |
| `location` | `String?` | Optional physical location |
| `dueDate` | `Long?` | Deadline timestamp |
| `reminderTime` | `Long?` | Timestamp for local notification alarm |
| `durationEst` | `Long?` | Estimated time to complete (ms) |
| `status` | `String` | Enum: `TODO`, `DONE`, `OVERDUE` |

### 3. `Category`
Custom categories for tasks and focus sessions (e.g., "Math", "Coding", "Reading").
| Column Name | Kotlin Type | Description |
| :--- | :--- | :--- |
| `id` | `Int` | Primary Key (Auto-generate) |
| `name` | `String` | Category label |
| `colorHex` | `String` | Hex code for UI representation |
| `iconName` | `String` | Identifier for the Compose material icon |

### 4. `FocusSession`
Logs every completed Pomodoro or Stopwatch session for statistics and insights.
| Column Name | Kotlin Type | Description |
| :--- | :--- | :--- |
| `id` | `Int` | Primary Key (Auto-generate) |
| `categoryId` | `Int` | Foreign Key -> `Category` |
| `taskId` | `Int?` | Foreign Key -> `Task` (Optional, for linked sessions) |
| `startTime` | `Long` | Timestamp when session started |
| `endTime` | `Long` | Timestamp when session ended |
| `duration` | `Long` | Total focused time in milliseconds |
| `rating` | `String?` | Enum: `GREAT`, `OKAY`, `DISTRACTED` (Micro-journaling) |
| `note` | `String?` | Optional 1-sentence review |