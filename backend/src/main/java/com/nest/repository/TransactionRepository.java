package com.nest.repository;

import com.nest.model.Transaction;
import com.nest.model.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    /** Ownership goes through the account: transaction → account → user. */
    @EntityGraph(attributePaths = {"account", "category"})
    Optional<Transaction> findByIdAndAccountUserId(Long id, Long userId);

    /** Loads account and category in the same query, so a page of results costs one select (no N+1). */
    @Override
    @EntityGraph(attributePaths = {"account", "category"})
    Page<Transaction> findAll(Specification<Transaction> spec, Pageable pageable);

    /**
     * Per-type totals for a set of accounts in one GROUP BY query. Balances for any number of
     * accounts cost a single round trip.
     */
    @Query("""
            select new com.nest.repository.AccountTypeTotal(t.account.id, t.type, sum(t.amount))
            from Transaction t
            where t.account.id in :accountIds
            group by t.account.id, t.type
            """)
    List<AccountTypeTotal> sumByAccountAndType(@Param("accountIds") Collection<Long> accountIds);

    // ---- Analytics (Sprint 2). All are scoped to one user, a date range, and the given types. ----
    // Callers pass INCOME and EXPENSE only, so transfers never reach budgets or dashboards.

    @Query("""
            select new com.nest.repository.TypeTotal(t.type, sum(t.amount))
            from Transaction t
            where t.account.user.id = :userId
              and t.account.currency = :currency
              and t.transactionDate between :from and :to
              and t.type in :types
            group by t.type
            """)
    List<TypeTotal> sumByType(@Param("userId") Long userId, @Param("currency") String currency,
                              @Param("from") LocalDate from, @Param("to") LocalDate to,
                              @Param("types") Collection<TransactionType> types);

    @Query("""
            select new com.nest.repository.CurrencyTypeTotal(t.account.currency, t.type, sum(t.amount))
            from Transaction t
            where t.account.user.id = :userId
              and t.transactionDate between :from and :to
              and t.type in :types
            group by t.account.currency, t.type
            """)
    List<CurrencyTypeTotal> sumByCurrencyAndType(@Param("userId") Long userId,
                                                 @Param("from") LocalDate from, @Param("to") LocalDate to,
                                                 @Param("types") Collection<TransactionType> types);

    @Query("""
            select new com.nest.repository.CategoryTotal(c.id, c.name, c.color, sum(t.amount))
            from Transaction t join t.category c
            where t.account.user.id = :userId
              and t.account.currency = :currency
              and t.transactionDate between :from and :to
              and t.type = :type
            group by c.id, c.name, c.color
            """)
    List<CategoryTotal> sumByCategory(@Param("userId") Long userId, @Param("currency") String currency,
                                      @Param("from") LocalDate from, @Param("to") LocalDate to,
                                      @Param("type") TransactionType type);

    @Query("""
            select new com.nest.repository.MonthlyTypeTotal(
                extract(year from t.transactionDate), extract(month from t.transactionDate), t.type, sum(t.amount))
            from Transaction t
            where t.account.user.id = :userId
              and t.account.currency = :currency
              and t.transactionDate between :from and :to
              and t.type in :types
            group by extract(year from t.transactionDate), extract(month from t.transactionDate), t.type
            """)
    List<MonthlyTypeTotal> sumByMonthAndType(@Param("userId") Long userId, @Param("currency") String currency,
                                             @Param("from") LocalDate from, @Param("to") LocalDate to,
                                             @Param("types") Collection<TransactionType> types);

    /** Generated occurrences of a user's recurring expenses since a date, newest first per rule. */
    @EntityGraph(attributePaths = {"recurringTransaction", "account"})
    @Query("""
            select t from Transaction t
            where t.recurringTransaction is not null
              and t.account.user.id = :userId
              and t.account.currency = :currency
              and t.type = com.nest.model.TransactionType.EXPENSE
              and t.occurrenceDate >= :since
            order by t.recurringTransaction.id, t.occurrenceDate desc
            """)
    List<Transaction> findRecurringExpenseOccurrences(@Param("userId") Long userId, @Param("currency") String currency,
                                                      @Param("since") LocalDate since);
}
