package com.nest.repository;

import com.nest.model.MonthlyBudget;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MonthlyBudgetRepository extends JpaRepository<MonthlyBudget, Long> {

    @EntityGraph(attributePaths = {"allocations", "allocations.category"})
    Optional<MonthlyBudget> findByIdAndUserId(Long id, Long userId);

    @EntityGraph(attributePaths = {"allocations", "allocations.category"})
    Optional<MonthlyBudget> findByUserIdAndYearAndMonthAndCurrency(Long userId, int year, int month, String currency);

    boolean existsByUserIdAndYearAndMonthAndCurrency(Long userId, int year, int month, String currency);

    List<MonthlyBudget> findByUserIdOrderByYearDescMonthDescCurrencyAsc(Long userId);
}
