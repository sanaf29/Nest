package com.nest.service;

import com.nest.dto.request.CreateAccountRequest;
import com.nest.dto.request.UpdateAccountRequest;
import com.nest.dto.response.AccountResponse;

import java.util.List;

/**
 * Management of the authenticated user's financial accounts. Every method is scoped to
 * {@code userId}. An account owned by someone else behaves as if it does not exist.
 */
public interface FinancialAccountService {

    AccountResponse create(long userId, CreateAccountRequest request);

    List<AccountResponse> list(long userId, boolean includeInactive);

    AccountResponse get(long userId, long accountId);

    AccountResponse update(long userId, long accountId, UpdateAccountRequest request);

    /** Closes the account: hidden from default listings and closed to new activity, with history kept. */
    void deactivate(long userId, long accountId);
}
