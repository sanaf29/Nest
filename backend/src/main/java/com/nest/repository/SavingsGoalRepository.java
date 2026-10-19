package com.nest.repository;

import com.nest.model.GoalStatus;
import com.nest.model.SavingsGoal;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, Long> {

    @EntityGraph(attributePaths = "contributions")
    Optional<SavingsGoal> findByIdAndUserId(Long id, Long userId);

    /** Active goals first ("ACTIVE" sorts after "ACHIEVED"), then by deadline. */
    @EntityGraph(attributePaths = "contributions")
    List<SavingsGoal> findByUserIdOrderByStatusDescTargetDateAscNameAsc(Long userId);

    @EntityGraph(attributePaths = "contributions")
    List<SavingsGoal> findByUserIdAndStatusOrderByTargetDateAscNameAsc(Long userId, GoalStatus status);

    boolean existsByUserIdAndNameIgnoreCase(Long userId, String name);

    boolean existsByUserIdAndNameIgnoreCaseAndIdNot(Long userId, String name, Long excludedId);
}
