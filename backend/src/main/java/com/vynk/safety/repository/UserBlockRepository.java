package com.vynk.safety.repository;

import com.vynk.safety.entity.UserBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {

    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    @Query("SELECT COUNT(b) > 0 FROM UserBlock b WHERE (b.blocker.id = :u1 AND b.blocked.id = :u2) OR (b.blocker.id = :u2 AND b.blocked.id = :u1)")
    boolean existsBlockBetween(@Param("u1") Long u1, @Param("u2") Long u2);

    Optional<UserBlock> findByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    List<UserBlock> findByBlockerId(Long blockerId);

    @Query("SELECT b.blocked.id FROM UserBlock b WHERE b.blocker.id = :userId")
    Set<Long> findBlockedUserIdsByBlockerId(@Param("userId") Long userId);

    @Query("SELECT b.blocker.id FROM UserBlock b WHERE b.blocked.id = :userId")
    Set<Long> findBlockerUserIdsByBlockedId(@Param("userId") Long userId);
}
