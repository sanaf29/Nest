package com.nest.service.report;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class CsvWriterTest {

    @Test
    void quotesTextAndDoublesEmbeddedQuotes() {
        assertThat(CsvWriter.cell("Joe's \"best\" pizza, downtown")).isEqualTo("\"Joe's \"\"best\"\" pizza, downtown\"");
        assertThat(CsvWriter.cell("line\nbreak")).isEqualTo("\"line\nbreak\"");
    }

    @Test
    void neutralisesFormulaInjectionInText() {
        assertThat(CsvWriter.cell("=HYPERLINK(\"http://evil\")")).isEqualTo("\"'=HYPERLINK(\"\"http://evil\"\")\"");
        assertThat(CsvWriter.cell("+SUM(A1)")).startsWith("\"'+");
        assertThat(CsvWriter.cell("-2+3")).startsWith("\"'-");
        assertThat(CsvWriter.cell("@cmd")).startsWith("\"'@");
        assertThat(CsvWriter.cell("\tTAB")).startsWith("\"'\t");
        assertThat(CsvWriter.cell("Normal")).isEqualTo("\"Normal\"");
    }

    @Test
    void numbersAndDatesStayPlainSoNegativesRemainNumeric() {
        assertThat(CsvWriter.cell(new BigDecimal("-84.37"))).isEqualTo("-84.37");
        assertThat(CsvWriter.cell(new BigDecimal("1E+3"))).isEqualTo("1000");
        assertThat(CsvWriter.cell(LocalDate.of(2026, 10, 19))).isEqualTo("2026-10-19");
        assertThat(CsvWriter.cell(42L)).isEqualTo("42");
        assertThat(CsvWriter.cell(null)).isEmpty();
    }

    @Test
    void writesCrlfRowsWithAUtf8Bom() {
        byte[] bytes = new CsvWriter().row("Name", new BigDecimal("1.50")).row("Café", null).toBytes();
        String text = new String(bytes, StandardCharsets.UTF_8);

        assertThat(text).isEqualTo("﻿\"Name\",1.50\r\n\"Café\",\r\n");
    }
}
