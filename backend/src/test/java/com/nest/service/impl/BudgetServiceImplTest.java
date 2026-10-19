package com.nest.service.impl;

import com.nest.dto.request.AllocationRequest;
import com.nest.dto.request.CreateBudgetRequest;
import com.nest.dto.response.BudgetResponse;
import com.nest.dto.response.BudgetSummaryResponse;
import com.nest.exception.ConflictException;
import com.nest.exception.ResourceNotFoundException;
import com.nest.mapper.BudgetMapper;
import com.nest.model.CategoryFixtures;
import com.nest.model.CategoryType;
import com.nest.model.Money;
import com.nest.model.MonthlyActuals;
import com.nest.model.MonthlyBudget;
import com.nest.model.SavingsStrategyType;
import com.nest.model.User;
import com.nest.repository.CategoryRepository;
import com.nest.repository.MonthlyBudgetRepository;
import com.nest.repository.UserRepository;
import com.nest.service.LedgerAnalytics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetServiceImplTest {

    private static final long USER_ID = 1L;

    @Mock
    private MonthlyBudgetRepository budgetRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private LedgerAnalytics ledgerAnalytics;

    private BudgetServiceImpl service;
    private User owner;

    @BeforeEach
    void setUp() {
        service = new BudgetServiceImpl(budgetRepository, userRepository, categoryRepository, ledgerAnalytics, new BudgetMapper());
        owner = new User("Sanae", "Faghfouri", "sanae@example.com", "$2a$hash", "CAD");
        ReflectionTestUtils.setField(owner, "id", USER_ID);
    }

    private static CreateBudgetRequest request(String currency, List<AllocationRequest> allocations) {
        return new CreateBudgetRequest(2026, 10, currency, new BigDecimal("3000"), SavingsStrategyType.PERCENTAGE,
                BigDecimal.valueOf(20), allocations);
    }

    @Test
    void createsBudgetInPreferredCurrencyWithAllocations() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(owner));
        when(categoryRepository.findVisible(9L, USER_ID))
                .thenReturn(Optional.of(CategoryFixtures.defaultCategory(9L, "Groceries", CategoryType.EXPENSE)));
        when(budgetRepository.saveAndFlush(any(MonthlyBudget.class))).thenAnswer(inv -> inv.getArgument(0));

        BudgetResponse response = service.create(USER_ID, request(null, List.of(new AllocationRequest(9L, new BigDecimal("500")))));

        assertThat(response.currency()).isEqualTo("CAD");
        assertThat(response.savingsTarget()).isEqualByComparingTo("600.00");
        assertThat(response.spendableAmount()).isEqualByComparingTo("2400.00");
        assertThat(response.unallocatedAmount()).isEqualByComparingTo("1900.00");
        assertThat(response.allocations()).singleElement().satisfies(a -> assertThat(a.categoryName()).isEqualTo("Groceries"));
    }

    @Test
    void duplicateMonthAndCurrencyIsAConflict() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(owner));
        when(budgetRepository.existsByUserIdAndYearAndMonthAndCurrency(USER_ID, 2026, 10, "CAD")).thenReturn(true);

        assertThatThrownBy(() -> service.create(USER_ID, request("CAD", List.of()))).isInstanceOf(ConflictException.class);
        verify(budgetRepository, never()).saveAndFlush(any());
    }

    @Test
    void invisibleCategoryIsNotFound() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(owner));
        when(categoryRepository.findVisible(77L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(USER_ID, request("CAD", List.of(new AllocationRequest(77L, BigDecimal.TEN)))))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void summaryUsesActualsForTheBudgetsMonthAndCurrency() {
        MonthlyBudget budget = new MonthlyBudget(owner, YearMonth.of(2026, 10), "CAD", new BigDecimal("3000"),
                SavingsStrategyType.PERCENTAGE, BigDecimal.valueOf(20));
        when(budgetRepository.findByIdAndUserId(5L, USER_ID)).thenReturn(Optional.of(budget));
        when(ledgerAnalytics.monthlyActuals(USER_ID, YearMonth.of(2026, 10), "CAD"))
                .thenReturn(new MonthlyActuals(Money.of(new BigDecimal("3000"), "CAD"),
                        Map.of(9L, Money.of(new BigDecimal("1000"), "CAD"))));

        BudgetSummaryResponse summary = service.summary(USER_ID, 5L);

        assertThat(summary.remaining()).isEqualByComparingTo("1400.00");
        assertThat(summary.netCashFlow()).isEqualByComparingTo("2000.00");
    }

    @Test
    void anotherUsersBudgetIsNotFound() {
        when(budgetRepository.findByIdAndUserId(5L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.summary(2L, 5L)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.delete(2L, 5L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
