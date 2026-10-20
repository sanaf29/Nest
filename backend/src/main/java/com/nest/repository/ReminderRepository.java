package com.nest.repository;

import com.nest.model.Reminder;
import com.nest.model.ReminderStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ReminderRepository extends JpaRepository<Reminder, Long> {

    @EntityGraph(attributePaths = {"savingsGoal", "category"})
    Optional<Reminder> findByIdAndUserId(Long id, Long userId);

    @EntityGraph(attributePaths = {"savingsGoal", "category"})
    List<Reminder> findByUserIdOrderByStatusDescCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = {"savingsGoal", "category"})
    List<Reminder> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, ReminderStatus status);

    /** Timed reminders whose notification time has come (uses the indexed, computed remind_at column). */
    @Query("""
            select r.id from Reminder r
            where r.status = com.nest.model.ReminderStatus.SCHEDULED and r.remindAtColumn <= :now
            order by r.remindAtColumn
            """)
    List<Long> findDueIds(@Param("now") Instant now);

    @EntityGraph(attributePaths = {"user", "category"})
    @Query("""
            select r from Reminder r
            where r.status = com.nest.model.ReminderStatus.SCHEDULED
              and r.type = com.nest.model.ReminderType.BUDGET_THRESHOLD
            """)
    List<Reminder> findActiveBudgetThresholds();
}
