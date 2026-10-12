package com.vynk.auth.repository;

import com.vynk.auth.entity.EmailVerificationOtp;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EmailVerificationOtpRepository
        extends JpaRepository<EmailVerificationOtp, Long> {

    Optional<EmailVerificationOtp> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM EmailVerificationOtp o WHERE o.user.id = :userId")
    Optional<EmailVerificationOtp> findByUserIdForUpdate(@Param("userId") Long userId);
}
