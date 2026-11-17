package com.dormrental.util;

import com.dormrental.exception.DormRentalException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Password hashing with PBKDF2-HMAC-SHA256.
 *
 * <p>New hashes are stored as {@code pbkdf2_sha256$iterations$salt$hash} (salt and hash in Base64).
 * Hashes created by the old version of this class ({@code salt$hash}, one round of SHA-256) still
 * verify, so existing accounts keep working; use {@link #needsRehash(String)} to upgrade them on login.
 */
public final class PasswordUtil {
    private static final String SCHEME = "pbkdf2_sha256";
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 600_000;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BITS = 256;
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {
    }

    public static String hash(String password) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] hash = pbkdf2(password, salt, ITERATIONS, HASH_BITS);
        return String.join("$", SCHEME, String.valueOf(ITERATIONS), encode(salt), encode(hash));
    }

    public static boolean verify(String password, String stored) {
        if (password == null || stored == null) {
            return false;
        }
        if (stored.startsWith(SCHEME + "$")) {
            return verifyPbkdf2(password, stored);
        }
        return verifyLegacy(password, stored);
    }

    /**
     * True if the stored hash is in the old format or uses fewer iterations than the current setting.
     * Call after a successful {@link #verify} and save {@link #hash(String)} of the same password if so.
     */
    public static boolean needsRehash(String stored) {
        if (stored == null || !stored.startsWith(SCHEME + "$")) {
            return true;
        }
        String[] parts = stored.split("\\$", 4);
        try {
            return parts.length != 4 || Integer.parseInt(parts[1]) < ITERATIONS;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    // ------------------------------------------------------------- PBKDF2

    private static boolean verifyPbkdf2(String password, String stored) {
        String[] parts = stored.split("\\$", 4);
        if (parts.length != 4) {
            return false;
        }
        try {
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] actual = pbkdf2(password, salt, iterations, expected.length * 8);
            return MessageDigest.isEqual(actual, expected);
        } catch (IllegalArgumentException e) {
            // Malformed number, Base64, or parameters in the stored value.
            return false;
        }
    }

    private static byte[] pbkdf2(String password, byte[] salt, int iterations, int keyBits) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, keyBits);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new DormRentalException("Password hashing is not available on this JVM.", e);
        } finally {
            spec.clearPassword();
        }
    }

    private static String encode(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    // ------------------------------------------- legacy "salt$hash" format

    private static boolean verifyLegacy(String password, String stored) {
        String[] parts = stored.split("\\$", 2);
        if (parts.length != 2) {
            return false;
        }
        String candidate = legacyDigest(parts[0], password);
        return MessageDigest.isEqual(
                candidate.getBytes(StandardCharsets.UTF_8),
                parts[1].getBytes(StandardCharsets.UTF_8));
    }

    private static String legacyDigest(String salt, String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes(StandardCharsets.UTF_8));
            return toHex(md.digest(password.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new DormRentalException("SHA-256 is not available on this JVM.", e);
        }
    }

    private static String toHex(byte[] bytes) {
        char[] out = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xff;
            out[i * 2] = HEX[v >>> 4];
            out[i * 2 + 1] = HEX[v & 0x0f];
        }
        return new String(out);
    }
}