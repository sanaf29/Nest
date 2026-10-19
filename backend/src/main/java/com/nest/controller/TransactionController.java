package com.nest.controller;

import com.nest.dto.request.TransactionFilter;
import com.nest.dto.request.TransactionRequest;
import com.nest.dto.response.PageResponse;
import com.nest.dto.response.TransactionResponse;
import com.nest.security.CurrentUserProvider;
import com.nest.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/transactions")
@Tag(name = "Transactions")
public class TransactionController {

    /** Only these fields may be sorted on. Arbitrary property names would otherwise reach the query. */
    private static final String SORT_PATTERN = "^(transactionDate|amount|createdAt)(,(asc|desc))?$";

    private final TransactionService transactionService;
    private final CurrentUserProvider currentUser;

    public TransactionController(TransactionService transactionService, CurrentUserProvider currentUser) {
        this.transactionService = transactionService;
        this.currentUser = currentUser;
    }

    @PostMapping
    @Operation(summary = "Record an income or expense",
            description = "201 created; 400 invalid input; 404 account/category not found or not yours; "
                    + "422 business rule (closed account, archived or mismatched category, currency mismatch, too many decimals).")
    public ResponseEntity<TransactionResponse> create(@Valid @RequestBody TransactionRequest request) {
        TransactionResponse created = transactionService.create(currentUser.currentUserId(), request);
        return ResponseEntity.created(URI.create("/api/transactions/" + created.id())).body(created);
    }

    @GetMapping
    @Operation(summary = "Search my transactions",
            description = "Filters are combined with AND. Default sort: newest first. Page size 1–100.")
    public PageResponse<TransactionResponse> search(
            @Valid @ModelAttribute TransactionFilter filter,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @Parameter(description = "transactionDate | amount | createdAt, optionally ',asc' or ',desc'")
            @RequestParam(defaultValue = "transactionDate,desc") @Pattern(regexp = SORT_PATTERN) String sort) {
        return transactionService.search(currentUser.currentUserId(), filter, PageRequest.of(page, size, toSort(sort)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one of my transactions")
    public TransactionResponse get(@PathVariable long id) {
        return transactionService.get(currentUser.currentUserId(), id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace a transaction", description = "All rules are re-checked. 422 if its account is closed.")
    public TransactionResponse update(@PathVariable long id, @Valid @RequestBody TransactionRequest request) {
        return transactionService.update(currentUser.currentUserId(), id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a transaction", description = "204 on success; 422 if its account is closed.")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        transactionService.delete(currentUser.currentUserId(), id);
        return ResponseEntity.noContent().build();
    }

    /** Adds id as a tie-breaker so pages are stable when many rows share a date or amount. */
    private static Sort toSort(String sort) {
        String[] parts = sort.split(",");
        Sort.Direction direction = parts.length > 1 && parts[1].equals("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, parts[0]).and(Sort.by(direction, "id"));
    }
}
