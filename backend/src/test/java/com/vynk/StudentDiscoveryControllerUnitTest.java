package com.vynk;

import com.vynk.common.exception.GlobalExceptionHandler;
import com.vynk.college.entity.College;
import com.vynk.matching.dto.StudentMatchResponse;
import com.vynk.matching.service.PurposeMatchingService;
import com.vynk.profile.controller.StudentDiscoveryController;
import com.vynk.profile.dto.StudentDiscoveryResponse;
import com.vynk.profile.service.StudentProfileService;
import com.vynk.user.entity.User;
import com.vynk.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Field;
import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StudentDiscoveryControllerUnitTest {

    private MockMvc mockMvc;
    private StudentProfileService profileService;
    private PurposeMatchingService matchingService;
    private UserRepository userRepository;

    private User testUser;
    private Principal principal;

    @BeforeEach
    void setUp() throws Exception {
        profileService = Mockito.mock(StudentProfileService.class);
        matchingService = Mockito.mock(PurposeMatchingService.class);
        userRepository = Mockito.mock(UserRepository.class);

        StudentDiscoveryController controller = new StudentDiscoveryController(
                profileService, matchingService, userRepository
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        College college = new College();
        Field collegeIdField = College.class.getDeclaredField("id");
        collegeIdField.setAccessible(true);
        collegeIdField.set(college, 1L);
        college.setName("Campus Institute");

        testUser = new User();
        Field idField = User.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(testUser, 50L);
        testUser.setEmail("matchtester@campus.edu");
        testUser.setCollege(college);

        principal = new UsernamePasswordAuthenticationToken("matchtester@campus.edu", "password");
        when(userRepository.findByEmail("matchtester@campus.edu")).thenReturn(Optional.of(testUser));
    }

    @Test
    void shouldReturnTopMatchesForAuthenticatedUser() throws Exception {
        StudentMatchResponse match = new StudentMatchResponse(
                60L,
                "Teammate Student",
                "Computer Science",
                "B.Tech",
                3,
                "Passionate about AI",
                List.of("Python", "React"),
                List.of("Hackathons"),
                "Evenings",
                List.of("Hackathon Team"),
                80,
                List.of("Shared purpose: Hackathon Team", "Same academic year (3)")
        );

        when(matchingService.getTopMatches(testUser)).thenReturn(List.of(match));

        mockMvc.perform(get("/api/students/matches")
                        .principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(60))
                .andExpect(jsonPath("$[0].fullName").value("Teammate Student"))
                .andExpect(jsonPath("$[0].matchScore").value(80))
                .andExpect(jsonPath("$[0].matchReasons[0]").value("Shared purpose: Hackathon Team"))
                .andExpect(jsonPath("$[0].activePurposes[0]").value("Hackathon Team"));
    }

    @Test
    void shouldReturnEmptyMatchesWhenNoRecommendations() throws Exception {
        when(matchingService.getTopMatches(testUser)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/students/matches")
                        .principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldReturnDiscoverableStudents() throws Exception {
        StudentDiscoveryResponse student = new StudentDiscoveryResponse(
                1L, 70L, "Peer Student", "IT", "B.Tech", 2, "Bio", "Campus Institute",
                "Weekends", List.of("Java"), List.of("Coding")
        );

        when(profileService.getDiscoverableStudentsInCollege(1L, 50L)).thenReturn(List.of(student));

        mockMvc.perform(get("/api/students")
                        .principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(70))
                .andExpect(jsonPath("$[0].fullName").value("Peer Student"));
    }
}
