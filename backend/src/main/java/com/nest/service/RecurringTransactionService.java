package com.nest.service;

import com.nest.dto.request.RecurringRequest;
import com.nest.dto.request.UpdateRecurringRequest;
import com.nest.dto.response.RecurringResponse;
import com.nest.dto.response.RecurringRunResponse;

import java.util.List;

public interface RecurringTransactionService {

    /** Creates the rule and immediately generates any occurrences already due (start date in the past or today). */
    RecurringResponse create(long userId, RecurringRequest request);

    List<RecurringResponse> list(long userId);

    RecurringResponse get(long userId, long id);

    RecurringResponse update(long userId, long id, UpdateRecurringRequest request);

    RecurringResponse pause(long userId, long id);

    RecurringResponse resume(long userId, long id);

    /** Ends the rule permanently. Generated transactions remain. */
    void end(long userId, long id);

    /** Generates the user's due occurrences now (the scheduler does this for everyone hourly). */
    RecurringRunResponse runNow(long userId);
}
