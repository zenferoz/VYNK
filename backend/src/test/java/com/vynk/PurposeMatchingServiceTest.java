package com.vynk;

import com.vynk.college.entity.College;
import com.vynk.goal.entity.NetworkingGoal;
import com.vynk.goal.entity.NetworkingPurpose;
import com.vynk.goal.repository.NetworkingGoalRepository;
import com.vynk.matching.dto.StudentMatchResponse;
import com.vynk.matching.service.PurposeMatchingService;
import com.vynk.profile.entity.StudentProfile;
import com.vynk.profile.repository.StudentProfileRepository;
import com.vynk.safety.service.SafetyService;
import com.vynk.user.entity.AccountStatus;
import com.vynk.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class PurposeMatchingServiceTest {

    private StudentProfileRepository profileRepository;
    private NetworkingGoalRepository goalRepository;
    private SafetyService safetyService;
    private PurposeMatchingService matchingService;

    private College college;
    private User currentUser;
    private StudentProfile currentProfile;

    @BeforeEach
    void setUp() throws Exception {
        profileRepository = Mockito.mock(StudentProfileRepository.class);
        goalRepository = Mockito.mock(NetworkingGoalRepository.class);
        safetyService = Mockito.mock(SafetyService.class);
        matchingService = new PurposeMatchingService(profileRepository, goalRepository, safetyService);

        college = new College();
        setField(college, "id", 1L);
        college.setName("Test College");

        currentUser = new User();
        setField(currentUser, "id", 10L);
        currentUser.setCollege(college);
        currentUser.setStatus(AccountStatus.ACTIVE);
        currentUser.setEmailVerified(true);

        currentProfile = new StudentProfile();
        currentProfile.setUser(currentUser);
        currentProfile.setFullName("Current User");
        currentProfile.setDepartment("CSE");
        currentProfile.setCourse("B.Tech");
        currentProfile.setCurrentYear(3);
        currentProfile.setBio("Hello");
        currentProfile.setSkills(new HashSet<>(List.of("java", "react")));
        currentProfile.setInterests(new HashSet<>(List.of("ai", "web")));
        currentProfile.setAvailability("Weekends and evenings");
        currentProfile.setOnboardingCompleted(true);
        currentProfile.setDiscoverable(true);

        when(profileRepository.findByUserId(10L)).thenReturn(Optional.of(currentProfile));
        when(safetyService.getAllBlockedUserIdsFor(10L)).thenReturn(Collections.emptySet());
    }

    @Test
    void shouldCalculateAccurateExplainableScores() throws Exception {
        // Candidate 1: shares purpose, skills, dept, year, and availability -> 40 + 20 + 20 + 10 = 90
        User cand1User = new User();
        setField(cand1User, "id", 20L);
        cand1User.setCollege(college);
        cand1User.setStatus(AccountStatus.ACTIVE);
        cand1User.setEmailVerified(true);

        StudentProfile cand1Profile = new StudentProfile();
        cand1Profile.setUser(cand1User);
        cand1Profile.setFullName("Candidate One");
        cand1Profile.setDepartment("CSE");
        cand1Profile.setCourse("B.Tech");
        cand1Profile.setCurrentYear(3);
        cand1Profile.setBio("Bio 1");
        cand1Profile.setSkills(new HashSet<>(List.of("java", "docker")));
        cand1Profile.setInterests(new HashSet<>(List.of("ai")));
        cand1Profile.setAvailability("Free on weekends");
        cand1Profile.setOnboardingCompleted(true);
        cand1Profile.setDiscoverable(true);

        // Candidate 2: different dept, no availability, only 1 shared skill -> 0 purpose + 10 skill + 0 dept = 10
        User cand2User = new User();
        setField(cand2User, "id", 30L);
        cand2User.setCollege(college);
        cand2User.setStatus(AccountStatus.ACTIVE);
        cand2User.setEmailVerified(true);

        StudentProfile cand2Profile = new StudentProfile();
        cand2Profile.setUser(cand2User);
        cand2Profile.setFullName("Candidate Two");
        cand2Profile.setDepartment("ECE");
        cand2Profile.setCourse("B.Tech");
        cand2Profile.setCurrentYear(1);
        cand2Profile.setBio("Bio 2");
        cand2Profile.setSkills(new HashSet<>(List.of("react")));
        cand2Profile.setInterests(new HashSet<>());
        cand2Profile.setAvailability(null);
        cand2Profile.setOnboardingCompleted(true);
        cand2Profile.setDiscoverable(true);

        when(profileRepository.findDiscoverableStudentsInCollege(1L, 10L))
                .thenReturn(List.of(cand1Profile, cand2Profile));

        NetworkingGoal userGoal = new NetworkingGoal();
        userGoal.setUser(currentUser);
        userGoal.setPurpose(NetworkingPurpose.HACKATHON_TEAM);
        userGoal.setDescription("Win hackathon");
        userGoal.setSkills(Set.of("react"));
        userGoal.setTargetYear(3);
        userGoal.setActive(true);

        when(goalRepository.findActiveGoalsByUserId(10L)).thenReturn(List.of(userGoal));

        NetworkingGoal cand1Goal = new NetworkingGoal();
        cand1Goal.setUser(cand1User);
        cand1Goal.setPurpose(NetworkingPurpose.HACKATHON_TEAM);
        cand1Goal.setDescription("Team hunt");
        cand1Goal.setSkills(Set.of("java"));
        cand1Goal.setTargetYear(3);
        cand1Goal.setActive(true);

        when(goalRepository.findActiveGoalsByUserIds(List.of(20L, 30L))).thenReturn(List.of(cand1Goal));

        List<StudentMatchResponse> matches = matchingService.getTopMatches(currentUser);

        assertNotNull(matches);
        assertEquals(2, matches.size());

        // First candidate should have higher score and be ranked first
        StudentMatchResponse topMatch = matches.get(0);
        assertEquals(20L, topMatch.getUserId());
        assertTrue(topMatch.getMatchScore() >= 80);
        assertTrue(topMatch.getMatchReasons().stream().anyMatch(r -> r.contains("Shared purpose")));
        assertTrue(topMatch.getMatchReasons().stream().anyMatch(r -> r.contains("Same department")));
        assertTrue(topMatch.getMatchReasons().stream().anyMatch(r -> r.contains("Same academic year")));

        // Second candidate
        StudentMatchResponse secondMatch = matches.get(1);
        assertEquals(30L, secondMatch.getUserId());
        assertTrue(secondMatch.getMatchScore() <= 20);
    }

    @Test
    void shouldExcludeBlockedUsersAndCapAtTop10() throws Exception {
        List<StudentProfile> candidates = new ArrayList<>();

        for (int i = 1; i <= 15; i++) {
            User candUser = new User();
            setField(candUser, "id", (long) (100 + i));
            candUser.setCollege(college);
            candUser.setStatus(AccountStatus.ACTIVE);
            candUser.setEmailVerified(true);

            StudentProfile cp = new StudentProfile();
            cp.setUser(candUser);
            cp.setFullName("Student " + i);
            cp.setDepartment("CSE");
            cp.setCourse("B.Tech");
            cp.setCurrentYear(3);
            cp.setBio("Bio");
            cp.setSkills(new HashSet<>(List.of("java")));
            cp.setInterests(new HashSet<>());
            cp.setOnboardingCompleted(true);
            cp.setDiscoverable(true);

            candidates.add(cp);
        }

        // Suppose user 101 is blocked
        when(safetyService.getAllBlockedUserIdsFor(10L)).thenReturn(Set.of(101L));
        when(profileRepository.findDiscoverableStudentsInCollege(1L, 10L)).thenReturn(candidates);
        when(goalRepository.findActiveGoalsByUserId(10L)).thenReturn(Collections.emptyList());
        when(goalRepository.findActiveGoalsByUserIds(any())).thenReturn(Collections.emptyList());

        List<StudentMatchResponse> matches = matchingService.getTopMatches(currentUser);

        // Max recommendations is 10
        assertTrue(matches.size() <= 10);
        // Excluded blocked user
        assertFalse(matches.stream().anyMatch(m -> m.getUserId().equals(101L)));
    }

    @Test
    void shouldReturnEmptyWhenUserHasNoCollegeOrIncompleteOnboarding() {
        User userWithoutCollege = new User();
        assertTrue(matchingService.getTopMatches(userWithoutCollege).isEmpty());

        currentProfile.setOnboardingCompleted(false);
        assertTrue(matchingService.getTopMatches(currentUser).isEmpty());
    }

    @Test
    void shouldSortDeterministicallyByScoreDescThenUserIdAsc() throws Exception {
        // Create 3 candidates with equal score (same department + same year = 20 pts)
        List<StudentProfile> candidates = new ArrayList<>();
        long[] userIds = {500L, 200L, 300L};

        for (long uid : userIds) {
            User candUser = new User();
            setField(candUser, "id", uid);
            candUser.setCollege(college);
            candUser.setStatus(AccountStatus.ACTIVE);
            candUser.setEmailVerified(true);

            StudentProfile cp = new StudentProfile();
            cp.setUser(candUser);
            cp.setFullName("Student " + uid);
            cp.setDepartment("CSE"); // +12 pts
            cp.setCourse("B.Tech");
            cp.setCurrentYear(3);    // +8 pts (same year diff 0)
            cp.setSkills(new HashSet<>());
            cp.setInterests(new HashSet<>());
            cp.setOnboardingCompleted(true);
            cp.setDiscoverable(true);

            candidates.add(cp);
        }

        when(profileRepository.findDiscoverableStudentsInCollege(1L, 10L)).thenReturn(candidates);
        when(goalRepository.findActiveGoalsByUserId(10L)).thenReturn(Collections.emptyList());
        when(goalRepository.findActiveGoalsByUserIds(any())).thenReturn(Collections.emptyList());

        List<StudentMatchResponse> matches = matchingService.getTopMatches(currentUser);
        assertEquals(3, matches.size());
        // All have score 20, so they must be tie-broken by userId ASC: 200, 300, 500
        assertEquals(200L, matches.get(0).getUserId());
        assertEquals(300L, matches.get(1).getUserId());
        assertEquals(500L, matches.get(2).getUserId());
    }

    @Test
    void shouldSafelyHandleCandidatesWithZeroScoreAndEmptyProfile() throws Exception {
        User candUser = new User();
        setField(candUser, "id", 999L);
        candUser.setCollege(college);
        candUser.setStatus(AccountStatus.ACTIVE);
        candUser.setEmailVerified(true);

        StudentProfile cp = new StudentProfile();
        cp.setUser(candUser);
        cp.setFullName("Zero Match");
        cp.setDepartment("Completely Different");
        cp.setCourse("Different");
        cp.setCurrentYear(6); // diff 3 => 0 points
        cp.setSkills(new HashSet<>());
        cp.setInterests(new HashSet<>());
        cp.setOnboardingCompleted(true);
        cp.setDiscoverable(true);

        when(profileRepository.findDiscoverableStudentsInCollege(1L, 10L)).thenReturn(List.of(cp));
        when(goalRepository.findActiveGoalsByUserId(10L)).thenReturn(Collections.emptyList());
        when(goalRepository.findActiveGoalsByUserIds(any())).thenReturn(Collections.emptyList());

        List<StudentMatchResponse> matches = matchingService.getTopMatches(currentUser);
        // Candidate with score 0 is excluded
        assertTrue(matches.isEmpty());
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
