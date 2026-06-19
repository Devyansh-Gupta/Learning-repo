package habittracker.exceptions;

/**
 * Exception thrown when database operations fail.
 * Wraps SQLException with a readable message.
 */
public class DatabaseException extends Exception {
    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
