package com.dormrental.util;

import com.dormrental.exception.ValidationException;
import java.math.BigDecimal;
import java.util.regex.Pattern;

/** Input checks shared by the services. Each {@code require*} method returns the cleaned value. */
public final class Validator {
    private static final int EMAIL_MAX_LENGTH = 254;
    private static final int EMAIL_LOCAL_MAX_LENGTH = 64;
    private static final int PHONE_MIN_DIGITS = 7;
    private static final int PHONE_MAX_DIGITS = 15; // E.164 limit

    /** No leading/trailing/double dots in the name; domain labels can't start or end with a hyphen. */
    private static final Pattern EMAIL = Pattern.compile(
            "[A-Za-z0-9_%+-]+(?:\\.[A-Za-z0-9_%+-]+)*"
                    + "@(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\\.)+[A-Za-z]{2,}");

    /** Optional leading +, digits, with spaces or dashes allowed only between digits. */
    private static final Pattern PHONE = Pattern.compile("\\+?[0-9]+(?:[ \\-]+[0-9]+)*");

    private Validator() {
    }

    public static String requireNotBlank(String value, String field) {
        if (isBlank(value)) {
            throw new ValidationException(field + " is required.");
        }
        return value.trim();
    }

    public static String requireEmail(String value) {
        String email = requireNotBlank(value, "Email");
        int at = email.indexOf('@');
        boolean validLength = email.length() <= EMAIL_MAX_LENGTH && at <= EMAIL_LOCAL_MAX_LENGTH;
        if (!validLength || !EMAIL.matcher(email).matches()) {
            throw new ValidationException("Invalid email address: " + email);
        }
        return email;
    }

    public static String requirePhone(String value) {
        String phone = requireNotBlank(value, "Phone number");
        int digits = (int) phone.chars().filter(c -> c >= '0' && c <= '9').count();
        boolean validDigits = digits >= PHONE_MIN_DIGITS && digits <= PHONE_MAX_DIGITS;
        if (!validDigits || !PHONE.matcher(phone).matches()) {
            throw new ValidationException("Invalid phone number: " + phone
                    + " (" + PHONE_MIN_DIGITS + "-" + PHONE_MAX_DIGITS
                    + " digits, optional spaces or dashes between them, optional leading +).");
        }
        return phone;
    }

    public static BigDecimal requirePositive(BigDecimal value, String field) {
        if (value == null || value.signum() <= 0) {
            throw new ValidationException(field + " must be greater than zero.");
        }
        return value;
    }

    /** For amounts that may legitimately be zero, such as a waived security deposit. */
    public static BigDecimal requireNonNegative(BigDecimal value, String field) {
        if (value == null || value.signum() < 0) {
            throw new ValidationException(field + " must not be negative.");
        }
        return value;
    }

    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}