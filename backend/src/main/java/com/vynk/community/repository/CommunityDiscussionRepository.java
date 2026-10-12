package com.vynk.community.repository;

import com.vynk.community.entity.CommunityDiscussion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

public interface CommunityDiscussionRepository extends JpaRepository<CommunityDiscussion, Long> {

    Page<CommunityDiscussion> findByCommunityIdOrderByCreatedAtDesc(Long communityId, Pageable pageable);

    @Query("SELECT d FROM CommunityDiscussion d WHERE d.community.id = :communityId AND d.author.id NOT IN :blockedIds ORDER BY d.createdAt DESC")
    Page<CommunityDiscussion> findByCommunityIdAndAuthorIdNotInOrderByCreatedAtDesc(
            @Param("communityId") Long communityId,
            @Param("blockedIds") Collection<Long> blockedIds,
            Pageable pageable
    );
}
