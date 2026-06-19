package habittracker.exceptions;

/**
 * Exception thrown for invalid habit operations.
 * Includes blank names, duplicates, wrong day for WeeklyHabit, etc.
 */
public class InvalidHabitException extends Exception {
    public InvalidHabitException(String message) {
        super(message);
    }
}
