package com.vynk.profile.repository;

import com.vynk.profile.entity.StudentProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long> {

    Optional<StudentProfile> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    @Query("SELECT p FROM StudentProfile p " +
           "JOIN FETCH p.user u " +
           "JOIN FETCH u.college c " +
           "WHERE c.id = :collegeId " +
           "AND u.id != :currentUserId " +
           "AND p.discoverable = true " +
           "AND p.onboardingCompleted = true " +
           "AND u.status = com.vynk.user.entity.AccountStatus.ACTIVE " +
           "AND u.emailVerified = true")
    List<StudentProfile> findDiscoverableStudentsInCollege(
            @Param("collegeId") Long collegeId,
            @Param("currentUserId") Long currentUserId);

    @Query("SELECT p FROM StudentProfile p WHERE p.user.id IN :userIds")
    List<StudentProfile> findByUserIdIn(@Param("userIds") java.util.Collection<Long> userIds);
}