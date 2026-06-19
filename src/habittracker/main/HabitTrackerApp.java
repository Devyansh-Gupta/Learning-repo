package habittracker.main;

import habittracker.auth.AuthService;
import habittracker.auth.SessionManager;
import habittracker.auth.PasswordUtils;
import habittracker.db.DatabaseManager;
import habittracker.db.HabitRepository;
import habittracker.manager.HabitManager;
import habittracker.model.*;
import habittracker.threads.ReminderThread;
import habittracker.threads.AutoSaveThread;
import habittracker.exceptions.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * Main entry point for the Habit Tracker CLI application.
 * Orchestrates all components and handles user interaction.
 */
public class HabitTrackerApp {
    private static DatabaseManager dbManager;
    private static HabitRepository habitRepository;
    private static AuthService authService;
    private static HabitManager<Habit> habitManager;
    private static User currentUser;
    private static ReminderThread reminderThread;
    private static AutoSaveThread autoSaveThread;
    private static Scanner scanner;
    private static boolean running;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    public static void main(String[] args) {
        System.out.println("=== HABIT TRACKER ===\n");

        try {
            initializeApp();

            if (tryAutoLogin()) {
                showMainMenu();
            } else {
                showAuthMenu();
            }

        } catch (Exception e) {
            System.out.println("Fatal error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            shutdown();
        }
    }

    private static void initializeApp() throws DatabaseException {
        dbManager = DatabaseManager.getInstance();
        habitRepository = new HabitRepository(dbManager);
        authService = new AuthService(dbManager, habitRepository, new SessionManager());
        scanner = new Scanner(System.in);
        running = true;
    }

    private static boolean tryAutoLogin() {
        try {
            currentUser = authService.autoLogin();
            System.out.println("Welcome back, " + currentUser.getUsername() + "!");
            habitManager = new HabitManager<>(currentUser, habitRepository);
            habitManager.loadHabits();
            startThreads();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static void showAuthMenu() {
        while (running) {
            System.out.println("\n=== AUTH MENU ===");
            System.out.println("[1] Login");
            System.out.println("[2] Register");
            System.out.println("[3] Forgot Password");
            System.out.println("[0] Exit");
            System.out.print("\nEnter choice: ");

            String choice = scanner.nextLine().trim();

            try {
                switch (choice) {
                    case "1":
                        handleLogin();
                        if (currentUser != null) {
                            return;
                        }
                        break;
                    case "2":
                        handleRegister();
                        break;
                    case "3":
                        handleForgotPassword();
                        break;
                    case "0":
                        running = false;
                        return;
                    default:
                        System.out.println("Invalid choice. Please try again.");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private static void handleLogin() throws AuthException, DatabaseException {
        System.out.print("Username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Password: ");
        String password = scanner.nextLine();

        currentUser = authService.login(username, password);
        habitManager = new HabitManager<>(currentUser, habitRepository);
        habitManager.loadHabits();
        startThreads();
    }

    private static void handleRegister() throws AuthException, DatabaseException {
        System.out.print("Username: ");
        String username = scanner.nextLine().trim();

        System.out.print("Password: ");
        String password = scanner.nextLine();

        System.out.print("Security Question (e.g., What is your pet's name?): ");
        String question = scanner.nextLine().trim();

        System.out.print("Security Answer: ");
        String answer = scanner.nextLine();

        currentUser = authService.register(username, password, question, answer);
        System.out.println("Registration successful! Please login.");
    }

    private static void handleForgotPassword() {
        try {
            System.out.print("Username: ");
            String username = scanner.nextLine().trim();

            User user = authService.getUserByUsername(username);
            if (user == null) {
                System.out.println("User not found.");
                return;
            }

            System.out.println("Security Question: " + user.getSecurityQuestion());
            System.out.print("Your Answer: ");
            String answer = scanner.nextLine();

            System.out.print("New Password: ");
            String newPassword = scanner.nextLine();

            authService.resetPassword(username, answer, newPassword);
            System.out.println("Password reset successful! Please login.");

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void startThreads() {
        List<Habit> habits = habitManager.getAllHabits();
        reminderThread = new ReminderThread(habits);
        new Thread(reminderThread).start();

        autoSaveThread = new AutoSaveThread(habitManager);
        autoSaveThread.start();
    }

    private static void showMainMenu() {
        while (running && currentUser != null) {
            System.out.println("\n=== HABIT TRACKER ===");
            System.out.println("Logged in as: " + currentUser.getUsername());
            System.out.println("\n[1] View All Habits");
            System.out.println("[2] Add Habit");
            System.out.println("[3] Mark Habit Complete (Today)");
            System.out.println("[4] View Streaks");
            System.out.println("[5] Filter by Category");
            System.out.println("[6] Delete Habit");
            System.out.println("[7] Manage Reminders");
            System.out.println("[8] Logout");
            System.out.println("[0] Exit");
            System.out.print("\nEnter choice: ");

            String choice = scanner.nextLine().trim();

            try {
                switch (choice) {
                    case "1":
                        viewAllHabits();
                        break;
                    case "2":
                        addHabit();
                        break;
                    case "3":
                        markHabitComplete();
                        break;
                    case "4":
                        viewStreaks();
                        break;
                    case "5":
                        filterByCategory();
                        break;
                    case "6":
                        deleteHabit();
                        break;
                    case "7":
                        manageReminders();
                        break;
                    case "8":
                        handleLogout();
                        return;
                    case "0":
                        running = false;
                        return;
                    default:
                        System.out.println("Invalid choice. Please try again.");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private static void viewAllHabits() {
        List<Habit> habits = habitManager.getAllHabits();

        if (habits.isEmpty()) {
            System.out.println("\nNo habits yet. Add your first habit!");
            return;
        }

        System.out.println("\n=== YOUR HABITS ===");
        for (int i = 0; i < habits.size(); i++) {
            Habit habit = habits.get(i);
            Streak streak = habit.getStreak();
            String streakInfo = streak != null ? " | Streak: " + streak : "";
            boolean completed = false;
            try {
                completed = habitManager.isCompletedToday(habit.getId());
            } catch (Exception e) {
            }
            String status = completed ? " [DONE]" : "";
            System.out.println((i + 1) + ". " + habit + streakInfo + status);
        }
    }

    private static void addHabit() {
        try {
            System.out.println("\n=== ADD NEW HABIT ===");

            System.out.print("Habit Name: ");
            String name = scanner.nextLine().trim();
            if (name.isBlank()) {
                System.out.println("Habit name cannot be blank.");
                return;
            }

            System.out.println("Category: ");
            System.out.println("  1. HEALTH");
            System.out.println("  2. STUDY");
            System.out.println("  3. FITNESS");
            System.out.println("  4. FINANCE");
            System.out.println("  5. OTHER");
            System.out.print("Enter choice (1-5): ");

            String catChoice = scanner.nextLine().trim();
            Category category;
            switch (catChoice) {
                case "1": category = Category.HEALTH; break;
                case "2": category = Category.STUDY; break;
                case "3": category = Category.FITNESS; break;
                case "4": category = Category.FINANCE; break;
                default: category = Category.OTHER;
            }

            System.out.print("Habit Type: (D)aily or (W)eekly: ");
            String type = scanner.nextLine().trim().toUpperCase();

            Habit habit;
            if ("W".equals(type)) {
                System.out.print("Target days (e.g., MONDAY,THURSDAY): ");
                String daysInput = scanner.nextLine().trim().toUpperCase();
                List<String> targetDays = java.util.Arrays.asList(daysInput.split(","));
                habit = new WeeklyHabit(currentUser.getId(), name, category, LocalDate.now().format(DATE_FORMAT));
                ((WeeklyHabit) habit).setTargetDays(targetDays);
            } else {
                habit = new DailyHabit(currentUser.getId(), name, category, LocalDate.now().format(DATE_FORMAT));
            }

            System.out.print("Set reminder? Enter time (HH:MM) or press Enter to skip: ");
            String reminder = scanner.nextLine().trim();
            if (!reminder.isBlank()) {
                habit.setReminderTime(reminder);
            }

            habitManager.addHabit(habit);
            System.out.println("Habit added successfully!");

        } catch (InvalidHabitException e) {
            System.out.println("Invalid habit: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Error adding habit: " + e.getMessage());
        }
    }

    private static void markHabitComplete() {
        List<Habit> habits = habitManager.getAllHabits();
        if (habits.isEmpty()) {
            System.out.println("No habits to complete. Add a habit first!");
            return;
        }

        viewAllHabits();
        System.out.print("\nEnter habit number to mark complete: ");

        try {
            String input = scanner.nextLine().trim();
            int num = Integer.parseInt(input) - 1;

            if (num < 0 || num >= habits.size()) {
                System.out.println("Invalid habit number.");
                return;
            }

            Habit habit = habits.get(num);
            habitManager.markComplete(habit.getId());
            System.out.println("Marked complete: " + habit.getName());

        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid number.");
        } catch (HabitNotFoundException e) {
            System.out.println("Habit not found.");
        } catch (InvalidHabitException e) {
            System.out.println("Cannot mark complete: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void viewStreaks() {
        List<Habit> habits = habitManager.getAllHabits();

        if (habits.isEmpty()) {
            System.out.println("No habits yet.");
            return;
        }

        System.out.println("\n=== STREAKS ===");
        for (Habit habit : habits) {
            Streak streak = habit.getStreak();
            if (streak != null) {
                System.out.println(habit.getName() + ": " + streak);
            } else {
                System.out.println(habit.getName() + ": No streak data");
            }
        }
    }

    private static void filterByCategory() {
        System.out.println("\n=== FILTER BY CATEGORY ===");
        System.out.println("  1. HEALTH");
        System.out.println("  2. STUDY");
        System.out.println("  3. FITNESS");
        System.out.println("  4. FINANCE");
        System.out.println("  5. OTHER");
        System.out.print("Enter choice (1-5): ");

        try {
            String catChoice = scanner.nextLine().trim();
            Category category;
            switch (catChoice) {
                case "1": category = Category.HEALTH; break;
                case "2": category = Category.STUDY; break;
                case "3": category = Category.FITNESS; break;
                case "4": category = Category.FINANCE; break;
                default: category = Category.OTHER;
            }

            List<Habit> allHabits = habitManager.getAllHabits();
            List<Habit> filtered = habitManager.filterByCategory(allHabits, category);

            if (filtered.isEmpty()) {
                System.out.println("No habits in category: " + category);
            } else {
                System.out.println("\nHabits in " + category + ":");
                for (Habit habit : filtered) {
                    System.out.println("- " + habit);
                }
            }

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void deleteHabit() {
        List<Habit> habits = habitManager.getAllHabits();
        if (habits.isEmpty()) {
            System.out.println("No habits to delete.");
            return;
        }

        viewAllHabits();
        System.out.print("\nEnter habit number to delete: ");

        try {
            String input = scanner.nextLine().trim();
            int num = Integer.parseInt(input) - 1;

            if (num < 0 || num >= habits.size()) {
                System.out.println("Invalid habit number.");
                return;
            }

            Habit habit = habits.get(num);
            System.out.print("Confirm delete '" + habit.getName() + "'? (Y/N): ");
            String confirm = scanner.nextLine().trim().toUpperCase();

            if ("Y".equals(confirm)) {
                habitManager.removeHabit(habit.getId());
                System.out.println("Habit deleted.");
            } else {
                System.out.println("Deletion cancelled.");
            }

        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid number.");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void manageReminders() {
        List<Habit> habits = habitManager.getAllHabits();
        if (habits.isEmpty()) {
            System.out.println("No habits to manage reminders for.");
            return;
        }

        System.out.println("\n=== MANAGE REMINDERS ===");
        viewAllHabits();

        System.out.print("\nEnter habit number to set/update reminder: ");

        try {
            String input = scanner.nextLine().trim();
            int num = Integer.parseInt(input) - 1;

            if (num < 0 || num >= habits.size()) {
                System.out.println("Invalid habit number.");
                return;
            }

            Habit habit = habits.get(num);
            System.out.print("Enter reminder time (HH:MM) or press Enter to remove: ");
            String reminder = scanner.nextLine().trim();

            habit.setReminderTime(reminder.isBlank() ? null : reminder);
            System.out.println("Reminder updated.");

        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid number.");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void handleLogout() {
        stopThreads();
        authService.logout();
        currentUser = null;
        habitManager = null;
        System.out.println("Logged out successfully.");
    }

    private static void stopThreads() {
        if (reminderThread != null) {
            reminderThread.stop();
        }
        if (autoSaveThread != null) {
            autoSaveThread.stopThread();
        }
    }

    private static void shutdown() {
        System.out.println("\nShutting down...");
        stopThreads();
        try {
            if (dbManager != null) {
                dbManager.closeConnection();
            }
            if (scanner != null) {
                scanner.close();
            }
        } catch (Exception e) {
            System.out.println("Error during shutdown: " + e.getMessage());
        }
        System.out.println("Goodbye!");
    }
}
