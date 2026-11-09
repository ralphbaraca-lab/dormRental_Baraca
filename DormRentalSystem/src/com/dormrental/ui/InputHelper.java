package com.dormrental.ui;

import com.dormrental.exception.InputClosedException;
import com.dormrental.exception.ValidationException;
import com.dormrental.util.DateUtil;
import com.dormrental.util.Validator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Set;

/** All keyboard input goes through here. Each read method keeps asking until the answer is valid. */
public class InputHelper {
    private final Scanner scanner;

    public InputHelper(Scanner scanner) {
        this.scanner = scanner;
    }

    private String nextLine() {
        try {
            return scanner.nextLine().trim();
        } catch (NoSuchElementException e) {
            throw new InputClosedException();
        }
    }

    /** Free text; an empty answer is allowed. */
    public String readLine(String prompt) {
        System.out.print(prompt + ": ");
        return nextLine();
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

    /** Ids are stored in upper case (e.g. TNT-0001), so typing "tnt-0001" works too. */
    public String readId(String prompt) {
        return readRequired(prompt).toUpperCase();
    }

    /** Like readId but a blank answer returns null. */
    public String readOptionalId(String prompt) {
        String line = readLine(prompt);
        return line.isEmpty() ? null : line.toUpperCase();
    }

    public String readEmail(String prompt) {
        while (true) {
            try {
                return Validator.requireEmail(readLine(prompt));
            } catch (ValidationException e) {
                Display.error(e.getMessage());
            }
        }
    }

    public String readPhone(String prompt) {
        while (true) {
            try {
                return Validator.requirePhone(readLine(prompt));
            } catch (ValidationException e) {
                Display.error(e.getMessage());
            }
        }
    }

    /** A phone number that may be left blank. */
    public String readOptionalPhone(String prompt) {
        while (true) {
            String line = readLine(prompt + " (optional)");
            if (line.isEmpty()) {
                return "";
            }
            try {
                return Validator.requirePhone(line);
            } catch (ValidationException e) {
                Display.error(e.getMessage());
            }
        }
    }

    public int readInt(String prompt, int min, int max) {
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

    private static BigDecimal parseMoney(String text) {
        try {
            return new BigDecimal(text.replace(",", "")).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public LocalDate readDate(String prompt) {
        while (true) {
            try {
                return DateUtil.parse(readLine(prompt + " (yyyy-MM-dd)"));
            } catch (ValidationException e) {
                Display.error(e.getMessage());
            }
        }
    }

    public LocalDate readDateOrDefault(String prompt, LocalDate defaultValue) {
        while (true) {
            String line = readLine(prompt + " (yyyy-MM-dd, blank = " + DateUtil.format(defaultValue) + ")");
            if (line.isEmpty()) {
                return defaultValue;
            }
            try {
                return DateUtil.parse(line);
            } catch (ValidationException e) {
                Display.error(e.getMessage());
            }
        }
    }

    /** Shows a numbered list and returns the option the user picks. */
    public <T> T readOption(String prompt, T[] options) {
        for (int i = 0; i < options.length; i++) {
            System.out.printf("  %d. %s%n", i + 1, options[i]);
        }
        int choice = readInt(prompt, 1, options.length);
        return options[choice - 1];
    }

    public <E extends Enum<E>> E readEnum(String prompt, Class<E> type) {
        return readOption(prompt, type.getEnumConstants());
    }

    /** Lets the user pick any number of enum values, e.g. "1,3,4". Blank means none. */
    public <E extends Enum<E>> Set<E> readEnumSet(String prompt, Class<E> type) {
        E[] values = type.getEnumConstants();
        for (int i = 0; i < values.length; i++) {
            System.out.printf("  %d. %s%n", i + 1, values[i]);
        }
        while (true) {
            String line = readLine(prompt + " (numbers separated by commas, blank for none)");
            Set<E> result = EnumSet.noneOf(type);
            boolean valid = true;
            if (!line.isEmpty()) {
                for (String token : line.split(",")) {
                    try {
                        int number = Integer.parseInt(token.trim());
                        if (number < 1 || number > values.length) {
                            valid = false;
                            break;
                        }
                        result.add(values[number - 1]);
                    } catch (NumberFormatException e) {
                        valid = false;
                        break;
                    }
                }
            }
            if (valid) {
                return result;
            }
            Display.error("Enter numbers between 1 and " + values.length + ", separated by commas.");
        }
    }

    public boolean confirm(String prompt) {
        while (true) {
            String answer = readLine(prompt + " (y/n)").toLowerCase();
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
}
