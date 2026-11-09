package com.dormrental.util;

import com.dormrental.exception.ValidationException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class DateUtil {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private DateUtil() {
    }

    public static LocalDate parse(String text) {
        if (text == null) {
            throw new ValidationException("Date is required. Use yyyy-MM-dd.");
        }
        try {
            return LocalDate.parse(text.trim(), DATE);
        } catch (DateTimeParseException e) {
            throw new ValidationException("Invalid date '" + text + "'. Use yyyy-MM-dd, e.g. 2026-11-09.");
        }
    }

    public static String format(LocalDate date) {
        return date == null ? "-" : date.format(DATE);
    }

    public static String format(LocalDateTime time) {
        return time == null ? "-" : time.format(DATE_TIME);
    }
}
