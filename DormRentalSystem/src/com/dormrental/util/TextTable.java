package com.dormrental.util;

import java.util.ArrayList;
import java.util.List;

/** Tiny helper that renders rows as an aligned plain-text table. */
public class TextTable {
    private final String[] headers;
    private final List<String[]> rows = new ArrayList<>();

    public TextTable(String... headers) {
        this.headers = headers;
    }

    public TextTable addRow(Object... cells) {
        String[] row = new String[headers.length];
        for (int i = 0; i < headers.length; i++) {
            Object cell = i < cells.length ? cells[i] : null;
            row[i] = cell == null ? "-" : String.valueOf(cell);
        }
        rows.add(row);
        return this;
    }

    public int size() {
        return rows.size();
    }

    public String render() {
        if (rows.isEmpty()) {
            return "  (no records)" + System.lineSeparator();
        }
        int[] widths = new int[headers.length];
        for (int i = 0; i < headers.length; i++) {
            widths[i] = headers[i].length();
        }
        for (String[] row : rows) {
            for (int i = 0; i < row.length; i++) {
                widths[i] = Math.max(widths[i], row[i].length());
            }
        }

        StringBuilder sb = new StringBuilder();
        appendRow(sb, headers, widths);
        sb.append("  ");
        for (int i = 0; i < widths.length; i++) {
            sb.append(repeat('-', widths[i]));
            if (i < widths.length - 1) {
                sb.append("-+-");
            }
        }
        sb.append(System.lineSeparator());
        for (String[] row : rows) {
            appendRow(sb, row, widths);
        }
        return sb.toString();
    }

    private void appendRow(StringBuilder sb, String[] cells, int[] widths) {
        sb.append("  ");
        for (int i = 0; i < cells.length; i++) {
            sb.append(cells[i]);
            if (i < cells.length - 1) {
                sb.append(repeat(' ', widths[i] - cells[i].length()));
                sb.append(" | ");
            }
        }
        sb.append(System.lineSeparator());
    }

    private static String repeat(char c, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(c);
        }
        return sb.toString();
    }
}
