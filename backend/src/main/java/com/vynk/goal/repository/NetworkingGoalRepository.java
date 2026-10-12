package com.vynk.goal.repository;

import com.vynk.goal.entity.NetworkingGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NetworkingGoalRepository extends JpaRepository<NetworkingGoal, Long> {

    List<NetworkingGoal> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT g FROM NetworkingGoal g WHERE g.user.id = :userId AND g.active = true AND (g.expiresAt IS NULL OR g.expiresAt > CURRENT_TIMESTAMP)")
    List<NetworkingGoal> findActiveGoalsByUserId(@Param("userId") Long userId);

    @Query("SELECT g FROM NetworkingGoal g WHERE g.user.id IN :userIds AND g.active = true AND (g.expiresAt IS NULL OR g.expiresAt > CURRENT_TIMESTAMP)")
    List<NetworkingGoal> findActiveGoalsByUserIds(@Param("userIds") List<Long> userIds);

    Optional<NetworkingGoal> findByIdAndUserId(Long id, Long userId);
}
