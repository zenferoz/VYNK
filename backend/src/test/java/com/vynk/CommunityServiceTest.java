package com.vynk;

import com.vynk.college.entity.College;
import com.vynk.community.dto.CreateCommunityRequest;
import com.vynk.community.dto.CreateDiscussionRequest;
import com.vynk.community.entity.Community;
import com.vynk.community.entity.CommunityDiscussion;
import com.vynk.community.entity.CommunityMember;
import com.vynk.community.repository.CommunityDiscussionRepository;
import com.vynk.community.repository.CommunityMemberRepository;
import com.vynk.community.repository.CommunityRepository;
import com.vynk.community.service.CommunityService;
import com.vynk.profile.repository.StudentProfileRepository;
import com.vynk.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CommunityServiceTest {

    private CommunityRepository communityRepository;
    private CommunityMemberRepository memberRepository;
    private CommunityDiscussionRepository discussionRepository;
    private StudentProfileRepository profileRepository;
    private com.vynk.safety.service.SafetyService safetyService;
    private CommunityService communityService;

    private College college1;
    private College college2;
    private User student1;
    private User student2;

    @BeforeEach
    void setUp() throws Exception {
        communityRepository = mock(CommunityRepository.class);
        memberRepository = mock(CommunityMemberRepository.class);
        discussionRepository = mock(CommunityDiscussionRepository.class);
        profileRepository = mock(StudentProfileRepository.class);
        safetyService = mock(com.vynk.safety.service.SafetyService.class);

        communityService = new CommunityService(
                communityRepository,
                memberRepository,
                discussionRepository,
                profileRepository,
                safetyService
        );

        college1 = new College();
        setField(college1, "id", 1L);
        college1.setName("College One");

        college2 = new College();
        setField(college2, "id", 2L);
        college2.setName("College Two");

        student1 = new User();
        setField(student1, "id", 10L);
        student1.setCollege(college1);

        student2 = new User();
        setField(student2, "id", 20L);
        student2.setCollege(college2);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void createsCommunityAndAutoJoinsAsCreator() throws Exception {
        when(communityRepository.save(any(Community.class))).thenAnswer(i -> {
            Community c = i.getArgument(0);
            setField(c, "id", 50L);
            return c;
        });

        CreateCommunityRequest req = new CreateCommunityRequest("Hackathon Club", "For coders", "HACKATHONS");
        var res = communityService.createCommunity(student1, req);

        assertNotNull(res);
        assertEquals(50L, res.getId());
        assertEquals("Hackathon Club", res.getName());
        assertTrue(res.isMember());
        verify(memberRepository, times(1)).save(any(CommunityMember.class));
    }

    @Test
    void rejectsCrossCollegeCommunityAccess() throws Exception {
        Community c = new Community("C1 Club", "desc", "PURPOSE", college1, student1);
        setField(c, "id", 100L);
        when(communityRepository.findById(100L)).thenReturn(Optional.of(c));

        // student2 belongs to college2
        assertThrows(AccessDeniedException.class, () ->
                communityService.getCommunityDetail(student2, 100L)
        );
    }

    @Test
    void rejectsDiscussionPostByNonMember() throws Exception {
        Community c = new Community("C1 Club", "desc", "PURPOSE", college1, student1);
        setField(c, "id", 100L);
        when(communityRepository.findById(100L)).thenReturn(Optional.of(c));
        when(memberRepository.existsByCommunityIdAndUserId(100L, student1.getId())).thenReturn(false);

        CreateDiscussionRequest req = new CreateDiscussionRequest("Topic", "Content text");
        assertThrows(AccessDeniedException.class, () ->
                communityService.createDiscussion(student1, 100L, req)
        );
    }

    @Test
    void allowsDiscussionPostByMember() throws Exception {
        Community c = new Community("C1 Club", "desc", "PURPOSE", college1, student1);
        setField(c, "id", 100L);
        when(communityRepository.findById(100L)).thenReturn(Optional.of(c));
        when(memberRepository.existsByCommunityIdAndUserId(100L, student1.getId())).thenReturn(true);
        when(discussionRepository.save(any(CommunityDiscussion.class))).thenAnswer(i -> {
            CommunityDiscussion d = i.getArgument(0);
            setField(d, "id", 200L);
            return d;
        });

        CreateDiscussionRequest req = new CreateDiscussionRequest("Topic", "Content text");
        var res = communityService.createDiscussion(student1, 100L, req);

        assertNotNull(res);
        assertEquals(200L, res.getId());
        assertEquals("Topic", res.getTitle());
    }

    @Test
    void filtersDiscussionsAuthoredByBlockedUsers() throws Exception {
        Community c = new Community("C1 Club", "desc", "PURPOSE", college1, student1);
        setField(c, "id", 100L);
        when(communityRepository.findById(100L)).thenReturn(Optional.of(c));
        when(memberRepository.existsByCommunityIdAndUserId(100L, student1.getId())).thenReturn(true);
        when(safetyService.getAllBlockedUserIdsFor(student1.getId())).thenReturn(java.util.Set.of(99L));

        CommunityDiscussion d = new CommunityDiscussion(c, student1, "Safe topic", "Safe content");
        setField(d, "id", 301L);
        org.springframework.data.domain.Page<CommunityDiscussion> page = new org.springframework.data.domain.PageImpl<>(java.util.List.of(d));

        when(discussionRepository.findByCommunityIdAndAuthorIdNotInOrderByCreatedAtDesc(
                eq(100L), eq(java.util.Set.of(99L)), any()
        )).thenReturn(page);

        var result = communityService.getDiscussions(student1, 100L, 0, 10);
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(301L, result.getContent().get(0).getId());

        verify(discussionRepository).findByCommunityIdAndAuthorIdNotInOrderByCreatedAtDesc(
                eq(100L), eq(java.util.Set.of(99L)), any()
        );
    }
}
