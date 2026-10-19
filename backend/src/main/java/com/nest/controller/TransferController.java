package com.nest.controller;

import com.nest.dto.request.TransferRequest;
import com.nest.dto.response.TransferResponse;
import com.nest.security.CurrentUserProvider;
import com.nest.service.TransferService;
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

/**
 * To list transfers, use {@code GET /api/transactions?type=TRANSFER_OUT}: each transfer appears
 * once, with its id in {@code transferId}.
 */
@RestController
@RequestMapping("/api/transfers")
@Tag(name = "Transfers")
public class TransferController {

    private final TransferService transferService;
    private final CurrentUserProvider currentUser;

    public TransferController(TransferService transferService, CurrentUserProvider currentUser) {
        this.transferService = transferService;
        this.currentUser = currentUser;
    }

    @PostMapping
    @Operation(summary = "Move money between two of my accounts",
            description = "Creates a TRANSFER_OUT and a TRANSFER_IN entry atomically. Not income or spending. "
                    + "201 created; 400 invalid input; 404 account not found or not yours; "
                    + "422 same account, different currencies, closed account, or too many decimals.")
    public ResponseEntity<TransferResponse> create(@Valid @RequestBody TransferRequest request) {
        TransferResponse created = transferService.create(currentUser.currentUserId(), request);
        return ResponseEntity.created(URI.create("/api/transfers/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one of my transfers")
    public TransferResponse get(@PathVariable long id) {
        return transferService.get(currentUser.currentUserId(), id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a transfer and both of its entries",
            description = "To correct a transfer, delete it and create it again. 422 if either account is closed.")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        transferService.delete(currentUser.currentUserId(), id);
        return ResponseEntity.noContent().build();
    }
}
