package com.dormrental.ui;

/** Small helpers so every screen prints in the same style. */
public final class Display {
    private static final int RULE_WIDTH = 64;
    private static final String RULE = "=".repeat(RULE_WIDTH);

    private static final String OK_PREFIX = "[OK] ";
    private static final String ERROR_PREFIX = "[!] ";

    private Display() {
    }

    public static void header(String title) {
        System.out.println();
        System.out.println(RULE);
        System.out.println("  " + safe(title));
        System.out.println(RULE);
    }

    public static void subheader(String title) {
        System.out.println();
        System.out.println("-- " + safe(title) + " --");
    }

    public static void println(String text) {
        System.out.println(safe(text));
    }

    /** Prints text that already ends with a line break (tables, reports). Adds one if it is missing. */
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

    public static void success(String message) {
        System.out.println(OK_PREFIX + safe(message));
    }

    public static void error(String message) {
        System.out.println(ERROR_PREFIX + safe(message));
    }

    public static void info(String message) {
        println(message);
    }

    /**
     * Returns "" for null, and replaces control characters (other than line breaks and tabs) with '?'.
     * Names and notes typed by users end up here, and a raw escape character in one could otherwise
     * change colours, move the cursor or clear the screen of whoever views it.
     */
    private static String safe(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder cleaned = null; // only built if something needs replacing, so normal text costs nothing extra
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isISOControl(c) && c != '\n' && c != '\r' && c != '\t') {
                if (cleaned == null) {
                    cleaned = new StringBuilder(value);
                }
                cleaned.setCharAt(i, '?');
            }
        }
        return cleaned == null ? value : cleaned.toString();
    }
}