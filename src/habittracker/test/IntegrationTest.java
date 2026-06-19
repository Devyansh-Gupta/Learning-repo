package habittracker.test;

import habittracker.auth.AuthService;
import habittracker.auth.SessionManager;
import habittracker.auth.PasswordUtils;
import habittracker.db.DatabaseManager;
import habittracker.db.HabitRepository;
import habittracker.model.*;
import habittracker.exceptions.*;
import habittracker.manager.HabitManager;

import java.util.List;

/**
 * Integration test for Habit Tracker.
 */
public class IntegrationTest {
    public static void main(String[] args) {
        System.out.println("=== Habit Tracker Integration Test ===\n");

        try {
            testDatabaseSetup();
            testPasswordUtils();
            testAuthService();
            testHabitRepository();
            testHabitManager();
            testStreakLogic();

            System.out.println("\n=== ALL TESTS PASSED ===");

        } catch (Exception e) {
            System.out.println("\n=== TEST FAILED ===");
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void testDatabaseSetup() throws Exception {
        System.out.println("Test 1: Database Setup");
        DatabaseManager db = DatabaseManager.getInstance();
        assert db.getConnection() != null : "Connection should not be null";
        System.out.println("  PASSED: Database initialized");
    }

    private static void testPasswordUtils() {
        System.out.println("Test 2: Password Utils");
        String hash1 = PasswordUtils.hash("password123");
        String hash2 = PasswordUtils.hash("password123");
        assert hash1.equals(hash2) : "Same password should produce same hash";
        assert !hash1.equals(PasswordUtils.hash("different")) : "Different password should produce different hash";
        assert PasswordUtils.verify("password123", hash1) : "Verification should pass for correct password";
        assert !PasswordUtils.verify("wrong", hash1) : "Verification should fail for wrong password";
        System.out.println("  PASSED: Password hashing and verification");
    }

    private static void testAuthService() throws Exception {
        System.out.println("Test 3: Auth Service");
        DatabaseManager db = DatabaseManager.getInstance();
        HabitRepository repo = new HabitRepository(db);
        AuthService auth = new AuthService(db, repo, new SessionManager());

        // Test registration
        String testUser = "testuser_" + System.currentTimeMillis();
        User user = auth.register(testUser, "testpass", "Question?", "answer");
        assert user.getUsername().equals(testUser) : "Username should match";
        System.out.println("  PASSED: User registration");

        // Test login
        User loggedIn = auth.login(testUser, "testpass");
        assert loggedIn != null : "Login should succeed";
        assert loggedIn.getUsername().equals(testUser) : "Username should match";
        System.out.println("  PASSED: User login");

        // Test password reset
        auth.resetPassword(testUser, "answer", "newpass");
        User resetUser = auth.login(testUser, "newpass");
        assert resetUser != null : "Login with new password should succeed";
        System.out.println("  PASSED: Password reset");
    }

    private static void testHabitRepository() throws Exception {
        System.out.println("Test 4: Habit Repository");
        DatabaseManager db = DatabaseManager.getInstance();
        HabitRepository repo = new HabitRepository(db);
        AuthService auth = new AuthService(db, repo, new SessionManager());

        String testUser = "habituser_" + System.currentTimeMillis();
        User user = auth.register(testUser, "pass", "Q?", "A");

        // Insert daily habit
        DailyHabit daily = new DailyHabit(user.getId(), "Exercise", Category.HEALTH, "2024-01-01");
        int dailyId = repo.insertHabit(daily);
        assert dailyId > 0 : "Daily habit ID should be positive";
        System.out.println("  PASSED: Insert daily habit");

        // Insert weekly habit
        WeeklyHabit weekly = new WeeklyHabit(user.getId(), "Grocery", Category.OTHER, "2024-01-01");
        weekly.addTargetDay("SATURDAY");
        weekly.addTargetDay("SUNDAY");
        int weeklyId = repo.insertHabit(weekly);
        assert weeklyId > 0 : "Weekly habit ID should be positive";
        System.out.println("  PASSED: Insert weekly habit");

        // Get habits
        List<Habit> habits = repo.getHabitsByUser(user.getId());
        assert habits.size() >= 2 : "Should have at least 2 habits";
        System.out.println("  PASSED: Get habits by user");

        // Log completion
        repo.logCompletion(dailyId, "2024-01-15");
        assert repo.isCompletedToday(dailyId) : "Should be marked complete";
        System.out.println("  PASSED: Log completion");

        // Delete habit
        repo.deleteHabit(dailyId, user.getId());
        habits = repo.getHabitsByUser(user.getId());
        assert habits.stream().noneMatch(h -> h.getId() == dailyId) : "Daily habit should be deleted";
        System.out.println("  PASSED: Delete habit");
    }

    private static void testHabitManager() throws Exception {
        System.out.println("Test 5: Habit Manager");
        DatabaseManager db = DatabaseManager.getInstance();
        HabitRepository repo = new HabitRepository(db);
        AuthService auth = new AuthService(db, repo, new SessionManager());

        String testUser = "manageruser_" + System.currentTimeMillis();
        User user = auth.register(testUser, "pass", "Q?", "A");

        @SuppressWarnings("unchecked")
        HabitManager<Habit> manager = new HabitManager<>(user, repo);

        // Add habit
        DailyHabit habit = new DailyHabit(user.getId(), "Read", Category.STUDY, "2024-01-01");
        manager.addHabit(habit);
        assert manager.getAllHabits().size() == 1 : "Should have 1 habit";
        System.out.println("  PASSED: Add habit");

        // Filter by category
        List<Habit> filtered = manager.filterByCategory(manager.getAllHabits(), Category.STUDY);
        assert filtered.size() == 1 : "Should have 1 study habit";
        System.out.println("  PASSED: Filter by category");

        // Remove habit
        manager.removeHabit(habit.getId());
        assert manager.getAllHabits().isEmpty() : "Should have no habits";
        System.out.println("  PASSED: Remove habit");
    }

    private static void testStreakLogic() {
        System.out.println("Test 6: Streak Logic");
        Streak streak = new Streak(1);

        // First completion
        streak.updateStreak("2024-01-15");
        assert streak.getCurrentStreak() == 1 : "First streak should be 1";
        assert streak.getBestStreak() == 1 : "Best streak should be 1";

        // Consecutive day
        streak.updateStreak("2024-01-16");
        assert streak.getCurrentStreak() == 2 : "Streak should be 2";
        assert streak.getBestStreak() == 2 : "Best streak should be 2";

        // Another consecutive day
        streak.updateStreak("2024-01-17");
        assert streak.getCurrentStreak() == 3 : "Streak should be 3";

        // Skip a day - streak should reset
        streak.updateStreak("2024-01-19");
        assert streak.getCurrentStreak() == 1 : "Streak should reset to 1";
        assert streak.getBestStreak() == 3 : "Best streak should remain 3";

        System.out.println("  PASSED: Streak logic");
    }
}
