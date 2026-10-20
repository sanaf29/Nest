package com.nest.controller;

import com.nest.dto.request.RecurringRequest;
import com.nest.dto.request.UpdateRecurringRequest;
import com.nest.dto.response.RecurringResponse;
import com.nest.dto.response.RecurringRunResponse;
import com.nest.security.CurrentUserProvider;
import com.nest.service.RecurringTransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/recurring-transactions")
@Tag(name = "Recurring Transactions")
public class RecurringTransactionController {

    private final RecurringTransactionService service;
    private final CurrentUserProvider currentUser;

    public RecurringTransactionController(RecurringTransactionService service, CurrentUserProvider currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @PostMapping
    @Operation(summary = "Schedule recurring income or an expense",
            description = "Occurrences on or before today are generated immediately; later ones by the hourly job. "
                    + "Each occurrence is created exactly once.")
    public ResponseEntity<RecurringResponse> create(@Valid @RequestBody RecurringRequest request) {
        RecurringResponse created = service.create(currentUser.currentUserId(), request);
        return ResponseEntity.created(URI.create("/api/recurring-transactions/" + created.id())).body(created);
    }

    @GetMapping
    @Operation(summary = "List my recurring transactions with their next dates")
    public List<RecurringResponse> list() {
        return service.list(currentUser.currentUserId());
    }

    @GetMapping("/{id}")
    public RecurringResponse get(@PathVariable long id) {
        return service.get(currentUser.currentUserId(), id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Change amount, account, category, description, end date, or reminder (future occurrences only)")
    public RecurringResponse update(@PathVariable long id, @Valid @RequestBody UpdateRecurringRequest request) {
        return service.update(currentUser.currentUserId(), id, request);
    }

    @PostMapping("/{id}/pause")
    public RecurringResponse pause(@PathVariable long id) {
        return service.pause(currentUser.currentUserId(), id);
    }

    @PostMapping("/{id}/resume")
    @Operation(summary = "Resume from the next scheduled date on or after today", description = "Occurrences missed while paused are skipped.")
    public RecurringResponse resume(@PathVariable long id) {
        return service.resume(currentUser.currentUserId(), id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "End a recurring transaction", description = "Permanent. Generated transactions are kept.")
    public ResponseEntity<Void> end(@PathVariable long id) {
        service.end(currentUser.currentUserId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/run")
    @Operation(summary = "Generate my due occurrences now", description = "Safe to call repeatedly: nothing is generated twice.")
    public RecurringRunResponse runNow() {
        return service.runNow(currentUser.currentUserId());
    }
}
