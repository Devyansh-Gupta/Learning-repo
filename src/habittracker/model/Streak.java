package habittracker.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Represents streak tracking for a habit.
 * Tracks current streak, best streak, and last completion date.
 */
public class Streak {
    private int habitId;
    private int currentStreak;
    private int bestStreak;
    private String lastCompleted;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    public Streak(int habitId) {
        this.habitId = habitId;
        this.currentStreak = 0;
        this.bestStreak = 0;
        this.lastCompleted = null;
    }

    public Streak(int habitId, int currentStreak, int bestStreak, String lastCompleted) {
        this.habitId = habitId;
        this.currentStreak = currentStreak;
        this.bestStreak = bestStreak;
        this.lastCompleted = lastCompleted;
    }

    public int getHabitId() {
        return habitId;
    }

    public int getCurrentStreak() {
        return currentStreak;
    }

    public void setCurrentStreak(int currentStreak) {
        this.currentStreak = currentStreak;
    }

    public int getBestStreak() {
        return bestStreak;
    }

    public void setBestStreak(int bestStreak) {
        this.bestStreak = bestStreak;
    }

    public String getLastCompleted() {
        return lastCompleted;
    }

    public void setLastCompleted(String lastCompleted) {
        this.lastCompleted = lastCompleted;
    }

    /**
     * Updates streak based on completion date.
     * - If lastCompleted is yesterday: increment streak
     * - If lastCompleted is today: no change (already marked)
     * - Otherwise: reset streak to 1
     */
    public void updateStreak(String today) {
        if (lastCompleted == null) {
            currentStreak = 1;
        } else {
            LocalDate lastDate = LocalDate.parse(lastCompleted, DATE_FORMAT);
            LocalDate todayDate = LocalDate.parse(today, DATE_FORMAT);
            long daysBetween = ChronoUnit.DAYS.between(lastDate, todayDate);

            if (daysBetween == 0) {
                // Already marked today - do nothing
                return;
            } else if (daysBetween == 1) {
                // Consecutive day - increment streak
                currentStreak++;
            } else {
                // Streak broken - reset to 1
                currentStreak = 1;
            }
        }

        // Update best streak if needed
        if (currentStreak > bestStreak) {
            bestStreak = currentStreak;
        }

        // Update last completed date
        lastCompleted = today;
    }

    @Override
    public String toString() {
        return "Current: " + currentStreak + " | Best: " + bestStreak;
    }
}
