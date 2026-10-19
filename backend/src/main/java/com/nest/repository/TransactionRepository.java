package com.nest.repository;

import com.nest.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
