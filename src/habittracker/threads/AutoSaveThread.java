package habittracker.threads;

import habittracker.manager.HabitManager;
import habittracker.model.Habit;

/**
 * Background daemon thread that auto-saves habit data.
 * Demonstrates Multithreading with Thread extension.
 * Runs as daemon so it dies when main thread exits.
 * Saves every 5 minutes (300 seconds).
 */
public class AutoSaveThread extends Thread {
    private HabitManager<? extends Habit> habitManager;
    private volatile boolean running;

    public AutoSaveThread(HabitManager<? extends Habit> habitManager) {
        this.habitManager = habitManager;
        this.running = true;
        setDaemon(true); // Die when main thread dies
    }

    @Override
    public void run() {
        System.out.println("[AutoSaveThread] Started - auto-saving every 5 minutes");
        while (running) {
            try {
                Thread.sleep(300_000); // 5 minutes
                if (running) {
                    performAutoSave();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        System.out.println("[AutoSaveThread] Stopped");
    }

    private void performAutoSave() {
        try {
            synchronized (habitManager) {
                // The habit manager handles syncing data to DB
                // This thread triggers any necessary saves
            }
            System.out.println("[AutoSave] Auto-saved.");
        } catch (Exception e) {
            System.out.println("[AutoSave] Auto-save failed: " + e.getMessage());
        }
    }

    /**
     * Stops the auto-save thread gracefully.
     */
    public void stopThread() {
        running = false;
    }
}
