package com.nest.service.impl;

import com.nest.dto.request.RecurringRequest;
import com.nest.dto.request.UpdateRecurringRequest;
import com.nest.dto.response.RecurringResponse;
import com.nest.dto.response.RecurringRunResponse;
import com.nest.exception.ResourceNotFoundException;
import com.nest.mapper.RecurringMapper;
import com.nest.model.Category;
import com.nest.model.FinancialAccount;
import com.nest.model.RecurringTransaction;
import com.nest.repository.CategoryRepository;
import com.nest.repository.FinancialAccountRepository;
import com.nest.repository.RecurringTransactionRepository;
import com.nest.service.RecurringTransactionProcessor;
import com.nest.service.RecurringTransactionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
public class RecurringTransactionServiceImpl implements RecurringTransactionService {

    private final RecurringTransactionRepository ruleRepository;
    private final FinancialAccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final RecurringTransactionProcessor processor;
    private final RecurringMapper mapper;
    private final Clock clock;

    public RecurringTransactionServiceImpl(RecurringTransactionRepository ruleRepository,
                                           FinancialAccountRepository accountRepository,
                                           CategoryRepository categoryRepository,
                                           RecurringTransactionProcessor processor,
                                           RecurringMapper mapper, Clock clock) {
        this.ruleRepository = ruleRepository;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
        this.processor = processor;
        this.mapper = mapper;
        this.clock = clock;
    }

    /**
     * Not transactional itself: the rule is saved and committed first, then the processor
     * generates due occurrences in its own per-rule transaction.
     */
    @Override
    public RecurringResponse create(long userId, RecurringRequest request) {
        LocalDate today = LocalDate.now(clock);
        RecurringTransaction rule = new RecurringTransaction(
                findAccount(userId, request.accountId()), findCategory(userId, request.categoryId()), request.type(),
                request.amount(), request.description(), request.frequency(), request.startDate(), request.endDate(),
                request.remindDaysBefore(), today);
        long id = ruleRepository.saveAndFlush(rule).getId();
        processor.processDueForUser(userId, today);
        return get(userId, id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecurringResponse> list(long userId) {
        return ruleRepository.findByAccountUserIdOrderByStatusAscNextOccurrenceAscDescriptionAsc(userId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RecurringResponse get(long userId, long id) {
        return mapper.toResponse(findOwned(userId, id));
    }

    @Override
    @Transactional
    public RecurringResponse update(long userId, long id, UpdateRecurringRequest request) {
        RecurringTransaction rule = findOwned(userId, id);
        rule.update(findAccount(userId, request.accountId()), findCategory(userId, request.categoryId()),
                request.amount(), request.description(), request.endDate(), request.remindDaysBefore());
        return mapper.toResponse(ruleRepository.saveAndFlush(rule));
    }

    @Override
    @Transactional
    public RecurringResponse pause(long userId, long id) {
        RecurringTransaction rule = findOwned(userId, id);
        rule.pause("Paused by you.");
        return mapper.toResponse(ruleRepository.saveAndFlush(rule));
    }

    @Override
    @Transactional
    public RecurringResponse resume(long userId, long id) {
        RecurringTransaction rule = findOwned(userId, id);
        rule.resume(LocalDate.now(clock));
        return mapper.toResponse(ruleRepository.saveAndFlush(rule));
    }

    @Override
    @Transactional
    public void end(long userId, long id) {
        findOwned(userId, id).end();
    }

    @Override
    public RecurringRunResponse runNow(long userId) {
        RecurringTransactionProcessor.Result result = processor.processDueForUser(userId, LocalDate.now(clock));
        return new RecurringRunResponse(result.generated(), result.paused());
    }

    private RecurringTransaction findOwned(long userId, long id) {
        return ruleRepository.findByIdAndAccountUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring transaction", id));
    }

    private FinancialAccount findAccount(long userId, long accountId) {
        return accountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountId));
    }

    private Category findCategory(long userId, long categoryId) {
        return categoryRepository.findVisible(categoryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", categoryId));
    }
}
