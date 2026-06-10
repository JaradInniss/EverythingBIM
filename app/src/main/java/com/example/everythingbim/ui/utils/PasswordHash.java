package com.example.everythingbim.ui.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for password hashing using SHA-256.
 * Note: For production, consider using BCrypt or Argon2 instead.
 */
public class PasswordHash {

    /**
     * Hash a password using SHA-256.
     * @param password the plain text password
     * @return the hashed password as a hex string
     */
    public static String hash(String password) {
        if (password == null || password.isEmpty()) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    /**
     * Verify a plain text password against a hashed password.
     * @param plainPassword the plain text password to verify
     * @param hashedPassword the stored hashed password
     * @return true if the password matches
     */
    public static boolean verify(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) {
            return false;
        }
        String hashedInput = hash(plainPassword);
        return hashedPassword.equals(hashedInput);
    }
}