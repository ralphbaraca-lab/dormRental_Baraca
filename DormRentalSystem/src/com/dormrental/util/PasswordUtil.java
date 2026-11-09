package com.dormrental.util;

import com.dormrental.exception.DormRentalException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/** Salted SHA-256 password hashing. Stored form is "salt$hash". */
public final class PasswordUtil {
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {
    }

    public static String hash(String password) {
        String salt = randomSalt();
        return salt + "$" + digest(salt, password);
    }

    public static boolean verify(String password, String stored) {
        if (stored == null) {
            return false;
        }
        String[] parts = stored.split("\\$", 2);
        if (parts.length != 2) {
            return false;
        }
        String candidate = digest(parts[0], password);
        return MessageDigest.isEqual(
                candidate.getBytes(StandardCharsets.UTF_8),
                parts[1].getBytes(StandardCharsets.UTF_8));
    }

    private static String randomSalt() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return toHex(bytes);
    }

    private static String digest(String salt, String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes(StandardCharsets.UTF_8));
            return toHex(md.digest(password.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new DormRentalException("SHA-256 is not available on this JVM.", e);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
