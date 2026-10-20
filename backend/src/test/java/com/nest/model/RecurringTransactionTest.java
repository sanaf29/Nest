package com.nest.model;

import com.nest.exception.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecurringTransactionTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 19);

    private FinancialAccount chequing;
    private Category housing;

    @BeforeEach
    void setUp() {
        User owner = new User("A", "B", "a@example.com", "$2a$hash", "CAD");
        ReflectionTestUtils.setField(owner, "id", 1L);
        chequing = new FinancialAccount(owner, "Chequing", AccountType.CHECKING, "CAD", BigDecimal.ZERO);
        ReflectionTestUtils.setField(chequing, "id", 10L);
        housing = CategoryFixtures.defaultCategory(7L, "Housing", CategoryType.EXPENSE);
    }

    private RecurringTransaction rent(LocalDate start, LocalDate end) {
        return new RecurringTransaction(chequing, housing, TransactionType.EXPENSE, new BigDecimal("1650"), " Rent ",
                Frequency.MONTHLY, start, end, 3, TODAY);
    }

    @Test
    void generatesOccurrencesAsOrdinaryTransactionsAndAdvances() {
        RecurringTransaction rule = rent(LocalDate.of(2026, 9, 1), null);

        assertThat(rule.isDue(TODAY)).isTrue();
        Transaction first = rule.generateNextOccurrence();
        Transaction second = rule.generateNextOccurrence();

        assertThat(first.getTransactionDate()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(first.getOccurrenceDate()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(first.getRecurringTransaction()).isSameAs(rule);
        assertThat(first.getDescription()).isEqualTo("Rent");
        assertThat(second.getTransactionDate()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(rule.getNextOccurrence()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(rule.isDue(TODAY)).isFalse();
    }

    @Test
    void endsAfterTheEndDate() {
        RecurringTransaction rule = rent(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 15));
        rule.generateNextOccurrence();
        rule.generateNextOccurrence();

        assertThat(rule.getStatus()).isEqualTo(RecurringStatus.ENDED);
        assertThat(rule.getNextOccurrence()).isNull();
        assertThat(rule.upcoming(3)).isEmpty();
    }

    @Test
    void closedAccountFailsGenerationWithoutChangingTheRule() {
        RecurringTransaction rule = rent(LocalDate.of(2026, 9, 1), null);
        chequing.deactivate();

        assertThatThrownBy(rule::generateNextOccurrence).isInstanceOf(BusinessRuleException.class);
        assertThat(rule.getNextOccurrence()).isEqualTo(LocalDate.of(2026, 9, 1));
    }

    @Test
    void resumingSkipsOccurrencesMissedWhilePaused() {
        RecurringTransaction rule = rent(LocalDate.of(2026, 6, 15), null);
        rule.pause("Away");
        assertThat(rule.isDue(TODAY)).isFalse();

        rule.resume(TODAY);

        assertThat(rule.getStatus()).isEqualTo(RecurringStatus.ACTIVE);
        // Jun 15 – Oct 15 were missed while paused; the next scheduled date on or after Oct 19 is Nov 15.
        assertThat(rule.getNextOccurrence()).isEqualTo(LocalDate.of(2026, 11, 15));
    }

    @Test
    void rejectsInvalidSchedules() {
        assertThatThrownBy(() -> rent(TODAY.minusDays(400), null)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> rent(TODAY, TODAY.minusDays(1))).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> new RecurringTransaction(chequing, housing, TransactionType.TRANSFER_OUT, BigDecimal.TEN,
                "x", Frequency.MONTHLY, TODAY, null, null, TODAY)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> new RecurringTransaction(chequing, housing, TransactionType.INCOME, BigDecimal.TEN,
                "x", Frequency.MONTHLY, TODAY, null, null, TODAY)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> new RecurringTransaction(chequing, housing, TransactionType.EXPENSE, BigDecimal.TEN,
                "x", Frequency.MONTHLY, TODAY, null, 31, TODAY)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void updatesApplyToFutureOccurrencesAndEndedRulesAreFinal() {
        RecurringTransaction rule = rent(LocalDate.of(2026, 9, 1), null);
        rule.generateNextOccurrence();

        rule.update(chequing, housing, new BigDecimal("1700"), "Rent (new lease)", null, null);
        Transaction next = rule.generateNextOccurrence();
        assertThat(next.money().amount()).isEqualByComparingTo("1700");

        rule.end();
        assertThatThrownBy(() -> rule.update(chequing, housing, BigDecimal.ONE, "x", null, null)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> rule.resume(TODAY)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void previewsUpcomingDates() {
        assertThat(rent(LocalDate.of(2026, 11, 30), null).upcoming(3))
                .containsExactly(LocalDate.of(2026, 11, 30), LocalDate.of(2026, 12, 30), LocalDate.of(2027, 1, 30));
    }
}
