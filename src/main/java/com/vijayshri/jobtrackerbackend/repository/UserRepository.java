package com.vijayshri.jobtrackerbackend.repository;

import com.vijayshri.jobtrackerbackend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    /** True once at least one real account (with a password) exists. */
    boolean existsByPasswordHashIsNotNull();
}
