# Habit Tracker — Implementation Guide

> This document walks through every class, its responsibilities, key methods, and important implementation notes. Follow the phases in order — each phase builds on the previous one.

---

## Phase 1 — Project Setup & Database

### `DatabaseManager.java`
**Package:** `habittracker.db`
**Pattern:** Singleton — only one DB connection exists at a time.

**Responsibilities:**
- Open/close the SQLite connection via JDBC
- Run `CREATE TABLE IF NOT EXISTS` for all 4 tables on first launch
- Provide a `getConnection()` method used by `HabitRepository` and `AuthService`

**Key Implementation Notes:**
- Use `Class.forName("org.sqlite.JDBC")` to load the driver
- DB file path: `habittracker.db` in the working directory
- Wrap connection creation in try-catch, throw `DatabaseException` on failure
- Tables to create in order (respect FK order): `users` → `habits` → `habit_logs` → `streaks`

```java
// Skeleton
public class DatabaseManager {
    private static DatabaseManager instance;
    private Connection connection;
    private static final String DB_URL = "jdbc:sqlite:habittracker.db";

    private DatabaseManager() throws DatabaseException { ... }
    public static DatabaseManager getInstance() throws DatabaseException { ... }
    public Connection getConnection() { ... }
    private void initializeTables() throws DatabaseException { ... }
    public void closeConnection() { ... }
}
```

---

## Phase 2 — Model Classes & Interfaces

### `Trackable.java`
**Package:** `habittracker.interfaces`

```java
public interface Trackable {
    void markComplete() throws HabitNotFoundException;
    Streak getStreak();
}
```

### `Categorizable.java`
**Package:** `habittracker.interfaces`

```java
public interface Categorizable {
    Category getCategory();
    void setCategory(Category category);
}
```

---

### `Habit.java` (Abstract)
**Package:** `habittracker.model`

**Fields (all private/protected with getters):**
- `int id` — DB primary key
- `int userId` — FK to owner
- `String name`
- `Category category`
- `String reminderTime` — nullable, "HH:MM" format
- `String createdAt`

**Abstract method:** `abstract String getType()` — returns `"DAILY"` or `"WEEKLY"`

**Concrete methods:**
- Getters/setters for all fields (encapsulation)
- `toString()` — formatted display string for CLI

---

### `DailyHabit.java`
**Package:** `habittracker.model`
**Extends:** `Habit`
**Implements:** `Trackable`, `Categorizable`

- `getType()` returns `"DAILY"`
- `markComplete()` — delegates to `HabitRepository` to log today's date
- `getStreak()` — returns associated `Streak` object

---

### `WeeklyHabit.java`
**Package:** `habittracker.model`
**Extends:** `Habit`
**Implements:** `Trackable`, `Categorizable`

- `getType()` returns `"WEEKLY"`
- Adds field: `List<String> targetDays` — e.g., `["MONDAY", "THURSDAY"]`
- `markComplete()` — checks if today is a target day before marking; throws `InvalidHabitException` if not

---

### `Category.java`
**Package:** `habittracker.model`

```java
public enum Category {
    HEALTH, STUDY, FITNESS, FINANCE, OTHER;

    public static Category fromString(String value) throws InvalidHabitException {
        // Parse with try-catch, throw InvalidHabitException on unknown value
    }
}
```

---

### `Streak.java`
**Package:** `habittracker.model`

**Fields:**
- `int habitId`
- `int currentStreak`
- `int bestStreak`
- `String lastCompleted` — YYYY-MM-DD

**Key method — `updateStreak()`:**
```
if lastCompleted == yesterday:
    currentStreak++
    if currentStreak > bestStreak: bestStreak = currentStreak
else if lastCompleted == today:
    do nothing (already marked)
else:
    currentStreak = 1   // streak broken, restart

lastCompleted = today
```

---

### `User.java`
**Package:** `habittracker.model`

**Fields:**
- `int id`, `String username`, `String passwordHash`
- `String securityQuestion`, `String securityAnswerHash`
- `String createdAt`

Purely a data-holding class (POJO). No business logic here.

---

## Phase 3 — Custom Exceptions

**Package:** `habittracker.exceptions`

All exceptions extend `Exception` (checked) so callers are forced to handle them.

```java
// AuthException — bad login, expired session, wrong security answer
public class AuthException extends Exception {
    public AuthException(String message) { super(message); }
}

// InvalidHabitException — blank name, duplicate, wrong day for WeeklyHabit
public class InvalidHabitException extends Exception { ... }

// HabitNotFoundException — habit ID doesn't exist or doesn't belong to user
public class HabitNotFoundException extends Exception { ... }

// DatabaseException — wraps SQLExceptions with a readable message
public class DatabaseException extends Exception {
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

---

## Phase 4 — Authentication System

### `PasswordUtils.java`
**Package:** `habittracker.auth`

**Static utility class — never instantiated.**

```java
public class PasswordUtils {
    public static String hash(String input) {
        // Use MessageDigest with SHA-256
        // Return hex string representation
    }

    public static boolean verify(String input, String storedHash) {
        return hash(input).equals(storedHash);
    }
}
```

---

### `SessionManager.java`
**Package:** `habittracker.auth`
**Uses:** `FileOutputStream`, `BufferedReader`, `FileReader` — satisfies I/O requirement

**File:** `session.dat` in working directory

**Format of session.dat:**
```
username:timestamp_millis
```

**Key Methods:**

```java
public void saveSession(String username) throws IOException
// Write "username:System.currentTimeMillis()" to session.dat using FileOutputStream + BufferedWriter

public String loadSession() throws IOException
// Read session.dat with BufferedReader
// Check if timestamp is within 7 days (7 * 24 * 60 * 60 * 1000 ms)
// Return username if valid, null if expired or missing

public void clearSession()
// Delete session.dat or overwrite with empty content
```

**Important:** Wrap all operations in try-catch-finally. Always close streams in `finally` block (or use try-with-resources).

---

### `AuthService.java`
**Package:** `habittracker.auth`

**Dependencies:** `DatabaseManager`, `PasswordUtils`, `SessionManager`

**Key Methods:**

```java
public User register(String username, String password,
                     String securityQuestion, String securityAnswer)
    throws AuthException, DatabaseException
// 1. Check username not blank → InvalidHabitException
// 2. Check username not already taken (query DB) → AuthException if taken
// 3. Hash password and answer
// 4. INSERT into users table
// 5. Return User object

public User login(String username, String password)
    throws AuthException, DatabaseException
// 1. SELECT user by username
// 2. If not found → AuthException("User not found")
// 3. PasswordUtils.verify(password, storedHash)
// 4. If mismatch → AuthException("Incorrect password")
// 5. Return User object

public User autoLogin() throws AuthException, DatabaseException, IOException
// 1. SessionManager.loadSession() → username or null
// 2. If null → throw AuthException("No valid session")
// 3. Fetch user from DB and return

public void logout(User user)
// SessionManager.clearSession()

public void resetPassword(String username, String answer, String newPassword)
    throws AuthException, DatabaseException
// 1. Fetch user by username
// 2. Verify security answer hash
// 3. Update password_hash in DB
```

---

## Phase 5 — Database CRUD

### `HabitRepository.java`
**Package:** `habittracker.db`

This is the **only class** that touches the SQLite DB for habit-related data.

**Key Methods:**

```java
// --- Habits ---
public int insertHabit(Habit habit) throws DatabaseException
// INSERT into habits, return generated ID

public List<Habit> getHabitsByUser(int userId) throws DatabaseException
// SELECT all habits WHERE user_id = ?
// For each row, instantiate DailyHabit or WeeklyHabit based on 'type' column
// Also load associated Streak for each habit

public void deleteHabit(int habitId, int userId) throws HabitNotFoundException, DatabaseException
// Check habit exists and belongs to this user
// DELETE from habits (cascade to habit_logs and streaks)

// --- Habit Logs ---
public void logCompletion(int habitId, String date) throws DatabaseException
// INSERT INTO habit_logs (habit_id, completed_date) — ignore duplicates (INSERT OR IGNORE)

public boolean isCompletedToday(int habitId) throws DatabaseException
// SELECT count WHERE habit_id = ? AND completed_date = today

// --- Streaks ---
public Streak getStreak(int habitId) throws DatabaseException
// SELECT from streaks WHERE habit_id = ?

public void updateStreak(Streak streak) throws DatabaseException
// UPDATE streaks SET current_streak, best_streak, last_completed WHERE habit_id = ?

public void insertStreak(int habitId) throws DatabaseException
// INSERT default streak row when a new habit is created
```

**Important Implementation Notes:**
- Always use `PreparedStatement` — never string-concatenate SQL (SQL injection prevention)
- Wrap every method body in try-catch, rethrow as `DatabaseException`
- Use `ResultSet` → model mapping in a private helper method `mapRowToHabit(ResultSet rs)`

---

## Phase 6 — Generic HabitManager

### `HabitManager.java`
**Package:** `habittracker.manager`

```java
public class HabitManager<T extends Habit> {
    private List<T> habits;
    private HabitRepository repository;
    private User currentUser;

    public HabitManager(User user, HabitRepository repository) { ... }

    public void loadHabits() throws DatabaseException
    // Fetch from DB and populate in-memory list

    public void addHabit(T habit) throws InvalidHabitException, DatabaseException
    // Validate → insert to DB → add to in-memory list

    public void removeHabit(int habitId) throws HabitNotFoundException, DatabaseException
    // Remove from DB → remove from in-memory list

    public void markComplete(int habitId) throws HabitNotFoundException, DatabaseException
    // Find habit in list → call habit.markComplete()
    // Update streak via repository

    public List<T> getAllHabits() { return habits; }

    // ★ Generic filter method — key Generics demonstration
    public <T extends Habit> List<T> filterByCategory(List<T> habitList, Category category) {
        // Stream/loop through, return filtered list
    }

    public List<T> getHabitsWithReminders()
    // Return habits where reminderTime != null
}
```

---

## Phase 7 — Background Threads

### `ReminderThread.java`
**Package:** `habittracker.threads`
**Implements:** `Runnable`

**Logic:**
```
while (running):
    currentTime = LocalTime.now() formatted as "HH:MM"
    for each habit in habitsWithReminders:
        if habit.reminderTime == currentTime:
            print alert to console
    Thread.sleep(60_000)  // check every minute
```

**Key points:**
- Use a `volatile boolean running` flag for clean shutdown
- Provide `stop()` method that sets `running = false`
- The `HabitTrackerApp` starts this as `new Thread(reminderThread).start()`
- Use `synchronized` on the habit list if `AutoSaveThread` also touches it

---

### `AutoSaveThread.java`
**Package:** `habittracker.threads`
**Extends:** `Thread`

**Logic:**
```
setDaemon(true)  // dies when main thread dies
while (true):
    Thread.sleep(300_000)  // every 5 minutes
    // Sync any in-memory streak changes back to DB
    // Print "Auto-saved." to console
```

**Key points:**
- `setDaemon(true)` ensures it doesn't block app shutdown
- Use `synchronized` block when accessing shared `HabitManager` state
- This is the primary demonstration of thread synchronization

---

## Phase 8 — CLI Entry Point

### `HabitTrackerApp.java`
**Package:** `habittracker.main`

This is the top-level orchestrator. It should contain **only** UI/menu logic — no business logic.

**Startup sequence:**
```java
public static void main(String[] args) {
    // 1. DatabaseManager.getInstance() — init DB + tables
    // 2. AuthService.autoLogin() → if success, skip to main menu
    // 3. If AuthException, show auth menu (login/register/forgot password)
    // 4. After login, create HabitManager for the logged-in user
    // 5. Load habits from DB into HabitManager
    // 6. Start ReminderThread and AutoSaveThread
    // 7. Show main menu loop
    // 8. On logout/exit: stop threads, close DB connection, exit
}
```

**CLI Menu Structure:**
```
=== HABIT TRACKER ===
Logged in as: raghav

[1] View All Habits
[2] Add Habit
[3] Mark Habit Complete (Today)
[4] View Streaks
[5] Filter by Category
[6] Delete Habit
[7] Manage Reminders
[8] Logout
[0] Exit

Enter choice:
```

**Input handling:**
- Use `Scanner` for all user input
- Wrap numeric input parsing in try-catch (`NumberFormatException`)
- All menu methods should be private static helpers, not one giant main method

---

## Phase 9 — Testing Checklist

Go through each scenario manually before demo:

### Auth
- [ ] Register new user → success
- [ ] Register duplicate username → error message shown
- [ ] Login with wrong password → error message shown
- [ ] Login with correct credentials → main menu shown
- [ ] Logout → session.dat cleared
- [ ] Re-launch app → Remember Me auto-login works
- [ ] Re-launch after 7+ day session → asks to login again
- [ ] Forgot password → wrong answer → error; correct answer → reset works

### Habits
- [ ] Add DailyHabit → appears in list
- [ ] Add WeeklyHabit → appears in list
- [ ] Add habit with blank name → InvalidHabitException shown
- [ ] Mark habit complete → streak updates
- [ ] Mark habit complete again same day → no duplicate log
- [ ] Mark WeeklyHabit on wrong day → error shown
- [ ] Delete habit → removed from list and DB
- [ ] Delete non-existent habit ID → HabitNotFoundException shown
- [ ] Filter by category → correct subset shown

### Threads
- [ ] Set a reminder for 1 minute from now → alert appears in console
- [ ] App runs 5+ minutes → "Auto-saved." printed
- [ ] Logout → threads stop cleanly

### Edge Cases
- [ ] Empty habit list → friendly "No habits yet" message
- [ ] Non-numeric input in menu → handled gracefully (no crash)
- [ ] DB file missing/corrupt on startup → DatabaseException caught and reported

---

## Code Quality Standards

- Every class has a Javadoc comment explaining its purpose
- Every public method has a Javadoc with `@param` and `@throws`
- No method exceeds ~40 lines — extract helpers if needed
- No raw SQL strings outside `HabitRepository`
- No `System.exit()` calls except in `HabitTrackerApp.main()`
- All `Scanner` and DB resources closed properly (try-with-resources preferred)
- No `e.printStackTrace()` in production code — always show a user-friendly message and log the cause

---

## README Structure (for Submission)

```
# Habit Tracker

## How to Run
1. Ensure JDK 11+ is installed
2. Add sqlite-jdbc JAR to classpath
3. Compile: javac -cp .:sqlite-jdbc.jar -d out src/**/*.java
4. Run:     java -cp .:sqlite-jdbc.jar:out habittracker.main.HabitTrackerApp

## Features
...

## Advanced Java Features Used
...

## Project Structure
...

## Database
SQLite DB file (habittracker.db) is auto-created on first run.
```
