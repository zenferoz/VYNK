package com.vynk.community.repository;

import com.vynk.community.entity.Community;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CommunityRepository extends JpaRepository<Community, Long> {

    Page<Community> findByCollegeIdAndIsActiveTrueOrderByCreatedAtDesc(Long collegeId, Pageable pageable);

    Optional<Community> findByIdAndCollegeIdAndIsActiveTrue(Long id, Long collegeId);

    @Query("SELECT COUNT(m) FROM CommunityMember m WHERE m.community.id = :communityId")
    long countMembersByCommunityId(@Param("communityId") Long communityId);
}
