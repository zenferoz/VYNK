
package com.vynk.auth.repository;

import com.vynk.auth.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EmailVerificationTokenRepository
        extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);

    Optional<EmailVerificationToken> findByUserId(Long userId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM EmailVerificationToken t WHERE t.user.id = :userId")
    Optional<EmailVerificationToken> findByUserIdForUpdate(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE EmailVerificationToken t SET t.used = true WHERE t.tokenHash = :tokenHash AND t.used = false AND t.expiresAt > :now")
    int markTokenUsed(@Param("tokenHash") String tokenHash, @Param("now") LocalDateTime now);
}
