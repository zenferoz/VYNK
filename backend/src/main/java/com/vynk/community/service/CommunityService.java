package com.vynk.community.service;

import com.vynk.college.entity.College;
import com.vynk.common.exception.ResourceNotFoundException;
import com.vynk.community.dto.*;
import com.vynk.community.entity.Community;
import com.vynk.community.entity.CommunityDiscussion;
import com.vynk.community.entity.CommunityMember;
import com.vynk.community.repository.CommunityDiscussionRepository;
import com.vynk.community.repository.CommunityMemberRepository;
import com.vynk.community.repository.CommunityRepository;
import com.vynk.profile.entity.StudentProfile;
import com.vynk.profile.repository.StudentProfileRepository;
import com.vynk.user.entity.User;
import com.vynk.safety.service.SafetyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommunityService {

    private final CommunityRepository communityRepository;
    private final CommunityMemberRepository memberRepository;
    private final CommunityDiscussionRepository discussionRepository;
    private final StudentProfileRepository profileRepository;
    private final SafetyService safetyService;

    public CommunityService(
            CommunityRepository communityRepository,
            CommunityMemberRepository memberRepository,
            CommunityDiscussionRepository discussionRepository,
            StudentProfileRepository profileRepository) {
        this(communityRepository, memberRepository, discussionRepository, profileRepository, null);
    }

    @Autowired
    public CommunityService(
            CommunityRepository communityRepository,
            CommunityMemberRepository memberRepository,
            CommunityDiscussionRepository discussionRepository,
            StudentProfileRepository profileRepository,
            @Autowired(required = false) SafetyService safetyService) {
        this.communityRepository = communityRepository;
        this.memberRepository = memberRepository;
        this.discussionRepository = discussionRepository;
        this.profileRepository = profileRepository;
        this.safetyService = safetyService;
    }

    @Transactional
    public CommunityResponse createCommunity(User currentUser, CreateCommunityRequest request) {
        College college = currentUser.getCollege();
        if (college == null) {
            throw new IllegalArgumentException("User must be affiliated with a college to create a community.");
        }

        Community community = new Community(
                request.getName().trim(),
                request.getDescription().trim(),
                request.getPurpose().trim(),
                college,
                currentUser
        );
        Community saved = communityRepository.save(community);

        // Creator automatically joins as CREATOR
        CommunityMember member = new CommunityMember(saved, currentUser, "CREATOR");
        memberRepository.save(member);

        return new CommunityResponse(saved, 1, true);
    }

    @Transactional(readOnly = true)
    public Page<CommunityResponse> getCommunities(User currentUser, int page, int size) {
        College college = currentUser.getCollege();
        if (college == null) {
            return Page.empty();
        }

        int boundedPage = Math.max(0, page);
        int boundedSize = Math.min(50, Math.max(1, size));

        return communityRepository.findByCollegeIdAndIsActiveTrueOrderByCreatedAtDesc(
                college.getId(),
                PageRequest.of(boundedPage, boundedSize)
        ).map(c -> {
            long count = communityRepository.countMembersByCommunityId(c.getId());
            boolean isMember = memberRepository.existsByCommunityIdAndUserId(c.getId(), currentUser.getId());
            return new CommunityResponse(c, count, isMember);
        });
    }

    @Transactional(readOnly = true)
    public CommunityResponse getCommunityDetail(User currentUser, Long communityId) {
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community not found."));

        // College isolation check
        if (!community.getCollege().getId().equals(currentUser.getCollege().getId())) {
            throw new AccessDeniedException("Cannot view communities from another college.");
        }

        long count = communityRepository.countMembersByCommunityId(community.getId());
        boolean isMember = memberRepository.existsByCommunityIdAndUserId(community.getId(), currentUser.getId());
        return new CommunityResponse(community, count, isMember);
    }

    @Transactional
    public void joinCommunity(User currentUser, Long communityId) {
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community not found."));

        if (!community.getCollege().getId().equals(currentUser.getCollege().getId())) {
            throw new AccessDeniedException("Cannot join communities from another college.");
        }

        if (memberRepository.existsByCommunityIdAndUserId(communityId, currentUser.getId())) {
            return; // Idempotent
        }

        CommunityMember member = new CommunityMember(community, currentUser, "MEMBER");
        memberRepository.save(member);
    }

    @Transactional
    public void leaveCommunity(User currentUser, Long communityId) {
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community not found."));

        if (community.getCreator().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Creator cannot leave the community. Deactivate the community instead.");
        }

        memberRepository.deleteByCommunityIdAndUserId(communityId, currentUser.getId());
    }

    @Transactional(readOnly = true)
    public Page<DiscussionResponse> getDiscussions(User currentUser, Long communityId, int page, int size) {
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community not found."));

        if (!community.getCollege().getId().equals(currentUser.getCollege().getId())) {
            throw new AccessDeniedException("Cannot access discussions from another college.");
        }

        if (!memberRepository.existsByCommunityIdAndUserId(communityId, currentUser.getId())) {
            throw new AccessDeniedException("You must be a member of this community to view discussions.");
        }

        int boundedPage = Math.max(0, page);
        int boundedSize = Math.min(50, Math.max(1, size));

        java.util.Set<Long> blockedIds = safetyService != null ? safetyService.getAllBlockedUserIdsFor(currentUser.getId()) : java.util.Collections.emptySet();

        Page<com.vynk.community.entity.CommunityDiscussion> discussions;
        if (blockedIds != null && !blockedIds.isEmpty()) {
            discussions = discussionRepository.findByCommunityIdAndAuthorIdNotInOrderByCreatedAtDesc(
                    communityId,
                    blockedIds,
                    PageRequest.of(boundedPage, boundedSize)
            );
        } else {
            discussions = discussionRepository.findByCommunityIdOrderByCreatedAtDesc(
                    communityId,
                    PageRequest.of(boundedPage, boundedSize)
            );
        }

        return discussions.map(d -> {
            StudentProfile profile = profileRepository.findByUserId(d.getAuthor().getId()).orElse(null);
            String authorName = profile != null ? profile.getFullName() : "Student #" + d.getAuthor().getId();
            String authorDept = profile != null ? profile.getDepartment() : null;
            return new DiscussionResponse(d, authorName, authorDept, currentUser.getId());
        });
    }

    @Transactional
    public DiscussionResponse createDiscussion(User currentUser, Long communityId, CreateDiscussionRequest request) {
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community not found."));

        if (!community.getCollege().getId().equals(currentUser.getCollege().getId())) {
            throw new AccessDeniedException("Cannot post in communities from another college.");
        }

        if (!memberRepository.existsByCommunityIdAndUserId(communityId, currentUser.getId())) {
            throw new AccessDeniedException("You must be a member of this community to post discussions.");
        }

        CommunityDiscussion discussion = new CommunityDiscussion(
                community,
                currentUser,
                request.getTitle().trim(),
                request.getContent().trim()
        );
        CommunityDiscussion saved = discussionRepository.save(discussion);

        StudentProfile profile = profileRepository.findByUserId(currentUser.getId()).orElse(null);
        String authorName = profile != null ? profile.getFullName() : "Student #" + currentUser.getId();
        String authorDept = profile != null ? profile.getDepartment() : null;

        return new DiscussionResponse(saved, authorName, authorDept, currentUser.getId());
    }

    @Transactional
    public void deleteDiscussion(User currentUser, Long communityId, Long discussionId) {
        CommunityDiscussion discussion = discussionRepository.findById(discussionId)
                .orElseThrow(() -> new ResourceNotFoundException("Discussion not found."));

        if (!discussion.getCommunity().getId().equals(communityId)) {
            throw new IllegalArgumentException("Discussion does not belong to this community.");
        }

        boolean isAuthor = discussion.getAuthor().getId().equals(currentUser.getId());
        boolean isCommunityCreator = discussion.getCommunity().getCreator().getId().equals(currentUser.getId());

        if (!isAuthor && !isCommunityCreator) {
            throw new AccessDeniedException("Not authorized to delete this discussion.");
        }

        discussionRepository.delete(discussion);
    }
}
