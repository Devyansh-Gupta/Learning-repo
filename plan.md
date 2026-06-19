# Habit Tracker — Project Plan

**Course Outcomes:** CO4, CO5 | **Bloom Levels:** L4 (Analyzing), L5 (Evaluating)
**Interface:** CLI | **Storage:** SQLite | **Future:** Android-ready

---

## 1. Problem Definition & Objective

### Problem Statement
People struggle to build and maintain consistent habits due to a lack of structured tracking, accountability, and progress visibility. Existing tools are either too complex or don't persist data meaningfully between sessions.

### Objective
Build a multi-user, CLI-based Habit Tracker in Java that allows users to:
- Register/login securely with hashed credentials
- Create, categorize, and track daily/weekly habits
- Monitor streaks and view progress
- Receive background reminder alerts
- Persist all data across sessions using SQLite

### Context
This project demonstrates mastery of core OOP principles alongside four advanced Java features: Exception Handling, Generics, Multithreading, and I/O Operations — structured in a way that can be migrated to an Android app in a future phase.

---

## 2. Scope

### In Scope
- User registration, login, logout, and password reset (via security question)
- Remember Me session persistence using a local `session.dat` file
- Create, view, delete, and mark habits as complete
- Daily and weekly habit types
- Habit categories (Health, Study, Fitness, Finance, Other)
- Streak tracking (current streak + best streak)
- Background reminder alerts via a dedicated thread
- Auto-save thread that periodically syncs in-memory state to SQLite
- All data stored in a local SQLite database (`habittracker.db`)
- Custom exceptions for auth, habit, and database errors

### Out of Scope (CLI Phase)
- GUI or web interface
- Push notifications
- Cloud sync or remote database
- Social / sharing features

---

## 3. Advanced Java Features — Usage Plan

| Feature | Where & How Used |
|---|---|
| **Exception Handling** | Custom exceptions: `AuthException`, `InvalidHabitException`, `HabitNotFoundException`, `DatabaseException`. Try-catch-finally around all DB operations, file I/O, and user input parsing |
| **Generics** | `HabitManager<T extends Habit>` — a generic manager class. Generic utility method `filterByCategory(List<T> habits, String category)` for type-safe filtering |
| **Multithreading** | `ReminderThread implements Runnable` — background thread checking reminders every 60s. `AutoSaveThread extends Thread` — daemon thread flushing in-memory state to DB every 5 minutes |
| **I/O Operations** | `SessionManager` reads/writes `session.dat` using `FileOutputStream`/`BufferedReader` for Remember Me session token persistence |
| **OOP** | Abstract `Habit` class, `DailyHabit`/`WeeklyHabit` subclasses, `Trackable` and `Categorizable` interfaces, encapsulation across all model classes |

---

## 4. System Design

### 4.1 OOP Class Hierarchy

```
Habit (abstract)
├── DailyHabit       implements Trackable, Categorizable
└── WeeklyHabit      implements Trackable, Categorizable

Interfaces
├── Trackable        → markComplete(), getStreak()
└── Categorizable    → getCategory(), setCategory()

Managers / Services
├── HabitManager<T extends Habit>
├── AuthService
├── SessionManager
└── PasswordUtils

Database
├── DatabaseManager  (Singleton)
└── HabitRepository

Threads
├── ReminderThread   implements Runnable
└── AutoSaveThread   extends Thread

Exceptions
├── AuthException
├── InvalidHabitException
├── HabitNotFoundException
└── DatabaseException
```

### 4.2 Database Schema

**users**
```
id               INTEGER  PRIMARY KEY AUTOINCREMENT
username         TEXT     UNIQUE NOT NULL
password_hash    TEXT     NOT NULL
security_question TEXT    NOT NULL
security_answer_hash TEXT NOT NULL
created_at       TEXT     NOT NULL
```

**habits**
```
id               INTEGER  PRIMARY KEY AUTOINCREMENT
user_id          INTEGER  NOT NULL  REFERENCES users(id)
name             TEXT     NOT NULL
category         TEXT     NOT NULL
type             TEXT     NOT NULL   -- 'DAILY' | 'WEEKLY'
reminder_time    TEXT               -- HH:MM, nullable
created_at       TEXT     NOT NULL
```

**habit_logs**
```
id               INTEGER  PRIMARY KEY AUTOINCREMENT
habit_id         INTEGER  NOT NULL  REFERENCES habits(id)
completed_date   TEXT     NOT NULL  -- YYYY-MM-DD
```

**streaks**
```
habit_id         INTEGER  PRIMARY KEY REFERENCES habits(id)
current_streak   INTEGER  DEFAULT 0
best_streak      INTEGER  DEFAULT 0
last_completed   TEXT               -- YYYY-MM-DD, nullable
```

### 4.3 Session Flow

```
App Start
    │
    ▼
Read session.dat
    │
    ├── Valid token found ──► Auto-login ──► Main Menu
    │
    └── No / expired token
            │
            ▼
        Auth Menu
        ├── [1] Login
        ├── [2] Register
        └── [3] Forgot Password
                │
                ▼
            Main Menu
            ├── [1] View My Habits
            ├── [2] Add Habit
            ├── [3] Mark Habit Complete
            ├── [4] View Streaks
            ├── [5] Filter by Category
            ├── [6] Delete Habit
            ├── [7] Manage Reminders
            └── [8] Logout
```

---

## 5. Package Structure

```
src/
└── habittracker/
    ├── main/
    │   └── HabitTrackerApp.java         ← Entry point, CLI menu loop
    ├── model/
    │   ├── Habit.java                   ← Abstract base class
    │   ├── DailyHabit.java
    │   ├── WeeklyHabit.java
    │   ├── User.java
    │   ├── Streak.java
    │   └── Category.java                ← Enum: HEALTH, STUDY, FITNESS, FINANCE, OTHER
    ├── interfaces/
    │   ├── Trackable.java
    │   └── Categorizable.java
    ├── manager/
    │   └── HabitManager.java            ← Generic: HabitManager<T extends Habit>
    ├── auth/
    │   ├── AuthService.java
    │   ├── SessionManager.java          ← File I/O for session.dat
    │   └── PasswordUtils.java           ← SHA-256 hashing
    ├── db/
    │   ├── DatabaseManager.java         ← Singleton SQLite connection
    │   └── HabitRepository.java         ← All CRUD operations
    ├── threads/
    │   ├── ReminderThread.java          ← implements Runnable
    │   └── AutoSaveThread.java          ← extends Thread (daemon)
    └── exceptions/
        ├── AuthException.java
        ├── InvalidHabitException.java
        ├── HabitNotFoundException.java
        └── DatabaseException.java
```

---

## 6. Dependencies

| Dependency | Purpose | How to Add |
|---|---|---|
| `sqlite-jdbc` (Xerial) | SQLite driver for Java | Add JAR to classpath or Maven/Gradle |
| Java SE 11+ | Core language | JDK 11 or higher |

**Maven dependency (if using Maven):**
```xml
<dependency>
    <groupId>org.xerial</groupId>
    <artifactId>sqlite-jdbc</artifactId>
    <version>3.45.1.0</version>
</dependency>
```

---

## 7. Milestones & Build Order

| Phase | What to Build | Files Involved |
|---|---|---|
| **Phase 1** | Project setup, DB schema, DatabaseManager | `DatabaseManager.java` |
| **Phase 2** | Model classes + interfaces | `Habit`, `DailyHabit`, `WeeklyHabit`, `User`, `Streak`, `Category`, interfaces |
| **Phase 3** | Custom exceptions | All 4 exception classes |
| **Phase 4** | Auth system (register, login, password reset, session) | `PasswordUtils`, `AuthService`, `SessionManager` |
| **Phase 5** | DB CRUD operations | `HabitRepository` |
| **Phase 6** | Generic HabitManager + business logic | `HabitManager` |
| **Phase 7** | Background threads | `ReminderThread`, `AutoSaveThread` |
| **Phase 8** | CLI menu & full integration | `HabitTrackerApp` |
| **Phase 9** | Testing, edge cases, documentation | README, inline comments |

---

## 8. Rubric Mapping

| Criteria | How This Project Addresses It |
|---|---|
| **Advanced Java Features** | All 4 features used meaningfully — not bolted on. Each has a clear functional purpose |
| **Program Functionality & Code Quality** | Modular packages, single-responsibility classes, clear naming, no monolithic methods |
| **Documentation & Demonstration** | This plan + implementation.md + inline Javadoc comments + README with run instructions |
| **Problem Definition & Objective** | Clearly defined in Section 1 with context and articulated goals |
| **Analysis and Interpretation** | Demonstrated through streak logic, session validation, thread synchronization decisions |

---

## 9. Android Expansion Notes

These design decisions keep Android migration simple:

- `DatabaseManager` uses standard JDBC SQL — same queries work with Android's `SQLiteDatabase` or Room
- `HabitRepository` is the **only** DB-touching class — one file to rewrite for Android
- All model classes (`Habit`, `User`, `Streak`) are pure Java POJOs — zero changes needed
- `AuthService` is UI-agnostic — swap CLI calls for Android Activity calls directly
- `SessionManager` concept maps to Android `SharedPreferences` for Remember Me
