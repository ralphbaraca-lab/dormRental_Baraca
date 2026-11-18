package com.dormrental.ui;

import com.dormrental.exception.InputClosedException;
import com.dormrental.exception.ValidationException;
import com.dormrental.util.DateUtil;
import com.dormrental.util.Validator;
import java.io.Console;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/** All keyboard input goes through here. Each read method keeps asking until the answer is valid. */
public class InputHelper {
    /** A leading peso sign or "PHP" is allowed, so "PHP 2,500.00" (how amounts are printed) can be typed back in. */
    private static final Pattern CURRENCY_PREFIX = Pattern.compile("^(?:₱|PHP)\\s*", Pattern.CASE_INSENSITIVE);

    /** Plain digits with an optional decimal part. This also rules out forms like "1e999999999" that BigDecimal accepts. */
    private static final Pattern PLAIN_NUMBER = Pattern.compile("[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+");

    private final Scanner scanner;

    public InputHelper(Scanner scanner) {
        this.scanner = Objects.requireNonNull(scanner, "scanner must not be null");
    }

    private String nextLine() {
        try {
            return scanner.nextLine().trim();
        } catch (NoSuchElementException | IllegalStateException e) {
            throw new InputClosedException();
        }
    }

    /** Asks again, showing the problem, until {@code reader} returns a value instead of throwing a ValidationException. */
    private static <T> T readValidated(Supplier<T> reader) {
        while (true) {
            try {
                return reader.get();
            } catch (ValidationException e) {
                Display.error(e.getMessage());
            }
        }
    }

    /** Free text; an empty answer is allowed. */
    public String readLine(String prompt) {
        System.out.print(prompt + ": ");
        return nextLine();
    }

    /**
     * Like {@link #readLine} but the typing is hidden when a real terminal is available.
     * In an IDE or with redirected input there is no way to hide it, so it is shown as normal.
     * The answer is trimmed, to match every other prompt in the app.
     */
    public String readPassword(String prompt) {
        Console console = System.console();
        if (console == null) {
            return readLine(prompt);
        }
        char[] typed = console.readPassword("%s: ", prompt);
        if (typed == null) {
            throw new InputClosedException();
        }
        try {
            return new String(typed).trim();
        } finally {
            Arrays.fill(typed, ' ');
        }
    }

    public String readRequired(String prompt) {
        while (true) {
            String line = readLine(prompt);
            if (!line.isEmpty()) {
                return line;
            }
            Display.error("This field is required.");
        }
    }

    /** IDs are stored in upper case (e.g. TNT-0001), so typing "tnt-0001" works too. */
    public String readId(String prompt) {
        return readRequired(prompt).toUpperCase(Locale.ROOT);
    }

    /** Like readId but a blank answer returns null. */
    public String readOptionalId(String prompt) {
        String line = readLine(prompt);
        return line.isEmpty() ? null : line.toUpperCase(Locale.ROOT);
    }

    public String readEmail(String prompt) {
        return readValidated(() -> Validator.requireEmail(readLine(prompt)));
    }

    public String readPhone(String prompt) {
        return readValidated(() -> Validator.requirePhone(readLine(prompt)));
    }

    /** A phone number that may be left blank. */
    public String readOptionalPhone(String prompt) {
        return readValidated(() -> {
            String line = readLine(prompt + " (optional)");
            return line.isEmpty() ? "" : Validator.requirePhone(line);
        });
    }

    public int readInt(String prompt, int min, int max) {
        if (min > max) {
            throw new IllegalArgumentException("min (" + min + ") must not exceed max (" + max + ")");
        }
        while (true) {
            String line = readLine(prompt + " [" + min + "-" + max + "]");
            try {
                int value = Integer.parseInt(line);
                if (value >= min && value <= max) {
                    return value;
                }
            } catch (NumberFormatException ignored) {
                // fall through to the error message
            }
            Display.error("Enter a whole number from " + min + " to " + max + ".");
        }
    }

    /** An amount greater than zero. */
    public BigDecimal readMoney(String prompt) {
        while (true) {
            BigDecimal value = parseMoney(readLine(prompt));
            if (value != null && value.signum() > 0) {
                return value;
            }
            Display.error("Enter an amount greater than zero, e.g. 2500 or 2,500.50.");
        }
    }

    /** An amount of zero or more; a blank answer returns null. */
    public BigDecimal readOptionalMoney(String prompt) {
        while (true) {
            String line = readLine(prompt);
            if (line.isEmpty()) {
                return null;
            }
            BigDecimal value = parseMoney(line);
            if (value != null && value.signum() >= 0) {
                return value;
            }
            Display.error("Enter an amount (0 or more), or leave it blank.");
        }
    }

    /** Returns the amount rounded to centavos, or null if the text isn't a plain amount. */
    private static BigDecimal parseMoney(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String cleaned = CURRENCY_PREFIX.matcher(text.replace(",", "").trim()).replaceFirst("").trim();
        if (!PLAIN_NUMBER.matcher(cleaned).matches()) {
            return null;
        }
        return new BigDecimal(cleaned).setScale(2, RoundingMode.HALF_UP);
    }

    public LocalDate readDate(String prompt) {
        return readValidated(() -> DateUtil.parse(readLine(prompt + " (yyyy-MM-dd)")));
    }

    public LocalDate readDateOrDefault(String prompt, LocalDate defaultValue) {
        String defaultLabel = (defaultValue != null) ? DateUtil.format(defaultValue) : "none";
        return readValidated(() -> {
            String line = readLine(prompt + " (yyyy-MM-dd, blank = " + defaultLabel + ")");
            return line.isEmpty() ? defaultValue : DateUtil.parse(line);
        });
    }

    /** Shows a numbered list and returns the option the user picks. */
    public <T> T readOption(String prompt, T[] options) {
        validateOptions(options);
        printOptions(options);
        int choice = readInt(prompt, 1, options.length);
        return options[choice - 1];
    }

    public <E extends Enum<E>> E readEnum(String prompt, Class<E> type) {
        Objects.requireNonNull(type, "type must not be null");
        return readOption(prompt, type.getEnumConstants());
    }

    /** Lets the user pick any number of enum values, e.g. "1,3,4". Blank means none. */
    public <E extends Enum<E>> Set<E> readEnumSet(String prompt, Class<E> type) {
        Objects.requireNonNull(type, "type must not be null");
        E[] values = type.getEnumConstants();
        validateOptions(values);
        printOptions(values);

        while (true) {
            String line = readLine(prompt + " (numbers separated by commas, blank for none)");
            if (line.isEmpty()) {
                return EnumSet.noneOf(type);
            }
            Set<E> picked = parsePicks(line, type, values);
            if (picked != null) {
                return picked;
            }
            Display.error("Enter numbers between 1 and " + values.length + ", separated by commas.");
        }
    }

    /** Turns "1, 3,4" into the matching values, or returns null if any part is missing, not a number or out of range. */
    private static <E extends Enum<E>> Set<E> parsePicks(String line, Class<E> type, E[] values) {
        Set<E> result = EnumSet.noneOf(type);
        for (String token : line.split(",", -1)) { // -1 keeps empty pieces, so "1,,2" and "1," are rejected
            try {
                int number = Integer.parseInt(token.trim());
                if (number < 1 || number > values.length) {
                    return null;
                }
                result.add(values[number - 1]);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return result;
    }

    public boolean confirm(String prompt) {
        while (true) {
            String answer = readLine(prompt + " (y/n)").toLowerCase(Locale.ROOT);
            if (answer.equals("y") || answer.equals("yes")) {
                return true;
            }
            if (answer.equals("n") || answer.equals("no")) {
                return false;
            }
            Display.error("Please answer y or n.");
        }
    }

    public void pause() {
        System.out.print(System.lineSeparator() + "Press Enter to continue...");
        nextLine();
    }

    private static <T> void validateOptions(T[] options) {
        if (options == null || options.length == 0) {
            throw new IllegalArgumentException("options must not be null or empty");
        }
    }

    private static <T> void printOptions(T[] options) {
        for (int i = 0; i < options.length; i++) {
            System.out.printf("  %d. %s%n", i + 1, options[i]);
        }
    }
}