package com.nest.service;

import com.nest.dto.request.ContributionRequest;
import com.nest.dto.request.CreateGoalRequest;
import com.nest.dto.request.UpdateGoalRequest;
import com.nest.dto.response.SavingsGoalResponse;
import com.nest.model.GoalStatus;

import java.util.List;

public interface SavingsGoalService {

    SavingsGoalResponse create(long userId, CreateGoalRequest request);

    /** @param status {@code null} for all goals */
    List<SavingsGoalResponse> list(long userId, GoalStatus status);

    SavingsGoalResponse get(long userId, long goalId);

    SavingsGoalResponse update(long userId, long goalId, UpdateGoalRequest request);

    void delete(long userId, long goalId);

    /** Result of recording a contribution: the updated goal, and whether this call created it. */
    record ContributionResult(SavingsGoalResponse goal, boolean created) {
    }

    /** Idempotent when the request carries a key: a replay returns {@code created = false} and changes nothing. */
    ContributionResult contribute(long userId, long goalId, ContributionRequest request);

    SavingsGoalResponse removeContribution(long userId, long goalId, long contributionId);
}
