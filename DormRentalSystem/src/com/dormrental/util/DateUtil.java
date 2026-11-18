
package com.dormrental.util;

import com.dormrental.exception.ValidationException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;

public final class DateUtil {

    private static final String DATE_PATTERN = "yyyy-MM-dd";
    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm";

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("uuuu-MM-dd", Locale.ROOT)
                    .withResolverStyle(ResolverStyle.STRICT);

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm", Locale.ROOT);

    private DateUtil() {
        throw new AssertionError("Utility class cannot be instantiated.");
    }

    /**
     * Parses a date using the yyyy-MM-dd format.
     *
     * @param text input date
     * @return parsed LocalDate
     * @throws ValidationException if the input is missing or invalid
     */
    public static LocalDate parse(String text) {

        if (text == null || text.isBlank()) {
            throw new ValidationException(
                    "Date is required. Use " + DATE_PATTERN + "."
            );
        }

        String cleanText = text.trim();

        try {
            return LocalDate.parse(cleanText, DATE_FORMATTER);

        } catch (DateTimeParseException e) {
            throw new ValidationException(
                    "Invalid date. Use yyyy-MM-dd, for example 2026-11-09."
            );
        }
    }

    /**
     * Formats a LocalDate into yyyy-MM-dd.
     *
     * @param date date to format
     * @return formatted date or "-" when null
     */
    public static String format(LocalDate date) {

        if (date == null) {
            return "-";
        }

        return DATE_FORMATTER.format(date);
    }

    /**
     * Formats a LocalDateTime into yyyy-MM-dd HH:mm.
     *
     * @param time date and time to format
     * @return formatted date and time or "-" when null
     */
    public static String format(LocalDateTime time) {

        if (time == null) {
            return "-";
        }

        return DATE_TIME_FORMATTER.format(time);
    }
}
