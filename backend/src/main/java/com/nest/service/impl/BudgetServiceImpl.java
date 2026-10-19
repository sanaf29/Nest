package com.nest.service.impl;

import com.nest.dto.request.AllocationRequest;
import com.nest.dto.request.CreateBudgetRequest;
import com.nest.dto.request.UpdateBudgetRequest;
import com.nest.dto.response.BudgetResponse;
import com.nest.dto.response.BudgetSummaryResponse;
import com.nest.exception.ConflictException;
import com.nest.exception.ResourceNotFoundException;
import com.nest.mapper.BudgetMapper;
import com.nest.model.MonthlyBudget;
import com.nest.model.User;
import com.nest.repository.CategoryRepository;
import com.nest.repository.MonthlyBudgetRepository;
import com.nest.repository.UserRepository;
import com.nest.service.BudgetService;
import com.nest.service.LedgerAnalytics;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;

@Service
public class BudgetServiceImpl implements BudgetService {

    private final MonthlyBudgetRepository budgetRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final LedgerAnalytics ledgerAnalytics;
    private final BudgetMapper budgetMapper;

    public BudgetServiceImpl(MonthlyBudgetRepository budgetRepository,
                             UserRepository userRepository,
                             CategoryRepository categoryRepository,
                             LedgerAnalytics ledgerAnalytics,
                             BudgetMapper budgetMapper) {
        this.budgetRepository = budgetRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.ledgerAnalytics = ledgerAnalytics;
        this.budgetMapper = budgetMapper;
    }

    @Override
    @Transactional
    public BudgetResponse create(long userId, CreateBudgetRequest request) {
        User owner = findUser(userId);
        String currency = request.currency() != null ? request.currency() : owner.getPreferredCurrency();
        if (budgetRepository.existsByUserIdAndYearAndMonthAndCurrency(userId, request.year(), request.month(), currency)) {
            throw new ConflictException("A %s budget for %d-%02d already exists."
                    .formatted(currency, request.year(), request.month()));
        }
        MonthlyBudget budget = new MonthlyBudget(owner, YearMonth.of(request.year(), request.month()), currency,
                request.plannedIncome(), request.savingsStrategy(), request.savingsValue());
        budget.replaceAllocations(resolveAllocations(userId, request.allocationsOrEmpty()));
        return budgetMapper.toResponse(budgetRepository.saveAndFlush(budget));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BudgetResponse> list(long userId) {
        return budgetRepository.findByUserIdOrderByYearDescMonthDescCurrencyAsc(userId).stream()
                .map(budgetMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BudgetResponse getForMonth(long userId, int year, int month, String currency) {
        String effectiveCurrency = currency != null ? currency : findUser(userId).getPreferredCurrency();
        return budgetRepository.findByUserIdAndYearAndMonthAndCurrency(userId, year, month, effectiveCurrency)
                .map(budgetMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Budget for %d-%02d in".formatted(year, month), effectiveCurrency));
    }

    @Override
    @Transactional
    public BudgetResponse update(long userId, long budgetId, UpdateBudgetRequest request) {
        MonthlyBudget budget = findOwned(userId, budgetId);
        budget.revise(request.plannedIncome(), request.savingsStrategy(), request.savingsValue(),
                resolveAllocations(userId, request.allocationsOrEmpty()));
        return budgetMapper.toResponse(budgetRepository.saveAndFlush(budget));
    }

    @Override
    @Transactional
    public void delete(long userId, long budgetId) {
        budgetRepository.delete(findOwned(userId, budgetId));
    }

    @Override
    @Transactional(readOnly = true)
    public BudgetSummaryResponse summary(long userId, long budgetId) {
        MonthlyBudget budget = findOwned(userId, budgetId);
        var actuals = ledgerAnalytics.monthlyActuals(userId, budget.period(), budget.getCurrency());
        return budgetMapper.toSummaryResponse(budget, budget.summarize(actuals));
    }

    private List<MonthlyBudget.AllocationSpec> resolveAllocations(long userId, List<AllocationRequest> requests) {
        return requests.stream()
                .map(request -> new MonthlyBudget.AllocationSpec(
                        categoryRepository.findVisible(request.categoryId(), userId)
                                .orElseThrow(() -> new ResourceNotFoundException("Category", request.categoryId())),
                        request.limit()))
                .toList();
    }

    private MonthlyBudget findOwned(long userId, long budgetId) {
        return budgetRepository.findByIdAndUserId(budgetId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget", budgetId));
    }

    private User findUser(long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }
}
