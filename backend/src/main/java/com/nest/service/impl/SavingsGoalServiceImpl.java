package com.nest.service.impl;

import com.nest.dto.request.ContributionRequest;
import com.nest.dto.request.CreateGoalRequest;
import com.nest.dto.request.UpdateGoalRequest;
import com.nest.dto.response.SavingsGoalResponse;
import com.nest.exception.ConflictException;
import com.nest.exception.ResourceNotFoundException;
import com.nest.mapper.SavingsGoalMapper;
import com.nest.model.GoalStatus;
import com.nest.model.SavingsGoal;
import com.nest.model.User;
import com.nest.repository.SavingsGoalRepository;
import com.nest.repository.UserRepository;
import com.nest.service.GoalAchievedEvent;
import com.nest.service.SavingsGoalService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
public class SavingsGoalServiceImpl implements SavingsGoalService {

    private final SavingsGoalRepository goalRepository;
    private final UserRepository userRepository;
    private final SavingsGoalMapper goalMapper;
    private final Clock clock;
    private final ApplicationEventPublisher events;

    public SavingsGoalServiceImpl(SavingsGoalRepository goalRepository, UserRepository userRepository,
                                  SavingsGoalMapper goalMapper, Clock clock, ApplicationEventPublisher events) {
        this.goalRepository = goalRepository;
        this.userRepository = userRepository;
        this.goalMapper = goalMapper;
        this.clock = clock;
        this.events = events;
    }

    @Override
    @Transactional
    public SavingsGoalResponse create(long userId, CreateGoalRequest request) {
        User owner = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));
        requireUniqueName(userId, request.name(), null);
        String currency = request.currency() != null ? request.currency() : owner.getPreferredCurrency();
        SavingsGoal goal = new SavingsGoal(owner, request.name(), currency, request.targetAmount(),
                request.targetDate(), LocalDate.now(clock));
        return goalMapper.toResponse(goalRepository.saveAndFlush(goal));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SavingsGoalResponse> list(long userId, GoalStatus status) {
        List<SavingsGoal> goals = status == null
                ? goalRepository.findByUserIdOrderByStatusDescTargetDateAscNameAsc(userId)
                : goalRepository.findByUserIdAndStatusOrderByTargetDateAscNameAsc(userId, status);
        return goals.stream().map(goalMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SavingsGoalResponse get(long userId, long goalId) {
        return goalMapper.toResponse(findOwned(userId, goalId));
    }

    @Override
    @Transactional
    public SavingsGoalResponse update(long userId, long goalId, UpdateGoalRequest request) {
        SavingsGoal goal = findOwned(userId, goalId);
        requireUniqueName(userId, request.name(), goalId);
        goal.update(request.name(), request.targetAmount(), request.targetDate(), clock.instant());
        return goalMapper.toResponse(goalRepository.saveAndFlush(goal));
    }

    @Override
    @Transactional
    public void delete(long userId, long goalId) {
        goalRepository.delete(findOwned(userId, goalId));
    }

    @Override
    @Transactional
    public ContributionResult contribute(long userId, long goalId, ContributionRequest request) {
        SavingsGoal goal = findOwned(userId, goalId);
        if (request.idempotencyKey() != null && goal.contributionWithKey(request.idempotencyKey()).isPresent()) {
            return new ContributionResult(goalMapper.toResponse(goal), false);
        }
        LocalDate today = LocalDate.now(clock);
        LocalDate date = request.contributionDate() != null ? request.contributionDate() : today;
        boolean wasActive = goal.getStatus() == GoalStatus.ACTIVE;
        goal.contribute(request.amount(), date, request.note(), request.idempotencyKey(), today, clock.instant());
        if (wasActive && goal.getStatus() == GoalStatus.ACHIEVED) {
            events.publishEvent(new GoalAchievedEvent(userId, goal.getId(), goal.getName(), goal.target().toString()));
        }
        // A concurrent replay of the same key that passes the check above is stopped by the
        // UX_goal_contributions_idempotency index and reported as 409.
        return new ContributionResult(goalMapper.toResponse(goalRepository.saveAndFlush(goal)), true);
    }

    @Override
    @Transactional
    public SavingsGoalResponse removeContribution(long userId, long goalId, long contributionId) {
        SavingsGoal goal = findOwned(userId, goalId);
        if (!goal.removeContribution(contributionId, clock.instant())) {
            throw new ResourceNotFoundException("Contribution", contributionId);
        }
        return goalMapper.toResponse(goalRepository.saveAndFlush(goal));
    }

    private SavingsGoal findOwned(long userId, long goalId) {
        return goalRepository.findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Savings goal", goalId));
    }

    private void requireUniqueName(long userId, String name, Long excludedId) {
        String trimmed = name.strip();
        boolean taken = excludedId == null
                ? goalRepository.existsByUserIdAndNameIgnoreCase(userId, trimmed)
                : goalRepository.existsByUserIdAndNameIgnoreCaseAndIdNot(userId, trimmed, excludedId);
        if (taken) {
            throw new ConflictException("You already have a goal named '%s'.".formatted(trimmed));
        }
    }
}
