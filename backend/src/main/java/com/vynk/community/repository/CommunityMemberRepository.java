package com.vynk.community.repository;

import com.vynk.community.entity.CommunityMember;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CommunityMemberRepository extends JpaRepository<CommunityMember, Long> {

    Optional<CommunityMember> findByCommunityIdAndUserId(Long communityId, Long userId);

    boolean existsByCommunityIdAndUserId(Long communityId, Long userId);

    Page<CommunityMember> findByCommunityIdOrderByJoinedAtAsc(Long communityId, Pageable pageable);

    void deleteByCommunityIdAndUserId(Long communityId, Long userId);
}
