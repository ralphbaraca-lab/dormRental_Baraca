package com.dormrental.ui;

/** Small helpers so every screen prints in the same style. */
public final class Display {
    private static final int RULE_WIDTH = 64;
    private static final String RULE = "=".repeat(RULE_WIDTH);

    private Display() {
    }

    public static void header(String title) {
        String safeTitle = safe(title);
        System.out.println();
        System.out.println(RULE);
        System.out.println("  " + safeTitle);
        System.out.println(RULE);
    }

    public static void subheader(String title) {
        String safeTitle = safe(title);
        System.out.println();
        System.out.println("-- " + safeTitle + " --");
    }

    public static void println(String text) {
        System.out.println(safe(text));
    }

    /** Prints text that already ends with a line break (tables, reports). */
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
        System.out.println("[OK] " + safe(message));
    }

    public static void error(String message) {
        System.out.println("[!] " + safe(message));
    }

    public static void info(String message) {
        System.out.println(safe(message));
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}