package com.dormrental.util;

import com.dormrental.exception.ValidationException;

import java.math.BigDecimal;
import java.util.regex.Pattern;

public final class Validator {
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE = Pattern.compile("^\\+?[0-9 \\-]{7,20}$");

    private Validator() {
    }

    public static String requireNotBlank(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(field + " is required.");
        }
        return value.trim();
    }

    public static String requireEmail(String value) {
        String email = requireNotBlank(value, "Email");
        if (!EMAIL.matcher(email).matches()) {
            throw new ValidationException("Invalid email address: " + email);
        }
        return email;
    }

    public static String requirePhone(String value) {
        String phone = requireNotBlank(value, "Phone number");
        if (!PHONE.matcher(phone).matches()) {
            throw new ValidationException("Invalid phone number: " + phone + " (digits, spaces, dashes, optional leading +).");
        }
        return phone;
    }

    public static BigDecimal requirePositive(BigDecimal value, String field) {
        if (value == null || value.signum() <= 0) {
            throw new ValidationException(field + " must be greater than zero.");
        }
        return value;
    }

    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
