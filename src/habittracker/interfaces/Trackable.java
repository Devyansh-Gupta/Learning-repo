package habittracker.interfaces;

import habittracker.model.Streak;
import habittracker.exceptions.HabitNotFoundException;
import habittracker.exceptions.InvalidHabitException;

/**
 * Interface for trackable habits that can be marked complete.
 */
public interface Trackable {
    void markComplete() throws HabitNotFoundException, InvalidHabitException;
    Streak getStreak();
}
