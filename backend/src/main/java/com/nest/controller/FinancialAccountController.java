package com.nest.controller;

import com.nest.dto.request.CreateAccountRequest;
import com.nest.dto.request.UpdateAccountRequest;
import com.nest.dto.response.AccountResponse;
import com.nest.security.CurrentUserProvider;
import com.nest.service.FinancialAccountService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@Tag(name = "Accounts")
public class FinancialAccountController {

    private final FinancialAccountService accountService;
    private final CurrentUserProvider currentUser;

    public FinancialAccountController(FinancialAccountService accountService, CurrentUserProvider currentUser) {
        this.accountService = accountService;
        this.currentUser = currentUser;
    }

    @PostMapping
    @Operation(summary = "Open an account",
            description = "201 created; 400 invalid input; 409 an open account with this name exists; "
                    + "422 domain rule violated (negative opening balance for SAVINGS/CASH, too many decimals).")
    public ResponseEntity<AccountResponse> create(@Valid @RequestBody CreateAccountRequest request) {
        AccountResponse created = accountService.create(currentUser.currentUserId(), request);
        return ResponseEntity.created(URI.create("/api/accounts/" + created.id())).body(created);
    }

    @GetMapping
    @Operation(summary = "List my accounts with current balances", description = "Closed accounts are hidden unless includeInactive=true.")
    public List<AccountResponse> list(@RequestParam(defaultValue = "false") boolean includeInactive) {
        return accountService.list(currentUser.currentUserId(), includeInactive);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one of my accounts", description = "404 if it does not exist or belongs to another user.")
    public AccountResponse get(@PathVariable long id) {
        return accountService.get(currentUser.currentUserId(), id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Rename an account or correct its opening balance",
            description = "Type and currency cannot change. 422 if the account is closed.")
    public AccountResponse update(@PathVariable long id, @Valid @RequestBody UpdateAccountRequest request) {
        return accountService.update(currentUser.currentUserId(), id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Close an account",
            description = "Soft delete: the account becomes inactive and keeps its history. Idempotent. 204 on success.")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        accountService.deactivate(currentUser.currentUserId(), id);
        return ResponseEntity.noContent().build();
    }
}
