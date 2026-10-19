package com.nest.service;

import com.nest.dto.request.TransferRequest;
import com.nest.dto.response.TransferResponse;

/**
 * Transfers between the authenticated user's own accounts. Separate from
 * {@link TransactionService} because a transfer is a two-entry aggregate with its own invariants
 * (single responsibility).
 */
public interface TransferService {

    /** Creates both entries atomically: either both are saved or neither is. */
    TransferResponse create(long userId, TransferRequest request);

    TransferResponse get(long userId, long transferId);

    /** Deletes the transfer and both of its entries. */
    void delete(long userId, long transferId);
}
