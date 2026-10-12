package com.vynk;

import com.vynk.common.exception.ResourceNotFoundException;
import com.vynk.goal.dto.NetworkingGoalRequest;
import com.vynk.goal.dto.NetworkingGoalResponse;
import com.vynk.goal.entity.NetworkingGoal;
import com.vynk.goal.entity.NetworkingPurpose;
import com.vynk.goal.repository.NetworkingGoalRepository;
import com.vynk.goal.service.NetworkingGoalService;
import com.vynk.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class NetworkingGoalServiceTest {

    private NetworkingGoalRepository goalRepository;
    private NetworkingGoalService goalService;
    private User testUser;

    @BeforeEach
    void setUp() {
        goalRepository = Mockito.mock(NetworkingGoalRepository.class);
        goalService = new NetworkingGoalService(goalRepository);

        testUser = new User();
        try {
            var idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(testUser, 100L);
        } catch (Exception ignored) {}
    }

    @Test
    void shouldCreateGoalSuccessfully() {
        NetworkingGoalRequest req = new NetworkingGoalRequest();
        req.setPurpose(NetworkingPurpose.HACKATHON_TEAM);
        req.setDescription("Building AI project");
        req.setSkills(List.of("React", "Spring Boot"));
        req.setTargetYear(3);

        when(goalRepository.save(any(NetworkingGoal.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NetworkingGoalResponse response = goalService.createGoal(testUser, req);
        assertNotNull(response);
        assertEquals("HACKATHON_TEAM", response.getPurpose());
        assertEquals("Building AI project", response.getDescription());
        assertEquals(2, response.getSkills().size());
        assertTrue(response.isActive());
    }

    @Test
    void shouldGetGoalsForUserInOrder() {
        NetworkingGoal g1 = new NetworkingGoal();
        g1.setUser(testUser);
        g1.setPurpose(NetworkingPurpose.RESEARCH);
        g1.setCreatedAt(LocalDateTime.now());

        NetworkingGoal g2 = new NetworkingGoal();
        g2.setUser(testUser);
        g2.setPurpose(NetworkingPurpose.PROJECT_COLLABORATION);
        g2.setCreatedAt(LocalDateTime.now().minusDays(1));

        when(goalRepository.findByUserIdOrderByCreatedAtDesc(100L)).thenReturn(List.of(g1, g2));

        List<NetworkingGoalResponse> goals = goalService.getGoalsForUser(100L);
        assertEquals(2, goals.size());
        assertEquals("RESEARCH", goals.get(0).getPurpose());
        assertEquals("PROJECT_COLLABORATION", goals.get(1).getPurpose());
    }

    @Test
    void shouldUpdateGoalWhenOwned() {
        NetworkingGoal existing = new NetworkingGoal();
        existing.setUser(testUser);
        existing.setPurpose(NetworkingPurpose.STUDY_PARTNER);
        existing.setDescription("Initial description");
        existing.setTargetYear(1);
        existing.setActive(true);

        when(goalRepository.findByIdAndUserId(5L, 100L)).thenReturn(Optional.of(existing));
        when(goalRepository.save(any(NetworkingGoal.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NetworkingGoalRequest updateReq = new NetworkingGoalRequest();
        updateReq.setPurpose(NetworkingPurpose.PLACEMENT_PREP);
        updateReq.setDescription("Updated to interview prep");
        updateReq.setTargetYear(4);
        updateReq.setSkills(List.of("Java", "Algorithms"));

        NetworkingGoalResponse updated = goalService.updateGoal(testUser, 5L, updateReq);
        assertEquals("PLACEMENT_PREP", updated.getPurpose());
        assertEquals("Updated to interview prep", updated.getDescription());
        assertEquals(4, updated.getTargetYear());
        assertTrue(updated.getSkills().contains("Java"));
        assertTrue(updated.getSkills().contains("Algorithms"));
    }

    @Test
    void shouldThrowWhenUpdatingUnownedOrMissingGoal() {
        when(goalRepository.findByIdAndUserId(99L, 100L)).thenReturn(Optional.empty());

        NetworkingGoalRequest updateReq = new NetworkingGoalRequest();
        updateReq.setPurpose(NetworkingPurpose.CASUAL_NETWORKING);

        assertThrows(ResourceNotFoundException.class, () -> goalService.updateGoal(testUser, 99L, updateReq));
    }

    @Test
    void shouldNormalizeAndLimitSkillsOnCreation() {
        List<String> rawSkills = new ArrayList<>();
        // Add 20 skills (should cap at 15)
        for (int i = 1; i <= 20; i++) {
            rawSkills.add("Skill " + i);
        }
        // Add duplicate and blank skills
        rawSkills.add("Skill 1");
        rawSkills.add("   ");
        rawSkills.add("A very long skill name that exceeds the maximum length of fifty characters allowed in the database schema");

        NetworkingGoalRequest req = new NetworkingGoalRequest();
        req.setPurpose(NetworkingPurpose.PROJECT_COLLABORATION);
        req.setSkills(rawSkills);

        when(goalRepository.save(any(NetworkingGoal.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NetworkingGoalResponse response = goalService.createGoal(testUser, req);
        assertEquals(15, response.getSkills().size());
        for (String skill : response.getSkills()) {
            assertTrue(skill.length() <= 50);
            assertFalse(skill.isBlank());
        }
    }

    @Test
    void shouldDeactivateOnlyOwnedGoal() {
        NetworkingGoal goal = new NetworkingGoal();
        goal.setUser(testUser);
        goal.setPurpose(NetworkingPurpose.STUDY_PARTNER);
        goal.setDescription("DSA prep");
        goal.setSkills(Set.of("Java"));
        goal.setTargetYear(2);
        goal.setActive(true);

        when(goalRepository.findByIdAndUserId(1L, 100L)).thenReturn(Optional.of(goal));

        goalService.deactivateGoal(testUser, 1L);
        assertFalse(goal.isActive());

        User otherUser = new User();
        try {
            var idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(otherUser, 200L);
        } catch (Exception ignored) {}

        when(goalRepository.findByIdAndUserId(1L, 200L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> goalService.deactivateGoal(otherUser, 1L));
    }
}
