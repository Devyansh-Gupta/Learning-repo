package habittracker.model;

import habittracker.interfaces.Trackable;
import habittracker.interfaces.Categorizable;
import habittracker.exceptions.HabitNotFoundException;
import habittracker.exceptions.InvalidHabitException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Weekly habit implementation.
 * Represents a habit that should be completed on specific days of the week.
 */
public class WeeklyHabit extends Habit implements Trackable, Categorizable {
    private List<String> targetDays;
    private Streak streak;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    public WeeklyHabit() {
        super();
        this.targetDays = new ArrayList<>();
    }

    public WeeklyHabit(int userId, String name, Category category, String createdAt) {
        super(userId, name, category, createdAt);
        this.targetDays = new ArrayList<>();
    }

    public WeeklyHabit(int id, int userId, String name, Category category,
                       String reminderTime, String targetDays, String createdAt) {
        super(id, userId, name, category, reminderTime, createdAt);
        this.targetDays = stringToDays(targetDays);
    }

    @Override
    public String getType() {
        return "WEEKLY";
    }

    public List<String> getTargetDays() {
        return targetDays;
    }

    public void setTargetDays(List<String> targetDays) {
        this.targetDays = targetDays;
    }

    public void addTargetDay(String day) {
        if (!targetDays.contains(day.toUpperCase())) {
            targetDays.add(day.toUpperCase());
        }
    }

    /**
     * Mark the habit complete for today.
     * Validates that today is a target day before marking.
     */
    @Override
    public void markComplete() throws HabitNotFoundException, InvalidHabitException {
        if (this.id == 0) {
            throw new HabitNotFoundException("Habit has not been saved yet");
        }

        String todayName = LocalDate.now().getDayOfWeek().name();
        if (!targetDays.contains(todayName)) {
            throw new InvalidHabitException(
                "Cannot mark complete on " + todayName + ". Target days: " + targetDays);
        }
    }

    @Override
    public Streak getStreak() {
        return streak;
    }

    public void setStreak(Streak streak) {
        this.streak = streak;
    }

    /**
     * Converts list of days to comma-separated string for DB storage.
     */
    public String daysToString() {
        return String.join(",", targetDays);
    }

    /**
     * Converts comma-separated string from DB to list of days.
     */
    private List<String> stringToDays(String days) {
        if (days == null || days.isBlank()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(days.split(",")));
    }

    @Override
    public String toString() {
        String reminder = (reminderTime != null) ? " @ " + reminderTime : "";
        return String.format("[%s] %s%s - %s (Days: %s)",
            getType(), name, reminder, category, targetDays);
    }
}
