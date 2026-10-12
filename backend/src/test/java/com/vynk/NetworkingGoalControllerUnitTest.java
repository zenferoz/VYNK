package com.vynk;

import com.vynk.common.exception.GlobalExceptionHandler;
import com.vynk.common.exception.ResourceNotFoundException;
import com.vynk.goal.controller.NetworkingGoalController;
import com.vynk.goal.dto.NetworkingGoalRequest;
import com.vynk.goal.dto.NetworkingGoalResponse;
import com.vynk.goal.entity.NetworkingGoal;
import com.vynk.goal.entity.NetworkingPurpose;
import com.vynk.goal.service.NetworkingGoalService;
import com.vynk.user.entity.User;
import com.vynk.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Field;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NetworkingGoalControllerUnitTest {

    private MockMvc mockMvc;
    private NetworkingGoalService goalService;
    private UserRepository userRepository;

    private User testUser;
    private Principal principal;

    @BeforeEach
    void setUp() throws Exception {
        goalService = Mockito.mock(NetworkingGoalService.class);
        userRepository = Mockito.mock(UserRepository.class);

        NetworkingGoalController controller = new NetworkingGoalController(goalService, userRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        testUser = new User();
        Field idField = User.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(testUser, 100L);
        testUser.setEmail("student100@college.edu");

        principal = new UsernamePasswordAuthenticationToken("student100@college.edu", "password");
        when(userRepository.findByEmail("student100@college.edu")).thenReturn(Optional.of(testUser));
    }

    @Test
    void shouldGetMyGoalsSuccessfully() throws Exception {
        NetworkingGoal goal = new NetworkingGoal();
        goal.setUser(testUser);
        goal.setPurpose(NetworkingPurpose.HACKATHON_TEAM);
        goal.setDescription("Looking for frontend dev");
        goal.setSkills(Set.of("React", "Tailwind"));
        goal.setTargetYear(3);
        goal.setActive(true);
        goal.setCreatedAt(LocalDateTime.now());

        NetworkingGoalResponse resp = new NetworkingGoalResponse(goal);
        when(goalService.getGoalsForUser(100L)).thenReturn(List.of(resp));

        mockMvc.perform(get("/api/goals/me")
                        .principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].purpose").value("HACKATHON_TEAM"))
                .andExpect(jsonPath("$[0].purposeDisplayName").value("Hackathon Team"))
                .andExpect(jsonPath("$[0].description").value("Looking for frontend dev"))
                .andExpect(jsonPath("$[0].targetYear").value(3))
                .andExpect(jsonPath("$[0].active").value(true));
    }

    @Test
    void shouldCreateGoalWithValidPayload() throws Exception {
        NetworkingGoal goal = new NetworkingGoal();
        goal.setUser(testUser);
        goal.setPurpose(NetworkingPurpose.PROJECT_COLLABORATION);
        goal.setDescription("Building microservices");
        goal.setSkills(Set.of("Java", "Spring Boot"));
        goal.setTargetYear(4);
        goal.setActive(true);
        goal.setCreatedAt(LocalDateTime.now());

        when(goalService.createGoal(eq(testUser), any(NetworkingGoalRequest.class)))
                .thenReturn(new NetworkingGoalResponse(goal));

        String requestJson = """
                {
                  "purpose": "PROJECT_COLLABORATION",
                  "description": "Building microservices",
                  "skills": ["Java", "Spring Boot"],
                  "targetYear": 4,
                  "active": true
                }
                """;

        mockMvc.perform(post("/api/goals")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.purpose").value("PROJECT_COLLABORATION"))
                .andExpect(jsonPath("$.purposeDisplayName").value("Project Collaboration"))
                .andExpect(jsonPath("$.targetYear").value(4));
    }

    @Test
    void shouldRejectGoalCreationWhenPurposeIsMissing() throws Exception {
        String requestJson = """
                {
                  "description": "No purpose provided",
                  "skills": ["Java"]
                }
                """;

        mockMvc.perform(post("/api/goals")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.purpose").exists());
    }

    @Test
    void shouldRejectGoalCreationWhenTargetYearIsInvalid() throws Exception {
        String requestJson = """
                {
                  "purpose": "STUDY_PARTNER",
                  "targetYear": 9
                }
                """;

        mockMvc.perform(post("/api/goals")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.targetYear").exists());
    }

    @Test
    void shouldUpdateGoalWhenOwned() throws Exception {
        NetworkingGoal goal = new NetworkingGoal();
        goal.setUser(testUser);
        goal.setPurpose(NetworkingPurpose.PLACEMENT_PREP);
        goal.setDescription("Leetcode grind");
        goal.setTargetYear(4);
        goal.setActive(true);
        goal.setCreatedAt(LocalDateTime.now());

        when(goalService.updateGoal(eq(testUser), eq(5L), any(NetworkingGoalRequest.class)))
                .thenReturn(new NetworkingGoalResponse(goal));

        String requestJson = """
                {
                  "purpose": "PLACEMENT_PREP",
                  "description": "Leetcode grind",
                  "targetYear": 4
                }
                """;

        mockMvc.perform(put("/api/goals/5")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.purpose").value("PLACEMENT_PREP"))
                .andExpect(jsonPath("$.description").value("Leetcode grind"));
    }

    @Test
    void shouldReturn404WhenUpdatingUnownedOrMissingGoal() throws Exception {
        when(goalService.updateGoal(eq(testUser), eq(999L), any(NetworkingGoalRequest.class)))
                .thenThrow(new ResourceNotFoundException("Networking goal not found or not owned by you."));

        String requestJson = """
                {
                  "purpose": "RESEARCH",
                  "description": "Quantum computing"
                }
                """;

        mockMvc.perform(put("/api/goals/999")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Networking goal not found or not owned by you."));
    }

    @Test
    void shouldDeactivateOwnedGoalSuccessfully() throws Exception {
        mockMvc.perform(delete("/api/goals/7")
                        .principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Networking goal deactivated successfully."));
    }

    @Test
    void shouldReturn404WhenDeactivatingUnownedGoal() throws Exception {
        doThrow(new ResourceNotFoundException("Networking goal not found or not owned by you."))
                .when(goalService).deactivateGoal(testUser, 999L);

        mockMvc.perform(delete("/api/goals/999")
                        .principal(principal))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Networking goal not found or not owned by you."));
    }
}
