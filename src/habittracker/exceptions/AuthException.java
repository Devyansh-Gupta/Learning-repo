package habittracker.exceptions;

/**
 * Exception thrown for authentication-related errors.
 * Includes bad login, expired session, wrong security answer, etc.
 */
public class AuthException extends Exception {
    public AuthException(String message) {
        super(message);
    }
}
