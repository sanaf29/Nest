package com.nest.model;

import com.nest.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SharedExpenseTest {

    private final User owner = new User("A", "B", "a@example.com", "$2a$hash", "CAD");

    private static SharedExpense.ParticipantSpec p(String name, boolean me, boolean paid) {
        return new SharedExpense.ParticipantSpec(name, null, me, paid, null);
    }

    private SharedExpense dinner(List<SharedExpense.ParticipantSpec> people) {
        SharedExpense expense = new SharedExpense(owner, "Dinner", new BigDecimal("90"), "CAD", LocalDate.of(2026, 10, 18), SplitMethod.EQUAL, people);
        for (int i = 0; i < expense.getParticipants().size(); i++) {
            ReflectionTestUtils.setField(expense.getParticipants().get(i), "id", (long) i + 1);
        }
        return expense;
    }

    @Test
    void whenIPaidOthersOweMeUntilSettled() {
        SharedExpense expense = dinner(List.of(p("Me", true, true), p("Alex", false, false), p("Sam", false, false)));

        assertThat(expense.owedToMe()).isEqualTo(Money.of(new BigDecimal("60"), "CAD"));
        assertThat(expense.iOwe()).isEqualTo(Money.zero("CAD"));
        assertThat(expense.isSettled()).isFalse();

        expense.settle(2, Instant.now());
        expense.settle(3, Instant.now());
        assertThat(expense.owedToMe()).isEqualTo(Money.zero("CAD"));
        assertThat(expense.isSettled()).isTrue();

        expense.unsettle(3);
        assertThat(expense.owedToMe()).isEqualTo(Money.of(new BigDecimal("30"), "CAD"));
    }

    @Test
    void whenSomeoneElsePaidIOweThem() {
        SharedExpense expense = dinner(List.of(p("Me", true, false), p("Alex", false, true)));

        assertThat(expense.iOwe()).isEqualTo(Money.of(new BigDecimal("45"), "CAD"));
        assertThat(expense.owedToMe()).isEqualTo(Money.zero("CAD"));
        assertThatThrownBy(() -> expense.settle(2, Instant.now())).isInstanceOf(BusinessRuleException.class); // the payer owes nothing
    }

    @Test
    void participantRules() {
        assertThatThrownBy(() -> dinner(List.of(p("Me", true, true)))).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> dinner(List.of(p("Me", true, true), p("me ", false, false)))).hasMessageContaining("more than once");
        assertThatThrownBy(() -> dinner(List.of(p("Me", false, true), p("Alex", false, false)))).hasMessageContaining("as you");
        assertThatThrownBy(() -> dinner(List.of(p("Me", true, true), p("Alex", false, true)))).hasMessageContaining("paid");
        assertThatThrownBy(() -> new SharedExpense(owner, "x", BigDecimal.ZERO, "CAD", LocalDate.now(), SplitMethod.EQUAL,
                List.of(p("Me", true, true), p("Alex", false, false)))).isInstanceOf(BusinessRuleException.class);
    }
}
