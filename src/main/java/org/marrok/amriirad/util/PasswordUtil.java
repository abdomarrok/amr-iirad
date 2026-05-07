package org.marrok.amriirad.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility for hashing and verifying passwords using SHA-256.
 * Reused pattern from GstockDz.
 */
public class PasswordUtil {
    private static final Logger logger = LogManager.getLogger(PasswordUtil.class);

    /**
     * Hash the password using SHA-256.
     * @param password Plain text password
     * @return Hashed password in hex format
     */
    public static String hashPassword(String password) {
        if (password == null) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            logger.error("Error hashing password", e);
            throw new RuntimeException("Error hashing password", e);
        }
    }

    /**
     * Compare a plain password with a hashed one.
     * @param plainPassword Plain text password from user input
     * @param hashedPassword Hashed password from database
     * @return true if matches
     */
    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) return false;
        return hashPassword(plainPassword).equals(hashedPassword);
    }
}
