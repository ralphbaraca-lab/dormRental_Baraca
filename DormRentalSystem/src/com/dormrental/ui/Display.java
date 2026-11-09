package com.dormrental.ui;

/** Small helpers so every screen prints in the same style. */
public final class Display {
    private static final String RULE = "================================================================";

    private Display() {
    }

    public static void header(String title) {
        System.out.println();
        System.out.println(RULE);
        System.out.println("  " + title);
        System.out.println(RULE);
    }

    public static void subheader(String title) {
        System.out.println();
        System.out.println("-- " + title + " --");
    }

    public static void println(String text) {
        System.out.println(text);
    }

    /** Prints text that already ends with a line break (tables, reports). */
    public static void print(String text) {
        System.out.print(text);
    }

    public static void success(String message) {
        System.out.println("[OK] " + message);
    }

    public static void error(String message) {
        System.out.println("[!] " + message);
    }

    public static void info(String message) {
        System.out.println(message);
    }
}
