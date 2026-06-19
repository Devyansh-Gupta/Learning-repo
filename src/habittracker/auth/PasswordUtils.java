package habittracker.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for password hashing and verification.
 * Uses SHA-256 algorithm for secure hashing.
 */
public final class PasswordUtils {

    private PasswordUtils() {
        // Utility class - never instantiated
    }

    /**
     * Hashes a password using SHA-256.
     * @param input The password to hash
     * @return Hex string representation of the hash
     */
    public static String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Verifies a password against a stored hash.
     * @param input The password to verify
     * @param storedHash The stored hash to compare against
     * @return true if password matches, false otherwise
     */
    public static boolean verify(String input, String storedHash) {
        if (input == null || storedHash == null) {
            return false;
        }
        return hash(input).equals(storedHash);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
