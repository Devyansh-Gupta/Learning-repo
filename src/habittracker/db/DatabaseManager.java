package habittracker.db;

import habittracker.exceptions.DatabaseException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages the SQLite database connection and table initialization.
 * Implements Singleton pattern to ensure only one database connection exists.
 */
public class DatabaseManager {
    private static DatabaseManager instance;
    private Connection connection;
    private static final String DB_URL = "jdbc:sqlite:habittracker.db";

    private DatabaseManager() throws DatabaseException {
        try {
            Class.forName("org.sqlite.JDBC");
            this.connection = DriverManager.getConnection(DB_URL);
            initializeTables();
        } catch (ClassNotFoundException e) {
            throw new DatabaseException("SQLite JDBC driver not found", e);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to connect to database", e);
        }
    }

    public static DatabaseManager getInstance() throws DatabaseException {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                try {
                    Class.forName("org.sqlite.JDBC");
                    this.connection = DriverManager.getConnection(DB_URL);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException("SQLite JDBC driver not found", e);
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to reconnect to database", e);
                }
            }
        } catch (SQLException e) {
            // Connection is not available, try to reconnect
            try {
                Class.forName("org.sqlite.JDBC");
                this.connection = DriverManager.getConnection(DB_URL);
            } catch (Exception ex) {
                throw new RuntimeException("Failed to reconnect to database", ex);
            }
        }
        return connection;
    }

    private void initializeTables() throws DatabaseException {
        String createUsersTable = """
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT UNIQUE NOT NULL,
                password_hash TEXT NOT NULL,
                security_question TEXT NOT NULL,
                security_answer_hash TEXT NOT NULL,
                created_at TEXT NOT NULL
            )
            """;

        String createHabitsTable = """
            CREATE TABLE IF NOT EXISTS habits (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                name TEXT NOT NULL,
                category TEXT NOT NULL,
                type TEXT NOT NULL,
                reminder_time TEXT,
                target_days TEXT,
                created_at TEXT NOT NULL,
                FOREIGN KEY (user_id) REFERENCES users(id)
            )
            """;

        String createHabitLogsTable = """
            CREATE TABLE IF NOT EXISTS habit_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                habit_id INTEGER NOT NULL,
                completed_date TEXT NOT NULL,
                FOREIGN KEY (habit_id) REFERENCES habits(id)
            )
            """;

        String createStreaksTable = """
            CREATE TABLE IF NOT EXISTS streaks (
                habit_id INTEGER PRIMARY KEY,
                current_streak INTEGER DEFAULT 0,
                best_streak INTEGER DEFAULT 0,
                last_completed TEXT,
                FOREIGN KEY (habit_id) REFERENCES habits(id)
            )
            """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createUsersTable);
            stmt.execute(createHabitsTable);
            stmt.execute(createHabitLogsTable);
            stmt.execute(createStreaksTable);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to initialize database tables", e);
        }
    }

    public void closeConnection() throws DatabaseException {
        if (connection != null) {
            try {
                connection.close();
                instance = null;
            } catch (SQLException e) {
                throw new DatabaseException("Failed to close database connection", e);
            }
        }
    }
}
