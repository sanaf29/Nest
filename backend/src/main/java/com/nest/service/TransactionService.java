package com.nest.service;

import com.nest.dto.request.TransactionFilter;
import com.nest.dto.request.TransactionRequest;
import com.nest.dto.response.PageResponse;
import com.nest.dto.response.TransactionResponse;
import org.springframework.data.domain.Pageable;

/**
 * Income and expense entries on the user's own accounts. Every lookup is scoped to
 * {@code userId}, through the account's owner.
 */
public interface TransactionService {

    TransactionResponse create(long userId, TransactionRequest request);

    PageResponse<TransactionResponse> search(long userId, TransactionFilter filter, Pageable pageable);

    TransactionResponse get(long userId, long transactionId);

    TransactionResponse update(long userId, long transactionId, TransactionRequest request);

    void delete(long userId, long transactionId);
}
