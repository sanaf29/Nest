package com.nest.service.impl;

import com.nest.dto.response.AccountResponse;
import com.nest.dto.response.CategorySpendingResponse;
import com.nest.dto.response.DashboardSummaryResponse;
import com.nest.dto.response.MonthlyTrendsResponse;
import com.nest.model.AccountType;
import com.nest.model.TransactionType;
import com.nest.model.User;
import com.nest.repository.CategoryTotal;
import com.nest.repository.CurrencyTypeTotal;
import com.nest.repository.MonthlyBudgetRepository;
import com.nest.repository.MonthlyTypeTotal;
import com.nest.repository.SavingsGoalRepository;
import com.nest.repository.TransactionRepository;
import com.nest.repository.UserRepository;
import com.nest.service.FinancialAccountService;
import com.nest.service.LedgerAnalytics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    private static final long USER_ID = 1L;
    private static final YearMonth OCTOBER = YearMonth.of(2026, 10);

    @Mock
    private FinancialAccountService accountService;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private MonthlyBudgetRepository budgetRepository;
    @Mock
    private SavingsGoalRepository goalRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private LedgerAnalytics ledgerAnalytics;

    private DashboardServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DashboardServiceImpl(accountService, transactionRepository, budgetRepository, goalRepository,
                userRepository, ledgerAnalytics);
        User user = new User("Sanae", "Faghfouri", "sanae@example.com", "$2a$hash", "CAD");
        // Lenient: the pure percentOf test does not look up the user.
        lenient().when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    }

    private static AccountResponse account(String currency, String balance) {
        return new AccountResponse(1L, "A", AccountType.CHECKING, currency, BigDecimal.ZERO, new BigDecimal(balance), true, null, null);
    }

    @Test
    void summaryGroupsBalancesAndCashFlowByCurrencyWithoutMixingThem() {
        when(accountService.list(USER_ID, false)).thenReturn(List.of(
                account("CAD", "1500.00"), account("CAD", "-220.40"), account("USD", "300.00")));
        when(transactionRepository.sumByCurrencyAndType(eq(USER_ID), eq(OCTOBER.atDay(1)), eq(OCTOBER.atEndOfMonth()), any()))
                .thenReturn(List.of(
                        new CurrencyTypeTotal("CAD", TransactionType.INCOME, new BigDecimal("3000.0000")),
                        new CurrencyTypeTotal("CAD", TransactionType.EXPENSE, new BigDecimal("1245.6700")),
                        new CurrencyTypeTotal("USD", TransactionType.EXPENSE, new BigDecimal("40.0000"))));
        when(budgetRepository.findByUserIdAndYearAndMonthAndCurrency(anyLong(), anyInt(), anyInt(), anyString()))
                .thenReturn(Optional.empty());
        when(goalRepository.findByUserIdOrderByStatusDescTargetDateAscNameAsc(USER_ID)).thenReturn(List.of());

        DashboardSummaryResponse summary = service.summary(USER_ID, OCTOBER);

        assertThat(summary.balances()).containsExactly(
                new DashboardSummaryResponse.CurrencyBalance("CAD", new BigDecimal("1279.60"), 2),
                new DashboardSummaryResponse.CurrencyBalance("USD", new BigDecimal("300.00"), 1));
        assertThat(summary.cashFlow()).containsExactly(
                new DashboardSummaryResponse.CashFlow("CAD", new BigDecimal("3000.00"), new BigDecimal("1245.67"), new BigDecimal("1754.33")),
                new DashboardSummaryResponse.CashFlow("USD", new BigDecimal("0.00"), new BigDecimal("40.00"), new BigDecimal("-40.00")));
        assertThat(summary.budget()).isNull();
        assertThat(summary.preferredCurrency()).isEqualTo("CAD");
    }

    @Test
    void trendsIncludeEmptyMonthsOldestFirst() {
        when(transactionRepository.sumByMonthAndType(eq(USER_ID), eq("CAD"), eq(LocalDate.of(2026, 8, 1)),
                eq(LocalDate.of(2026, 10, 31)), any()))
                .thenReturn(List.of(
                        new MonthlyTypeTotal(2026, 8, TransactionType.INCOME, new BigDecimal("100")),
                        new MonthlyTypeTotal(2026, 10, TransactionType.EXPENSE, new BigDecimal("30"))));

        MonthlyTrendsResponse trends = service.monthlyTrends(USER_ID, OCTOBER, 3, null);

        assertThat(trends.months()).extracting(MonthlyTrendsResponse.MonthTotals::month).containsExactly(8, 9, 10);
        assertThat(trends.months().get(0).net()).isEqualByComparingTo("100");
        assertThat(trends.months().get(1).income()).isEqualByComparingTo("0");
        assertThat(trends.months().get(2).net()).isEqualByComparingTo("-30");
    }

    @Test
    void trendsCrossYearBoundaries() {
        when(transactionRepository.sumByMonthAndType(eq(USER_ID), eq("CAD"), eq(LocalDate.of(2025, 11, 1)),
                eq(LocalDate.of(2026, 2, 28)), any())).thenReturn(List.of());

        MonthlyTrendsResponse trends = service.monthlyTrends(USER_ID, YearMonth.of(2026, 2), 4, null);

        assertThat(trends.months()).extracting(m -> m.year() + "-" + m.month())
                .containsExactly("2025-11", "2025-12", "2026-1", "2026-2");
    }

    @Test
    void spendingByCategoryIsSortedLargestFirstWithPercentages() {
        when(ledgerAnalytics.expensesByCategory(USER_ID, OCTOBER, "CAD")).thenReturn(List.of(
                new CategoryTotal(1L, "Dining", "#F59E0B", new BigDecimal("100")),
                new CategoryTotal(2L, "Groceries", "#D97706", new BigDecimal("200"))));

        CategorySpendingResponse spending = service.spendingByCategory(USER_ID, OCTOBER, null);

        assertThat(spending.total()).isEqualByComparingTo("300");
        assertThat(spending.categories()).extracting(CategorySpendingResponse.Slice::categoryName)
                .containsExactly("Groceries", "Dining");
        assertThat(spending.categories().get(0).percentOfTotal()).isEqualByComparingTo("66.67");
        assertThat(spending.categories().get(1).percentOfTotal()).isEqualByComparingTo("33.33");
    }

    @Test
    void percentOfZeroTotalIsZero() {
        assertThat(DashboardServiceImpl.percentOf(BigDecimal.ZERO, BigDecimal.ZERO)).isEqualByComparingTo("0");
    }
}
