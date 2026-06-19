package habittracker.auth;

import habittracker.db.DatabaseManager;
import habittracker.db.HabitRepository;
import habittracker.model.User;
import habittracker.exceptions.AuthException;
import habittracker.exceptions.DatabaseException;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service for user authentication operations.
 * Handles registration, login, logout, and password reset.
 */
public class AuthService {
    private DatabaseManager dbManager;
    private HabitRepository repository;
    private SessionManager sessionManager;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public AuthService(DatabaseManager dbManager, HabitRepository repository, SessionManager sessionManager) {
        this.dbManager = dbManager;
        this.repository = repository;
        this.sessionManager = sessionManager;
    }

    /**
     * Registers a new user.
     * @param username The username (must be unique)
     * @param password The plain text password
     * @param securityQuestion The security question for password reset
     * @param securityAnswer The security answer (will be hashed)
     * @return The created User object
     */
    public User register(String username, String password, String securityQuestion, String securityAnswer)
            throws AuthException, DatabaseException {

        if (username == null || username.isBlank()) {
            throw new AuthException("Username cannot be blank");
        }

        if (userExists(username)) {
            throw new AuthException("Username already taken");
        }

        String passwordHash = PasswordUtils.hash(password);
        String answerHash = PasswordUtils.hash(securityAnswer);
        String createdAt = LocalDateTime.now().format(DATE_FORMAT);

        String sql = "INSERT INTO users (username, password_hash, security_question, security_answer_hash, created_at) " +
                     "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, username);
            pstmt.setString(2, passwordHash);
            pstmt.setString(3, securityQuestion);
            pstmt.setString(4, answerHash);
            pstmt.setString(5, createdAt);

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new AuthException("Failed to create user");
            }

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int userId = rs.getInt(1);
                    return new User(userId, username, passwordHash, securityQuestion, answerHash, createdAt);
                }
            }

            throw new AuthException("Failed to retrieve user ID");

        } catch (SQLException e) {
            throw new DatabaseException("Database error during registration", e);
        }
    }

    /**
     * Logs in a user with username and password.
     * @param username The username
     * @param password The plain text password
     * @return The User object if credentials are valid
     */
    public User login(String username, String password) throws AuthException, DatabaseException {
        User user = getUserByUsername(username);

        if (user == null) {
            throw new AuthException("User not found");
        }

        if (!PasswordUtils.verify(password, user.getPasswordHash())) {
            throw new AuthException("Incorrect password");
        }

        try {
            sessionManager.saveSession(username);
        } catch (Exception e) {
            // Session saving is non-critical - continue with login
        }

        return user;
    }

    /**
     * Attempts automatic login using saved session.
     * @return The User object if valid session exists
     */
    public User autoLogin() throws AuthException, DatabaseException {
        try {
            String username = sessionManager.loadSession();
            if (username == null) {
                throw new AuthException("No valid session");
            }

            User user = getUserByUsername(username);
            if (user == null) {
                throw new AuthException("User not found");
            }

            return user;
        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            throw new AuthException("Auto-login failed: " + e.getMessage());
        }
    }

    /**
     * Logs out the current user.
     */
    public void logout() {
        sessionManager.clearSession();
    }

    /**
     * Resets a user's password after verifying security answer.
     */
    public void resetPassword(String username, String answer, String newPassword)
            throws AuthException, DatabaseException {

        User user = getUserByUsername(username);
        if (user == null) {
            throw new AuthException("User not found");
        }

        if (!PasswordUtils.verify(answer, user.getSecurityAnswerHash())) {
            throw new AuthException("Incorrect security answer");
        }

        String newHash = PasswordUtils.hash(newPassword);
        String sql = "UPDATE users SET password_hash = ? WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newHash);
            pstmt.setInt(2, user.getId());
            pstmt.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Failed to reset password", e);
        }
    }

    /**
     * Checks if a username already exists.
     */
    public boolean userExists(String username) throws DatabaseException {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
            return false;

        } catch (SQLException e) {
            throw new DatabaseException("Failed to check user existence", e);
        }
    }

    /**
     * Gets a user by username.
     */
    public User getUserByUsername(String username) throws DatabaseException {
        String sql = "SELECT * FROM users WHERE username = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToUser(rs);
                }
            }
            return null;

        } catch (SQLException e) {
            throw new DatabaseException("Failed to get user", e);
        }
    }

    private User mapRowToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setSecurityQuestion(rs.getString("security_question"));
        user.setSecurityAnswerHash(rs.getString("security_answer_hash"));
        user.setCreatedAt(rs.getString("created_at"));
        return user;
    }
}
