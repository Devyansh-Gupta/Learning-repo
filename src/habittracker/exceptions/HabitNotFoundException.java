package habittracker.exceptions;

/**
 * Exception thrown when a habit is not found.
 * Includes habit ID doesn't exist or doesn't belong to user.
 */
public class HabitNotFoundException extends Exception {
    public HabitNotFoundException(String message) {
        super(message);
    }
}
