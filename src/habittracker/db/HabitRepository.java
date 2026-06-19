package habittracker.db;

import habittracker.model.*;
import habittracker.exceptions.HabitNotFoundException;
import habittracker.exceptions.DatabaseException;
import habittracker.exceptions.InvalidHabitException;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for all habit-related database operations.
 * Contains all SQL queries - single point of database access.
 */
public class HabitRepository {
    private DatabaseManager dbManager;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    public HabitRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /**
     * Inserts a new habit and creates its streak record.
     * @return The generated habit ID
     */
    public int insertHabit(Habit habit) throws DatabaseException {
        String sql = "INSERT INTO habits (user_id, name, category, type, reminder_time, target_days, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = dbManager.getConnection();
            pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            pstmt.setInt(1, habit.getUserId());
            pstmt.setString(2, habit.getName());
            pstmt.setString(3, habit.getCategory().name());

            if (habit instanceof DailyHabit) {
                pstmt.setString(4, "DAILY");
                pstmt.setString(5, habit.getReminderTime());
                pstmt.setString(6, null);
            } else if (habit instanceof WeeklyHabit) {
                pstmt.setString(4, "WEEKLY");
                pstmt.setString(5, habit.getReminderTime());
                pstmt.setString(6, ((WeeklyHabit) habit).daysToString());
            } else {
                throw new DatabaseException("Unknown habit type");
            }

            pstmt.setString(7, habit.getCreatedAt());

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new DatabaseException("Failed to insert habit");
            }

            int habitId;
            rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                habitId = rs.getInt(1);
                habit.setId(habitId);
            } else {
                throw new DatabaseException("Failed to get habit ID");
            }

            insertStreak(habitId);
            return habitId;

        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert habit", e);
        } finally {
            closeQuietly(rs);
            closeQuietly(pstmt);
        }
    }

    /**
     * Gets all habits for a user, including their streaks.
     */
    public List<Habit> getHabitsByUser(int userId) throws DatabaseException {
        String sql = "SELECT * FROM habits WHERE user_id = ? ORDER BY created_at DESC";
        List<Habit> habits = new ArrayList<>();

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = dbManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, userId);
            rs = pstmt.executeQuery();

            List<Integer> habitIds = new ArrayList<>();
            while (rs.next()) {
                Habit habit = mapRowToHabit(rs);
                habitIds.add(habit.getId());
                habits.add(habit);
            }

            // Now get streaks for each habit
            for (int i = 0; i < habits.size(); i++) {
                Habit habit = habits.get(i);
                Streak streak = getStreak(habit.getId());
                habit.setStreak(streak);
            }

            return habits;

        } catch (SQLException e) {
            throw new DatabaseException("Failed to get habits", e);
        } finally {
            closeQuietly(rs);
            closeQuietly(pstmt);
        }
    }

    /**
     * Deletes a habit and its associated logs and streak.
     */
    public void deleteHabit(int habitId, int userId) throws HabitNotFoundException, DatabaseException {
        Connection conn = null;
        PreparedStatement checkStmt = null;
        ResultSet rs = null;

        try {
            conn = dbManager.getConnection();

            String checkSql = "SELECT id FROM habits WHERE id = ? AND user_id = ?";
            checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setInt(1, habitId);
            checkStmt.setInt(2, userId);
            rs = checkStmt.executeQuery();

            if (!rs.next()) {
                throw new HabitNotFoundException("Habit not found or does not belong to user");
            }
            closeQuietly(rs);
            closeQuietly(checkStmt);

            deleteHabitCascade(conn, habitId);

        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete habit", e);
        } finally {
            closeQuietly(rs);
            closeQuietly(checkStmt);
        }
    }

    private void deleteHabitCascade(Connection conn, int habitId) throws SQLException {
        String deleteLogsSql = "DELETE FROM habit_logs WHERE habit_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(deleteLogsSql)) {
            pstmt.setInt(1, habitId);
            pstmt.executeUpdate();
        }

        String deleteStreakSql = "DELETE FROM streaks WHERE habit_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(deleteStreakSql)) {
            pstmt.setInt(1, habitId);
            pstmt.executeUpdate();
        }

        String deleteHabitSql = "DELETE FROM habits WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(deleteHabitSql)) {
            pstmt.setInt(1, habitId);
            pstmt.executeUpdate();
        }
    }

    /**
     * Logs a habit completion for a specific date.
     * Uses INSERT OR IGNORE to prevent duplicates.
     */
    public void logCompletion(int habitId, String date) throws DatabaseException {
        String sql = "INSERT OR IGNORE INTO habit_logs (habit_id, completed_date) VALUES (?, ?)";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, habitId);
            pstmt.setString(2, date);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Failed to log completion", e);
        }
    }

    /**
     * Checks if a habit was completed today.
     */
    public boolean isCompletedToday(int habitId) throws DatabaseException {
        String today = LocalDate.now().format(DATE_FORMAT);
        String sql = "SELECT COUNT(*) FROM habit_logs WHERE habit_id = ? AND completed_date = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, habitId);
            pstmt.setString(2, today);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
            return false;

        } catch (SQLException e) {
            throw new DatabaseException("Failed to check completion", e);
        }
    }

    /**
     * Gets the streak for a habit.
     */
    public Streak getStreak(int habitId) throws DatabaseException {
        String sql = "SELECT * FROM streaks WHERE habit_id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, habitId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Streak streak = new Streak(
                        rs.getInt("habit_id"),
                        rs.getInt("current_streak"),
                        rs.getInt("best_streak"),
                        rs.getString("last_completed")
                    );
                    return streak;
                }
            }
            return new Streak(habitId);

        } catch (SQLException e) {
            throw new DatabaseException("Failed to get streak", e);
        }
    }

    /**
     * Updates a streak in the database.
     */
    public void updateStreak(Streak streak) throws DatabaseException {
        String sql = "UPDATE streaks SET current_streak = ?, best_streak = ?, last_completed = ? WHERE habit_id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, streak.getCurrentStreak());
            pstmt.setInt(2, streak.getBestStreak());
            pstmt.setString(3, streak.getLastCompleted());
            pstmt.setInt(4, streak.getHabitId());
            pstmt.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Failed to update streak", e);
        }
    }

    /**
     * Inserts a default streak record for a new habit.
     */
    public void insertStreak(int habitId) throws DatabaseException {
        String sql = "INSERT INTO streaks (habit_id, current_streak, best_streak, last_completed) VALUES (?, 0, 0, NULL)";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, habitId);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert streak", e);
        }
    }

    /**
     * Maps a ResultSet row to the appropriate Habit subtype.
     */
    private Habit mapRowToHabit(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int userId = rs.getInt("user_id");
        String name = rs.getString("name");
        Category category = Category.valueOf(rs.getString("category"));
        String type = rs.getString("type");
        String reminderTime = rs.getString("reminder_time");
        String createdAt = rs.getString("created_at");

        if ("DAILY".equals(type)) {
            return new DailyHabit(id, userId, name, category, reminderTime, createdAt);
        } else if ("WEEKLY".equals(type)) {
            String targetDays = rs.getString("target_days");
            return new WeeklyHabit(id, userId, name, category, reminderTime, targetDays, createdAt);
        } else {
            throw new SQLException("Unknown habit type: " + type);
        }
    }

    private void closeQuietly(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
                // Ignore
            }
        }
    }
}
