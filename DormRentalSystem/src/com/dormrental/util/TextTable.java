package com.dormrental.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Tiny helper that renders rows as an aligned plain-text table. */
public class TextTable {
    private static final String EMPTY_NOTICE = "  (no records)";
    private static final String NO_VALUE = "-";
    private static final String CELL_SEPARATOR = " | ";
    private static final String RULE_SEPARATOR = "-+-";

    private final String[] headers;
    private final List<String[]> rows = new ArrayList<>();

    public TextTable(String... headers) {
        Objects.requireNonNull(headers, "headers must not be null");
        if (headers.length == 0) {
            throw new IllegalArgumentException("A table needs at least one column.");
        }
        this.headers = headers.clone(); // so changing the caller's array later can't change the table
        for (String header : this.headers) {
            Objects.requireNonNull(header, "a header must not be null");
        }
    }

    /** Missing cells show as "-", and so do nulls. Extra cells beyond the number of columns are ignored. */
    public TextTable addRow(Object... cells) {
        Object[] given = cells == null ? new Object[0] : cells;
        String[] row = new String[headers.length];
        for (int i = 0; i < headers.length; i++) {
            Object cell = i < given.length ? given[i] : null;
            row[i] = cell == null ? NO_VALUE : singleLine(String.valueOf(cell));
        }
        rows.add(row);
        return this;
    }

    public int size() {
        return rows.size();
    }

    public String render() {
        String newline = System.lineSeparator();
        if (rows.isEmpty()) {
            return EMPTY_NOTICE + newline;
        }

        int[] widths = columnWidths();
        String[] rule = new String[widths.length];
        for (int i = 0; i < widths.length; i++) {
            rule[i] = "-".repeat(widths[i]);
        }

        StringBuilder sb = new StringBuilder();
        appendLine(sb, headers, widths, CELL_SEPARATOR);
        appendLine(sb, rule, widths, RULE_SEPARATOR);
        for (String[] row : rows) {
            appendLine(sb, row, widths, CELL_SEPARATOR);
        }
        return sb.toString();
    }

    private int[] columnWidths() {
        int[] widths = new int[headers.length];
        for (int i = 0; i < headers.length; i++) {
            widths[i] = headers[i].length();
        }
        for (String[] row : rows) {
            for (int i = 0; i < row.length; i++) {
                widths[i] = Math.max(widths[i], row[i].length());
            }
        }
        return widths;
    }

    /** One line: every cell but the last is padded to its column width, so the columns line up. */
    private static void appendLine(StringBuilder sb, String[] cells, int[] widths, String separator) {
        sb.append("  ");
        for (int i = 0; i < cells.length; i++) {
            sb.append(cells[i]);
            if (i < cells.length - 1) {
                sb.append(" ".repeat(widths[i] - cells[i].length()));
                sb.append(separator);
            }
        }
        sb.append(System.lineSeparator());
    }

    /** A line break or tab inside a cell (for example in a note) would push the rest of the table out of line. */
    private static String singleLine(String text) {
        return text.replaceAll("[\\r\\n\\t]+", " ");
    }
}