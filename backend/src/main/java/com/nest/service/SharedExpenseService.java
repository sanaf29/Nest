package com.nest.service;

import com.nest.dto.request.SharedExpenseRequest;
import com.nest.dto.response.SharedBalanceResponse;
import com.nest.dto.response.SharedExpenseResponse;

import java.util.List;

public interface SharedExpenseService {

    SharedExpenseResponse create(long userId, SharedExpenseRequest request);

    List<SharedExpenseResponse> list(long userId);

    SharedExpenseResponse get(long userId, long id);

    void delete(long userId, long id);

    SharedExpenseResponse settle(long userId, long id, long participantId);

    SharedExpenseResponse unsettle(long userId, long id, long participantId);

    /** Who owes whom, netted per person and currency (never across currencies). */
    List<SharedBalanceResponse> balances(long userId);
}
