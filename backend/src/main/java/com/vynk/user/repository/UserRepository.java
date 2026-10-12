package com.vynk.user.repository;

import com.vynk.user.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = {"college"})
    Optional<User> findByEmail(String email);

    @EntityGraph(attributePaths = {"college"})
    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmail(String email);
}