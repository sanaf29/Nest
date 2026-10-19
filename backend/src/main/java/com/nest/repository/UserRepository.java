package com.nest.repository;

import com.nest.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Expects emails already normalised with {@link User#normalizeEmail(String)}. */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
