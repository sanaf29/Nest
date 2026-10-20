package com.nest.model;

import com.nest.exception.BusinessRuleException;
import com.nest.exception.ResourceNotFoundException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * An expense shared with other people, tracked from the owner's point of view.
 *
 * <p>Rules: 2–20 participants with unique names; exactly one of them is "me" (the owner) and
 * exactly one paid. Shares come from the chosen {@link SplitStrategy} and always sum to the total.
 * Every other participant with a share owes it to the payer until it is marked settled.
 * Nothing here moves money; it only records who owes whom.
 */
@Entity
@Table(name = "shared_expenses")
public class SharedExpense extends AuditableEntity {

    public static final int MIN_PARTICIPANTS = 2;
    public static final int MAX_PARTICIPANTS = 20;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Column(name = "description", nullable = false, length = 150, updatable = false)
    private String description;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 4, updatable = false)
    private BigDecimal totalAmount;

    @Column(name = "currency", nullable = false, length = 3, columnDefinition = "char(3)", updatable = false)
    private String currency;

    @Column(name = "expense_date", nullable = false, updatable = false)
    private LocalDate expenseDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "split_method", nullable = false, length = 12, updatable = false)
    private SplitMethod splitMethod;

    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position")
    private List<ExpenseParticipant> participants = new ArrayList<>();

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /** Required by JPA only. */
    protected SharedExpense() {
    }

    /** One participant as entered: {@code value} is ignored (EQUAL), an amount (CUSTOM), or a percentage (PERCENTAGE). */
    public record ParticipantSpec(String name, String email, boolean me, boolean paid, BigDecimal value) {
    }

    public SharedExpense(User owner, String description, BigDecimal total, String currency, LocalDate expenseDate,
                         SplitMethod method, List<ParticipantSpec> specs) {
        this.user = Objects.requireNonNull(owner, "owner");
        if (description == null || description.isBlank() || description.strip().length() > 150) {
            throw new BusinessRuleException("Describe the expense in 1 to 150 characters.");
        }
        this.description = description.strip();
        this.currency = Currencies.requireSupported(currency);
        Money money = Money.of(Objects.requireNonNull(total, "total"), currency);
        if (!money.isPositive()) {
            throw new BusinessRuleException("The total must be greater than zero.");
        }
        this.totalAmount = money.amount();
        this.expenseDate = Objects.requireNonNull(expenseDate, "expenseDate");
        this.splitMethod = Objects.requireNonNull(method, "method");
        validateParticipants(specs);

        List<Money> shares = method.strategy().split(money, specs.stream().map(ParticipantSpec::value).toList());
        for (int i = 0; i < specs.size(); i++) {
            ParticipantSpec spec = specs.get(i);
            String email = spec.email() == null || spec.email().isBlank() ? null : spec.email().strip();
            participants.add(new ExpenseParticipant(this, i, spec.name().strip(), email, spec.me(), spec.paid(), shares.get(i)));
        }
    }

    private static void validateParticipants(List<ParticipantSpec> specs) {
        if (specs == null || specs.size() < MIN_PARTICIPANTS || specs.size() > MAX_PARTICIPANTS) {
            throw new BusinessRuleException("Add between %d and %d people, including yourself.".formatted(MIN_PARTICIPANTS, MAX_PARTICIPANTS));
        }
        Set<String> names = new HashSet<>();
        for (ParticipantSpec spec : specs) {
            if (spec.name() == null || spec.name().isBlank() || spec.name().strip().length() > 100) {
                throw new BusinessRuleException("Every person needs a name of up to 100 characters.");
            }
            if (!names.add(spec.name().strip().toLowerCase(Locale.ROOT))) {
                throw new BusinessRuleException("'%s' is listed more than once.".formatted(spec.name().strip()));
            }
        }
        if (specs.stream().filter(ParticipantSpec::me).count() != 1) {
            throw new BusinessRuleException("Mark exactly one person as you.");
        }
        if (specs.stream().filter(ParticipantSpec::paid).count() != 1) {
            throw new BusinessRuleException("Mark exactly one person as the one who paid.");
        }
    }

    public void settle(long participantId, Instant now) {
        participant(participantId).settle(now);
    }

    public void unsettle(long participantId) {
        participant(participantId).unsettle();
    }

    private ExpenseParticipant participant(long participantId) {
        return participants.stream()
                .filter(p -> p.getId() != null && p.getId() == participantId)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Participant", participantId));
    }

    public ExpenseParticipant payer() {
        return participants.stream().filter(ExpenseParticipant::isPayer).findFirst().orElseThrow();
    }

    public ExpenseParticipant me() {
        return participants.stream().filter(ExpenseParticipant::isMe).findFirst().orElseThrow();
    }

    /** All debts settled (or nobody owed anything). */
    public boolean isSettled() {
        return participants.stream().filter(ExpenseParticipant::owesPayer).allMatch(ExpenseParticipant::isSettled);
    }

    /** What others still owe me (I paid). */
    public Money owedToMe() {
        if (!payer().isMe()) {
            return Money.zero(currency);
        }
        return participants.stream()
                .filter(p -> p.owesPayer() && !p.isSettled())
                .map(ExpenseParticipant::share)
                .reduce(Money.zero(currency), Money::plus);
    }

    /** What I still owe the payer (someone else paid). */
    public Money iOwe() {
        ExpenseParticipant me = me();
        return me.owesPayer() && !me.isSettled() ? me.share() : Money.zero(currency);
    }

    public Money total() {
        return Money.of(totalAmount, currency);
    }

    public Long getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public String getCurrency() {
        return currency;
    }

    public LocalDate getExpenseDate() {
        return expenseDate;
    }

    public SplitMethod getSplitMethod() {
        return splitMethod;
    }

    public List<ExpenseParticipant> getParticipants() {
        return Collections.unmodifiableList(participants);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SharedExpense that)) {
            return false;
        }
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(SharedExpense.class);
    }
}
