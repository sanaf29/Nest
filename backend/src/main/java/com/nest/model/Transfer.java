package com.nest.model;

import com.nest.exception.BusinessRuleException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Money moved between two accounts of the same user.
 *
 * <p>A transfer owns exactly two ledger entries (composition): {@code TRANSFER_OUT} on the
 * source account and {@code TRANSFER_IN} on the destination account, with equal amounts and the
 * same date. {@link #between} is the only way to create one, so an unbalanced or half-created
 * transfer cannot exist (factory method). The entries are saved and deleted with the transfer
 * (cascade).
 *
 * <p>Neither entry is income or spending, so transfers move balances without affecting reports.
 */
@Entity
@Table(name = "transfers")
public class Transfer extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "transfer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Transaction> legs = new ArrayList<>();

    /** Required by JPA only. */
    protected Transfer() {
    }

    /**
     * Creates a balanced transfer.
     *
     * @param description optional; when blank, each side gets "Transfer to …" / "Transfer from …"
     * @throws BusinessRuleException if the accounts are the same, belong to different users, use
     *                               different currencies, or either is closed, or if the amount is
     *                               not positive or has too many decimals
     */
    public static Transfer between(FinancialAccount source, FinancialAccount destination,
                                   BigDecimal amount, LocalDate date, String description) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(destination, "destination");
        if (source == destination || (source.getId() != null && source.getId().equals(destination.getId()))) {
            throw new BusinessRuleException("Source and destination must be different accounts.");
        }
        if (!Objects.equals(source.ownerId(), destination.ownerId())) {
            throw new BusinessRuleException("Transfers are only possible between your own accounts.");
        }
        if (!source.getCurrency().equals(destination.getCurrency())) {
            throw new BusinessRuleException("Cannot transfer from %s to %s: currency conversion is not supported."
                    .formatted(source.getCurrency(), destination.getCurrency()));
        }
        Money money = Money.of(Objects.requireNonNull(amount, "amount"), source.getCurrency());
        if (!money.isPositive()) {
            throw new BusinessRuleException("Transfer amount must be greater than zero.");
        }

        boolean hasDescription = description != null && !description.isBlank();
        Transfer transfer = new Transfer();
        transfer.legs.add(Transaction.transferLeg(transfer, source, TransactionType.TRANSFER_OUT, money,
                hasDescription ? description : "Transfer to " + destination.getName(), date));
        transfer.legs.add(Transaction.transferLeg(transfer, destination, TransactionType.TRANSFER_IN, money,
                hasDescription ? description : "Transfer from " + source.getName(), date));
        return transfer;
    }

    /**
     * Checks that the transfer may be deleted. Both accounts must be open, because deleting
     * would change a closed account's frozen history.
     */
    public void requireRemovable() {
        outgoing().getAccount().requireOpenForActivity();
        incoming().getAccount().requireOpenForActivity();
    }

    public Transaction outgoing() {
        return leg(TransactionType.TRANSFER_OUT);
    }

    public Transaction incoming() {
        return leg(TransactionType.TRANSFER_IN);
    }

    public FinancialAccount source() {
        return outgoing().getAccount();
    }

    public FinancialAccount destination() {
        return incoming().getAccount();
    }

    public Money amount() {
        return outgoing().money();
    }

    public LocalDate date() {
        return outgoing().getTransactionDate();
    }

    public List<Transaction> legs() {
        return Collections.unmodifiableList(legs);
    }

    private Transaction leg(TransactionType type) {
        return legs.stream()
                .filter(leg -> leg.getType() == type)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Transfer %d has no %s entry".formatted(id, type)));
    }

    public Long getId() {
        return id;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Transfer that)) {
            return false;
        }
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(Transfer.class);
    }

    @Override
    public String toString() {
        return "Transfer{id=%d}".formatted(id);
    }
}
