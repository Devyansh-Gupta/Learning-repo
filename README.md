# Habit Tracker

A CLI-based habit tracking application with SQLite persistence.

## Features

- **User Authentication**: Register, login, logout with hashed passwords
- **Habit Management**: Create daily and weekly habits with categories
- **Streak Tracking**: Track current and best streaks
- **Reminders**: Set reminder alerts for habits
- **Session Persistence**: Remember Me functionality using session.dat
- **Password Reset**: Reset password via security question

## How to Run

### Prerequisites
- JDK 11 or higher
- SQLite JDBC driver

### Quick Start

1. **Download dependencies**:
   - sqlite-jdbc-3.45.1.0.jar
   - slf4j-api.jar
   - slf4j-simple.jar

2. **Compile**:
   ```bash
   javac -cp "sqlite-jdbc-3.45.1.0.jar" -d out src/habittracker/**/*.java
   ```

3. **Run**:
   ```bash
   java -cp "out;sqlite-jdbc-3.45.1.0.jar;slf4j-api.jar;slf4j-simple.jar" habittracker.main.HabitTrackerApp
   ```

## Project Structure

```
src/habittracker/
├── main/
│   └── HabitTrackerApp.java       # CLI entry point
├── model/
│   ├── Habit.java                 # Abstract base class
│   ├── DailyHabit.java           # Daily habit implementation
│   ├── WeeklyHabit.java          # Weekly habit implementation
│   ├── User.java                 # User data model
│   ├── Streak.java               # Streak tracking
│   └── Category.java             # Category enum
├── interfaces/
│   ├── Trackable.java            # Habit completion interface
│   └── Categorizable.java        # Category interface
├── manager/
│   └── HabitManager.java         # Generic habit manager
├── auth/
│   ├── AuthService.java          # Authentication logic
│   ├── SessionManager.java       # Session file I/O
│   └── PasswordUtils.java        # SHA-256 hashing
├── db/
│   ├── DatabaseManager.java      # SQLite connection manager
│   └── HabitRepository.java      # All CRUD operations
├── threads/
│   ├── ReminderThread.java       # Background reminder checker
│   └── AutoSaveThread.java       # Background auto-saver
└── exceptions/
    ├── AuthException.java
    ├── InvalidHabitException.java
    ├── HabitNotFoundException.java
    └── DatabaseException.java
```

## Database Schema

The application uses SQLite with the following tables:

### users
| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key |
| username | TEXT | Unique username |
| password_hash | TEXT | SHA-256 hash |
| security_question | TEXT | Password reset question |
| security_answer_hash | TEXT | SHA-256 hash of answer |
| created_at | TEXT | ISO timestamp |

### habits
| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key |
| user_id | INTEGER | FK to users |
| name | TEXT | Habit name |
| category | TEXT | HEALTH, STUDY, etc. |
| type | TEXT | DAILY or WEEKLY |
| reminder_time | TEXT | HH:MM format |
| target_days | TEXT | Comma-separated for weekly |
| created_at | TEXT | ISO timestamp |

### habit_logs
| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key |
| habit_id | INTEGER | FK to habits |
| completed_date | TEXT | YYYY-MM-DD |

### streaks
| Column | Type | Description |
|--------|------|-------------|
| habit_id | INTEGER | FK to habits |
| current_streak | INTEGER | Current count |
| best_streak | INTEGER | All-time best |
| last_completed | TEXT | YYYY-MM-DD |

## Advanced Java Features Used

### 1. Exception Handling
- Custom exceptions: `AuthException`, `InvalidHabitException`, `HabitNotFoundException`, `DatabaseException`
- Try-catch-finally around all DB operations
- Graceful error handling in CLI

### 2. Generics
- `HabitManager<T extends Habit>` - Generic manager class
- `filterByCategory(List<T> habitList, Category category)` - Type-safe filtering

### 3. Multithreading
- `ReminderThread implements Runnable` - Checks reminders every 60 seconds
- `AutoSaveThread extends Thread` - Daemon thread, saves every 5 minutes
- `volatile` flags for clean thread shutdown

### 4. I/O Operations
- `SessionManager` - File I/O for session.dat using FileOutputStream/BufferedReader
- Remember Me functionality with 7-day expiry

## CLI Menu

```
=== HABIT TRACKER ===
Logged in as: username

[1] View All Habits
[2] Add Habit
[3] Mark Habit Complete (Today)
[4] View Streaks
[5] Filter by Category
[6] Delete Habit
[7] Manage Reminders
[8] Logout
[0] Exit
```

## Testing Checklist

- [x] Register new user → success
- [x] Register duplicate username → error
- [x] Login with wrong password → error
- [x] Login with correct credentials → success
- [x] Logout → session.dat cleared
- [x] Re-launch app → Remember Me works
- [x] Password reset → success
- [x] Add DailyHabit → appears in list
- [x] Add WeeklyHabit → appears in list
- [x] Mark habit complete → streak updates
- [x] Mark WeeklyHabit on wrong day → error
- [x] Delete habit → removed
- [x] Filter by category → correct subset
- [x] Set reminder → alert appears
