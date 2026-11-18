
package com.dormrental.ui;

/**
 * Utility class for displaying consistent console output
 * throughout the Dorm Rental System.
 */
public final class Display {

    private static final int RULE_WIDTH = 64;

    private static final String RULE = "=".repeat(RULE_WIDTH);
    private static final String SUB_RULE = "-".repeat(40);

    private static final String OK_PREFIX = "[OK] ";
    private static final String ERROR_PREFIX = "[!] ";
    private static final String WARNING_PREFIX = "[WARNING] ";
    private static final String INFO_PREFIX = "[INFO] ";

    // Prevent object creation
    private Display() {
        throw new AssertionError("Utility class cannot be instantiated.");
    }

    // Main section header
    public static void header(String title) {
        System.out.println();
        System.out.println(RULE);
        System.out.println("  " + safe(title));
        System.out.println(RULE);
    }

    // Subsection header
    public static void subheader(String title) {
        System.out.println();
        System.out.println("-- " + safe(title) + " --");
        System.out.println(SUB_RULE);
    }

    // Standard output
    public static void println(String text) {
        System.out.println(safe(text));
    }

    // Prints text and ensures a trailing line break
    public static void print(String text) {
        String safeText = safe(text);

        if (safeText.isEmpty()) {
            return;
        }

        System.out.print(safeText);

        if (!safeText.endsWith("\n")) {
            System.out.println();
        }
    }

    // Success message
    public static void success(String message) {
        System.out.println(OK_PREFIX + safe(message));
    }

    // Error message
    public static void error(String message) {
        System.out.println(ERROR_PREFIX + safe(message));
    }

    // Warning message
    public static void warning(String message) {
        System.out.println(WARNING_PREFIX + safe(message));
    }

    // Informational message
    public static void info(String message) {
        System.out.println(INFO_PREFIX + safe(message));
    }

    // Prints a blank line
    public static void newLine() {
        System.out.println();
    }

    // Prints a horizontal divider
    public static void divider() {
        System.out.println(SUB_RULE);
    }

    /**
     * Sanitizes console output.
     *
     * Null values become empty strings.
     * Terminal control characters are replaced with '?'.
     * Newlines and tabs are preserved for formatting.
     */
    private static String safe(String value) {

        if (value == null) {
            return "";
        }

        StringBuilder cleaned = null;

        for (int i = 0; i < value.length(); i++) {

            char c = value.charAt(i);

            boolean invalidControl =
                    Character.isISOControl(c)
                    && c != '\n'
                    && c != '\r'
                    && c != '\t';

            if (invalidControl) {

                if (cleaned == null) {
                    cleaned = new StringBuilder(value);
                }

                cleaned.setCharAt(i, '?');
            }
        }

        return cleaned == null
                ? value
                : cleaned.toString();
    }
}
