package habittracker.auth;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Manages session persistence using file I/O.
 * Stores username and timestamp in session.dat for Remember Me functionality.
 */
public class SessionManager {
    private static final String SESSION_FILE = "session.dat";
    private static final long SESSION_DURATION_MS = 7 * 24 * 60 * 60 * 1000L; // 7 days

    public SessionManager() {}

    /**
     * Saves the session to file.
     * Format: username:timestamp_millis
     * @param username The username to save
     * @throws IOException if file operations fail
     */
    public void saveSession(String username) throws IOException {
        String content = username + ":" + System.currentTimeMillis();

        try (FileOutputStream fos = new FileOutputStream(SESSION_FILE);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos))) {
            writer.write(content);
            writer.flush();
        }
    }

    /**
     * Loads the session from file.
     * Validates that the session is not expired (within 7 days).
     * @return The username if valid session exists, null otherwise
     * @throws IOException if file operations fail
     */
    public String loadSession() throws IOException {
        File file = new File(SESSION_FILE);
        if (!file.exists()) {
            return null;
        }

        try (FileInputStream fis = new FileInputStream(file);
             BufferedReader reader = new BufferedReader(new InputStreamReader(fis))) {

            String line = reader.readLine();
            if (line == null || line.isBlank()) {
                return null;
            }

            String[] parts = line.split(":");
            if (parts.length != 2) {
                return null;
            }

            String username = parts[0];
            long timestamp;

            try {
                timestamp = Long.parseLong(parts[1]);
            } catch (NumberFormatException e) {
                return null;
            }

            long now = System.currentTimeMillis();
            if (now - timestamp > SESSION_DURATION_MS) {
                return null;
            }

            return username;
        }
    }

    /**
     * Clears the session by deleting the session file.
     */
    public void clearSession() {
        File file = new File(SESSION_FILE);
        if (file.exists()) {
            try {
                Files.deleteIfExists(Paths.get(SESSION_FILE));
            } catch (IOException e) {
                // Best effort - ignore errors when clearing
            }
        }
    }
}
