package com.nest.repository;

import com.nest.model.FinancialAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Every finder is scoped by owner. There is deliberately no unscoped lookup used by services,
 * so a forgotten ownership check cannot leak another user's account.
 */
public interface FinancialAccountRepository extends JpaRepository<FinancialAccount, Long> {

    Optional<FinancialAccount> findByIdAndUserId(Long id, Long userId);

    List<FinancialAccount> findByUserIdOrderByNameAsc(Long userId);

    List<FinancialAccount> findByUserIdAndActiveTrueOrderByNameAsc(Long userId);

    boolean existsByUserIdAndNameIgnoreCaseAndActiveTrue(Long userId, String name);

    boolean existsByUserIdAndNameIgnoreCaseAndActiveTrueAndIdNot(Long userId, String name, Long excludedId);
}
