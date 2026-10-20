package com.nest.service.report;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.List;

/**
 * Minimal RFC 4180 CSV builder with spreadsheet-formula protection.
 *
 * <ul>
 *   <li>Every text cell is quoted; embedded quotes are doubled.</li>
 *   <li>Text that starts with {@code = + - @}, tab, or carriage return is prefixed with an apostrophe,
 *       so spreadsheet apps never execute it as a formula (OWASP "CSV Injection").</li>
 *   <li>Numbers ({@link BigDecimal}) and dates are written as plain values, so negative amounts stay
 *       numeric and sortable.</li>
 *   <li>Output is UTF-8 with a byte-order mark so Excel opens accented names correctly.</li>
 * </ul>
 */
public final class CsvWriter {

    private static final String BOM = "﻿";
    private final StringBuilder out = new StringBuilder(BOM);

    public CsvWriter row(Object... cells) {
        return row(Arrays.asList(cells)); // not List.of: empty cells are null
    }

    public CsvWriter row(List<?> cells) {
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) out.append(',');
            out.append(cell(cells.get(i)));
        }
        out.append("\r\n");
        return this;
    }

    static String cell(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof BigDecimal number) {
            return number.toPlainString();
        }
        if (value instanceof Number || value instanceof TemporalAccessor) {
            return value.toString();
        }
        String text = value.toString();
        if (!text.isEmpty() && "=+-@\t\r".indexOf(text.charAt(0)) >= 0) {
            text = "'" + text;
        }
        return '"' + text.replace("\"", "\"\"") + '"';
    }

    public byte[] toBytes() {
        return out.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String toString() {
        return out.toString();
    }
}
