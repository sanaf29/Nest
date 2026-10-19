package com.nest.service.impl;

import com.nest.dto.response.AccountResponse;
import com.nest.dto.response.CategorySpendingResponse;
import com.nest.dto.response.DashboardSummaryResponse;
import com.nest.dto.response.MonthlyTrendsResponse;
import com.nest.exception.ResourceNotFoundException;
import com.nest.model.BudgetStatus;
import com.nest.model.BudgetSummary;
import com.nest.model.GoalStatus;
import com.nest.model.Money;
import com.nest.model.MonthlyBudget;
import com.nest.model.SavingsGoal;
import com.nest.model.TransactionType;
import com.nest.repository.CategoryTotal;
import com.nest.repository.CurrencyTypeTotal;
import com.nest.repository.MonthlyBudgetRepository;
import com.nest.repository.MonthlyTypeTotal;
import com.nest.repository.SavingsGoalRepository;
import com.nest.repository.TransactionRepository;
import com.nest.repository.UserRepository;
import com.nest.service.DashboardService;
import com.nest.service.FinancialAccountService;
import com.nest.service.LedgerAnalytics;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class DashboardServiceImpl implements DashboardService {

    private static final int MAX_ACTIVE_GOALS = 5;

    private final FinancialAccountService accountService;
    private final TransactionRepository transactionRepository;
    private final MonthlyBudgetRepository budgetRepository;
    private final SavingsGoalRepository goalRepository;
    private final UserRepository userRepository;
    private final LedgerAnalytics ledgerAnalytics;

    public DashboardServiceImpl(FinancialAccountService accountService,
                                TransactionRepository transactionRepository,
                                MonthlyBudgetRepository budgetRepository,
                                SavingsGoalRepository goalRepository,
                                UserRepository userRepository,
                                LedgerAnalytics ledgerAnalytics) {
        this.accountService = accountService;
        this.transactionRepository = transactionRepository;
        this.budgetRepository = budgetRepository;
        this.goalRepository = goalRepository;
        this.userRepository = userRepository;
        this.ledgerAnalytics = ledgerAnalytics;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardSummaryResponse summary(long userId, YearMonth month) {
        String preferred = preferredCurrency(userId);
        List<SavingsGoal> goals = goalRepository.findByUserIdOrderByStatusDescTargetDateAscNameAsc(userId);
        return new DashboardSummaryResponse(
                month.getYear(),
                month.getMonthValue(),
                preferred,
                balancesByCurrency(userId),
                cashFlowByCurrency(userId, month),
                budgetSnapshot(userId, month, preferred),
                goals.stream()
                        .filter(goal -> goal.getStatus() == GoalStatus.ACTIVE)
                        .limit(MAX_ACTIVE_GOALS)
                        .map(DashboardServiceImpl::toGoalProgress)
                        .toList(),
                goals.stream().filter(goal -> goal.getStatus() == GoalStatus.ACHIEVED).count());
    }

    /** Reuses the account service so balances are computed exactly as on the accounts page. */
    private List<DashboardSummaryResponse.CurrencyBalance> balancesByCurrency(long userId) {
        Map<String, List<AccountResponse>> byCurrency = new TreeMap<>();
        for (AccountResponse account : accountService.list(userId, false)) {
            byCurrency.computeIfAbsent(account.currency(), c -> new ArrayList<>()).add(account);
        }
        return byCurrency.entrySet().stream()
                .map(entry -> new DashboardSummaryResponse.CurrencyBalance(
                        entry.getKey(),
                        entry.getValue().stream()
                                .map(a -> Money.of(a.currentBalance(), entry.getKey()))
                                .reduce(Money.zero(entry.getKey()), Money::plus)
                                .amount(),
                        entry.getValue().size()))
                .toList();
    }

    private List<DashboardSummaryResponse.CashFlow> cashFlowByCurrency(long userId, YearMonth month) {
        Map<String, Money[]> flows = new TreeMap<>(); // currency → [income, expenses]
        for (CurrencyTypeTotal row : transactionRepository.sumByCurrencyAndType(userId, month.atDay(1),
                month.atEndOfMonth(), LedgerAnalytics.CASH_FLOW_TYPES)) {
            Money[] pair = flows.computeIfAbsent(row.currency(),
                    c -> new Money[]{Money.zero(c), Money.zero(c)});
            Money amount = Money.of(row.total(), row.currency());
            if (row.type() == TransactionType.INCOME) {
                pair[0] = pair[0].plus(amount);
            } else {
                pair[1] = pair[1].plus(amount);
            }
        }
        return flows.entrySet().stream()
                .map(entry -> new DashboardSummaryResponse.CashFlow(entry.getKey(),
                        entry.getValue()[0].amount(), entry.getValue()[1].amount(),
                        entry.getValue()[0].minus(entry.getValue()[1]).amount()))
                .toList();
    }

    private DashboardSummaryResponse.BudgetSnapshot budgetSnapshot(long userId, YearMonth month, String currency) {
        return budgetRepository.findByUserIdAndYearAndMonthAndCurrency(userId, month.getYear(), month.getMonthValue(), currency)
                .map(budget -> snapshot(budget, budget.summarize(ledgerAnalytics.monthlyActuals(userId, month, currency))))
                .orElse(null);
    }

    private static DashboardSummaryResponse.BudgetSnapshot snapshot(MonthlyBudget budget, BudgetSummary summary) {
        return new DashboardSummaryResponse.BudgetSnapshot(
                budget.getId(),
                budget.getCurrency(),
                summary.spendable().amount(),
                summary.actualExpenses().amount(),
                summary.remaining().amount(),
                summary.percentUsed(),
                (int) summary.categories().stream().filter(l -> l.status() == BudgetStatus.NEAR_LIMIT).count(),
                (int) summary.categories().stream().filter(l -> l.status() == BudgetStatus.EXCEEDED).count());
    }

    private static DashboardSummaryResponse.GoalProgress toGoalProgress(SavingsGoal goal) {
        return new DashboardSummaryResponse.GoalProgress(goal.getId(), goal.getName(), goal.getCurrency(),
                goal.savedAmount().amount(), goal.target().amount(), goal.progressPercent(), goal.getTargetDate(),
                goal.getStatus());
    }

    @Override
    @Transactional(readOnly = true)
    public CategorySpendingResponse spendingByCategory(long userId, YearMonth month, String currency) {
        String effective = currency != null ? currency : preferredCurrency(userId);
        List<CategoryTotal> totals = ledgerAnalytics.expensesByCategory(userId, month, effective);
        Money total = totals.stream().map(t -> Money.of(t.total(), effective)).reduce(Money.zero(effective), Money::plus);
        List<CategorySpendingResponse.Slice> slices = totals.stream()
                .sorted(Comparator.comparing(CategoryTotal::total).reversed().thenComparing(CategoryTotal::categoryName))
                .map(t -> new CategorySpendingResponse.Slice(t.categoryId(), t.categoryName(), t.color(),
                        Money.of(t.total(), effective).amount(), percentOf(t.total(), total.amount())))
                .toList();
        return new CategorySpendingResponse(month.getYear(), month.getMonthValue(), effective, total.amount(), slices);
    }

    @Override
    @Transactional(readOnly = true)
    public MonthlyTrendsResponse monthlyTrends(long userId, YearMonth endMonth, int months, String currency) {
        String effective = currency != null ? currency : preferredCurrency(userId);
        YearMonth start = endMonth.minusMonths(months - 1L);
        Map<YearMonth, Money[]> byMonth = new TreeMap<>();
        for (YearMonth m = start; !m.isAfter(endMonth); m = m.plusMonths(1)) {
            byMonth.put(m, new Money[]{Money.zero(effective), Money.zero(effective)});
        }
        for (MonthlyTypeTotal row : transactionRepository.sumByMonthAndType(userId, effective, start.atDay(1),
                endMonth.atEndOfMonth(), LedgerAnalytics.CASH_FLOW_TYPES)) {
            Money[] pair = byMonth.get(YearMonth.of(row.year(), row.month()));
            Money amount = Money.of(row.total(), effective);
            if (row.type() == TransactionType.INCOME) {
                pair[0] = pair[0].plus(amount);
            } else {
                pair[1] = pair[1].plus(amount);
            }
        }
        return new MonthlyTrendsResponse(effective, byMonth.entrySet().stream()
                .map(entry -> new MonthlyTrendsResponse.MonthTotals(entry.getKey().getYear(),
                        entry.getKey().getMonthValue(), entry.getValue()[0].amount(), entry.getValue()[1].amount(),
                        entry.getValue()[0].minus(entry.getValue()[1]).amount()))
                .toList());
    }

    /** part ÷ whole × 100, HALF_EVEN to 2 decimals; 0 when whole is 0. */
    static BigDecimal percentOf(BigDecimal part, BigDecimal whole) {
        if (whole.signum() == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return part.multiply(BigDecimal.valueOf(100)).divide(whole, 2, RoundingMode.HALF_EVEN);
    }

    private String preferredCurrency(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId))
                .getPreferredCurrency();
    }
}
