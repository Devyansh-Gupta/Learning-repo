package habittracker.model;

import habittracker.interfaces.Trackable;
import habittracker.interfaces.Categorizable;
import habittracker.exceptions.HabitNotFoundException;
import habittracker.exceptions.InvalidHabitException;

/**
 * Daily habit implementation.
 * Represents a habit that should be completed every day.
 */
public class DailyHabit extends Habit implements Trackable, Categorizable {
    private Streak streak;

    public DailyHabit() {
        super();
    }

    public DailyHabit(int userId, String name, Category category, String createdAt) {
        super(userId, name, category, createdAt);
    }

    public DailyHabit(int id, int userId, String name, Category category,
                      String reminderTime, String createdAt) {
        super(id, userId, name, category, reminderTime, createdAt);
    }

    @Override
    public String getType() {
        return "DAILY";
    }

    @Override
    public void markComplete() throws HabitNotFoundException, InvalidHabitException {
        if (this.id == 0) {
            throw new HabitNotFoundException("Habit has not been saved yet");
        }
        // Actual logging is done via HabitRepository
    }

    @Override
    public Streak getStreak() {
        return streak;
    }

    public void setStreak(Streak streak) {
        this.streak = streak;
    }
}
