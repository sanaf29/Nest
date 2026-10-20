package com.nest.service;

import com.nest.dto.request.TransactionFilter;
import com.nest.dto.response.MonthlyReportResponse;

import java.time.YearMonth;

public interface ReportService {

    /** @param currency {@code null} for the user's preferred currency */
    MonthlyReportResponse monthly(long userId, YearMonth month, String currency);

    /** The monthly report as CSV (UTF-8 with BOM). */
    byte[] monthlyCsv(long userId, YearMonth month, String currency);

    /** The user's transactions matching the filter as CSV, oldest first (at most 50,000 rows). */
    byte[] transactionsCsv(long userId, TransactionFilter filter);
}
