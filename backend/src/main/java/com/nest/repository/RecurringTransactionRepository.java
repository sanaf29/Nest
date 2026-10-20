package com.nest.repository;

import com.nest.model.RecurringTransaction;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RecurringTransactionRepository extends JpaRepository<RecurringTransaction, Long> {

    @EntityGraph(attributePaths = {"account", "category"})
    Optional<RecurringTransaction> findByIdAndAccountUserId(Long id, Long userId);

    @EntityGraph(attributePaths = {"account", "category"})
    List<RecurringTransaction> findByAccountUserIdOrderByStatusAscNextOccurrenceAscDescriptionAsc(Long userId);

    /** Ids only: each rule is then processed in its own transaction. */
    @Query("""
            select r.id from RecurringTransaction r
            where r.status = com.nest.model.RecurringStatus.ACTIVE and r.nextOccurrence <= :today
            order by r.nextOccurrence
            """)
    List<Long> findDueIds(@Param("today") LocalDate today);

    @Query("""
            select r.id from RecurringTransaction r
            where r.status = com.nest.model.RecurringStatus.ACTIVE and r.nextOccurrence <= :today
              and r.account.user.id = :userId
            order by r.nextOccurrence
            """)
    List<Long> findDueIdsForUser(@Param("userId") Long userId, @Param("today") LocalDate today);

    /** Active rules with a "due soon" reminder whose next occurrence falls within the window. */
    @EntityGraph(attributePaths = {"account", "account.user"})
    @Query("""
            select r from RecurringTransaction r
            where r.status = com.nest.model.RecurringStatus.ACTIVE and r.remindDaysBefore is not null
              and r.nextOccurrence between :from and :to
            """)
    List<RecurringTransaction> findWithRemindersDueBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
