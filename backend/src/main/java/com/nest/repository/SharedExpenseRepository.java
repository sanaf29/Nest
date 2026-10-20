package com.nest.repository;

import com.nest.model.SharedExpense;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SharedExpenseRepository extends JpaRepository<SharedExpense, Long> {

    @EntityGraph(attributePaths = "participants")
    Optional<SharedExpense> findByIdAndUserId(Long id, Long userId);

    @EntityGraph(attributePaths = "participants")
    List<SharedExpense> findByUserIdOrderByExpenseDateDescIdDesc(Long userId);
}
