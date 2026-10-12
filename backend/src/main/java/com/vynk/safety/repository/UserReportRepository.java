package com.vynk.safety.repository;

import com.vynk.safety.entity.UserReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface UserReportRepository extends JpaRepository<UserReport, Long> {

    boolean existsByReporterIdAndReportedUserIdAndCreatedAtAfter(
            Long reporterId,
            Long reportedUserId,
            LocalDateTime after
    );
}
