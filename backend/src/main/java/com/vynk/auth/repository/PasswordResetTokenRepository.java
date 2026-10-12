package com.vynk.auth.repository;

import com.vynk.auth.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    Optional<PasswordResetToken> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM PasswordResetToken t WHERE t.user.id = :userId")
    Optional<PasswordResetToken> findByUserIdForUpdate(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE PasswordResetToken t SET t.used = true WHERE t.tokenHash = :tokenHash AND t.used = false AND t.expiresAt > :now")
    int markTokenUsed(@Param("tokenHash") String tokenHash, @Param("now") LocalDateTime now);
}
