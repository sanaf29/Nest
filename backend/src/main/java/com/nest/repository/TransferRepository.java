package com.nest.repository;

import com.nest.model.Transfer;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TransferRepository extends JpaRepository<Transfer, Long> {

    /**
     * A transfer belongs to the owner of its accounts. Both entries always share an owner,
     * which {@code Transfer.between} enforces, so checking one is sufficient.
     */
    @EntityGraph(attributePaths = {"legs", "legs.account"})
    @Query("""
            select t from Transfer t
            where t.id = :id
              and exists (select 1 from Transaction leg where leg.transfer = t and leg.account.user.id = :userId)
            """)
    Optional<Transfer> findOwned(@Param("id") Long id, @Param("userId") Long userId);
}
