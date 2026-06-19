package habittracker.threads;

import habittracker.model.Habit;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Background thread that checks for habit reminders.
 * Demonstrates Multithreading with Runnable implementation.
 * Checks every 60 seconds and prints alerts when reminder time matches.
 */
public class ReminderThread implements Runnable {
    private volatile boolean running;
    private List<Habit> habits;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    public ReminderThread(List<Habit> habits) {
        this.habits = habits;
        this.running = true;
    }

    @Override
    public void run() {
        System.out.println("[ReminderThread] Started - checking every 60 seconds");
        while (running) {
            try {
                checkReminders();
                Thread.sleep(60_000); // Check every minute
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        System.out.println("[ReminderThread] Stopped");
    }

    private void checkReminders() {
        String currentTime = LocalTime.now().format(TIME_FORMAT);

        synchronized (habits) {
            for (Habit habit : habits) {
                if (habit.getReminderTime() != null &&
                    habit.getReminderTime().equals(currentTime)) {
                    System.out.println("\n*** REMINDER ***");
                    System.out.println("Time to: " + habit.getName() + " (" + habit.getType() + ")");
                    System.out.println("Category: " + habit.getCategory());
                    System.out.println("***************\n");
                }
            }
        }
    }

    /**
     * Stops the reminder thread gracefully.
     */
    public void stop() {
        running = false;
    }
}
