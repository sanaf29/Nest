package com.nest.controller;

import com.nest.dto.request.SharedExpenseRequest;
import com.nest.dto.response.SharedBalanceResponse;
import com.nest.dto.response.SharedExpenseResponse;
import com.nest.security.CurrentUserProvider;
import com.nest.service.SharedExpenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/shared-expenses")
@Tag(name = "Shared Expenses")
public class SharedExpenseController {

    private final SharedExpenseService service;
    private final CurrentUserProvider currentUser;

    public SharedExpenseController(SharedExpenseService service, CurrentUserProvider currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @PostMapping
    @Operation(summary = "Split an expense", description = "EQUAL, CUSTOM amounts, or PERCENTAGE. Shares always sum exactly to the total. Tracking only; no payments.")
    public ResponseEntity<SharedExpenseResponse> create(@Valid @RequestBody SharedExpenseRequest request) {
        SharedExpenseResponse created = service.create(currentUser.currentUserId(), request);
        return ResponseEntity.created(URI.create("/api/shared-expenses/" + created.id())).body(created);
    }

    @GetMapping
    public List<SharedExpenseResponse> list() {
        return service.list(currentUser.currentUserId());
    }

    @GetMapping("/balances")
    @Operation(summary = "Who owes whom", description = "Net per person and currency. Positive: they owe you.")
    public List<SharedBalanceResponse> balances() {
        return service.balances(currentUser.currentUserId());
    }

    @GetMapping("/{id}")
    public SharedExpenseResponse get(@PathVariable long id) {
        return service.get(currentUser.currentUserId(), id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        service.delete(currentUser.currentUserId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/participants/{participantId}/settle")
    @Operation(summary = "Mark a person's share as settled")
    public SharedExpenseResponse settle(@PathVariable long id, @PathVariable long participantId) {
        return service.settle(currentUser.currentUserId(), id, participantId);
    }

    @PostMapping("/{id}/participants/{participantId}/unsettle")
    public SharedExpenseResponse unsettle(@PathVariable long id, @PathVariable long participantId) {
        return service.unsettle(currentUser.currentUserId(), id, participantId);
    }
}
