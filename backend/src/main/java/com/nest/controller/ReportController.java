package com.nest.controller;

import com.nest.dto.request.TransactionFilter;
import com.nest.dto.response.MonthlyReportResponse;
import com.nest.security.CurrentUserProvider;
import com.nest.service.ReportService;
import com.nest.validation.ValidCurrency;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;

@RestController
@RequestMapping("/api/reports")
@Tag(name = "Reports")
public class ReportController {

    private static final MediaType CSV = new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8);

    private final ReportService reportService;
    private final CurrentUserProvider currentUser;

    public ReportController(ReportService reportService, CurrentUserProvider currentUser) {
        this.reportService = reportService;
        this.currentUser = currentUser;
    }

    @GetMapping("/monthly")
    @Operation(summary = "Monthly report: totals, categories, budget, goals, largest expenses")
    public MonthlyReportResponse monthly(@RequestParam @Min(2000) @Max(2100) int year,
                                         @RequestParam @Min(1) @Max(12) int month,
                                         @RequestParam(required = false) @ValidCurrency String currency) {
        return reportService.monthly(currentUser.currentUserId(), YearMonth.of(year, month), currency);
    }

    @GetMapping("/monthly.csv")
    @Operation(summary = "Monthly report as CSV")
    public ResponseEntity<byte[]> monthlyCsv(@RequestParam @Min(2000) @Max(2100) int year,
                                             @RequestParam @Min(1) @Max(12) int month,
                                             @RequestParam(required = false) @ValidCurrency String currency) {
        byte[] body = reportService.monthlyCsv(currentUser.currentUserId(), YearMonth.of(year, month), currency);
        return attachment(body, "nest-report-%d-%02d.csv".formatted(year, month));
    }

    @GetMapping("/transactions.csv")
    @Operation(summary = "Export transactions as CSV", description = "Same filters as GET /api/transactions. Oldest first, up to 50,000 rows.")
    public ResponseEntity<byte[]> transactionsCsv(@Valid @ModelAttribute TransactionFilter filter) {
        byte[] body = reportService.transactionsCsv(currentUser.currentUserId(), filter);
        String range = (filter.from() == null ? "start" : filter.from().toString()) + "_" + (filter.to() == null ? "today" : filter.to().toString());
        return attachment(body, "nest-transactions-%s.csv".formatted(range));
    }

    private static ResponseEntity<byte[]> attachment(byte[] body, String filename) {
        return ResponseEntity.ok()
                .contentType(CSV)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(body);
    }
}
