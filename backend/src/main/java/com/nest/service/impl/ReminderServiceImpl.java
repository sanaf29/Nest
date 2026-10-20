package com.nest.service.impl;

import com.nest.dto.request.ReminderRequest;
import com.nest.dto.response.ReminderResponse;
import com.nest.exception.ResourceNotFoundException;
import com.nest.model.Category;
import com.nest.model.Reminder;
import com.nest.model.ReminderStatus;
import com.nest.model.SavingsGoal;
import com.nest.repository.CategoryRepository;
import com.nest.repository.ReminderRepository;
import com.nest.repository.SavingsGoalRepository;
import com.nest.repository.UserRepository;
import com.nest.service.ReminderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

@Service
public class ReminderServiceImpl implements ReminderService {

    private final ReminderRepository reminderRepository;
    private final UserRepository userRepository;
    private final SavingsGoalRepository goalRepository;
    private final CategoryRepository categoryRepository;
    private final Clock clock;

    public ReminderServiceImpl(ReminderRepository reminderRepository, UserRepository userRepository,
                               SavingsGoalRepository goalRepository, CategoryRepository categoryRepository, Clock clock) {
        this.reminderRepository = reminderRepository;
        this.userRepository = userRepository;
        this.goalRepository = goalRepository;
        this.categoryRepository = categoryRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public ReminderResponse create(long userId, ReminderRequest request) {
        Reminder reminder = new Reminder(userRepository.getReferenceById(userId), request.type(), details(userId, request), clock.instant());
        return toResponse(reminderRepository.saveAndFlush(reminder));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReminderResponse> list(long userId, ReminderStatus status) {
        List<Reminder> reminders = status == null
                ? reminderRepository.findByUserIdOrderByStatusDescCreatedAtDesc(userId)
                : reminderRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status);
        return reminders.stream().map(ReminderServiceImpl::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ReminderResponse get(long userId, long id) {
        return toResponse(findOwned(userId, id));
    }

    @Override
    @Transactional
    public ReminderResponse update(long userId, long id, ReminderRequest request) {
        Reminder reminder = findOwned(userId, id);
        if (request.type() != reminder.getType()) {
            throw new com.nest.exception.BusinessRuleException("A reminder's type cannot be changed; create a new reminder instead.");
        }
        reminder.update(details(userId, request), clock.instant());
        return toResponse(reminderRepository.saveAndFlush(reminder));
    }

    @Override
    @Transactional
    public ReminderResponse dismiss(long userId, long id) {
        Reminder reminder = findOwned(userId, id);
        reminder.dismiss();
        return toResponse(reminderRepository.saveAndFlush(reminder));
    }

    @Override
    @Transactional
    public void delete(long userId, long id) {
        reminderRepository.delete(findOwned(userId, id));
    }

    private Reminder.Details details(long userId, ReminderRequest request) {
        SavingsGoal goal = request.savingsGoalId() == null ? null
                : goalRepository.findByIdAndUserId(request.savingsGoalId(), userId)
                        .orElseThrow(() -> new ResourceNotFoundException("Savings goal", request.savingsGoalId()));
        Category category = request.categoryId() == null ? null
                : categoryRepository.findVisible(request.categoryId(), userId)
                        .orElseThrow(() -> new ResourceNotFoundException("Category", request.categoryId()));
        return new Reminder.Details(request.title(), request.notes(), request.dueAt(),
                request.offsetMinutes() == null ? 0 : request.offsetMinutes(), request.location(), goal, category,
                request.thresholdPercent());
    }

    private Reminder findOwned(long userId, long id) {
        return reminderRepository.findByIdAndUserId(id, userId).orElseThrow(() -> new ResourceNotFoundException("Reminder", id));
    }

    private static ReminderResponse toResponse(Reminder r) {
        return new ReminderResponse(r.getId(), r.getType(), r.getTitle(), r.getNotes(), r.getDueAt(), r.getOffsetMinutes(),
                r.remindAt(), r.getStatus(), r.getSentAt(), r.getLocation(),
                r.getSavingsGoal() == null ? null : r.getSavingsGoal().getId(),
                r.getSavingsGoal() == null ? null : r.getSavingsGoal().getName(),
                r.getCategory() == null ? null : r.getCategory().getId(),
                r.getCategory() == null ? null : r.getCategory().getName(),
                r.getThresholdPercent(), r.getCreatedAt());
    }
}
