package com.vynk;

import com.vynk.common.exception.GlobalExceptionHandler;
import com.vynk.common.exception.ResourceNotFoundException;
import com.vynk.profile.controller.StudentProfileController;
import com.vynk.profile.dto.StudentProfileResponse;
import com.vynk.profile.service.StudentProfileService;
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
import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StudentProfileControllerUnitTest {

    private MockMvc mockMvc;
    private StudentProfileService profileService;
    private UserRepository userRepository;

    private User testUser;
    private Principal principal;

    @BeforeEach
    void setUp() throws Exception {
        profileService = Mockito.mock(StudentProfileService.class);
        userRepository = Mockito.mock(UserRepository.class);

        StudentProfileController controller = new StudentProfileController(profileService, userRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        testUser = new User();
        Field idField = User.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(testUser, 202L);
        testUser.setEmail("student202@anurag.edu.in");

        principal = new UsernamePasswordAuthenticationToken("student202@anurag.edu.in", "password");
        when(userRepository.findByEmail("student202@anurag.edu.in")).thenReturn(Optional.of(testUser));
    }

    @Test
    void getMyProfile_WhenProfileExists_Returns200WithProfile() throws Exception {
        StudentProfileResponse mockResponse = new StudentProfileResponse(
                10L, 202L, "Siddharth Rao", "CSE", "B.Tech", 3,
                "Student bio", true, true, "10 hrs/week",
                Collections.singletonList("Python"), Collections.singletonList("Robotics")
        );
        when(profileService.getProfileResponseForUser(202L)).thenReturn(mockResponse);

        mockMvc.perform(get("/api/profile/me").principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.userId").value(202))
                .andExpect(jsonPath("$.fullName").value("Siddharth Rao"))
                .andExpect(jsonPath("$.department").value("CSE"))
                .andExpect(jsonPath("$.onboardingCompleted").value(true));
    }

    @Test
    void getMyProfile_WhenProfileDoesNotExist_Returns404() throws Exception {
        when(profileService.getProfileResponseForUser(202L))
                .thenThrow(new ResourceNotFoundException("Student profile not found. Please complete onboarding."));

        mockMvc.perform(get("/api/profile/me").principal(principal))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Student profile not found. Please complete onboarding."));
    }

    @Test
    void updateMyProfile_WithValidPayload_Returns200() throws Exception {
        StudentProfileResponse updatedResponse = new StudentProfileResponse(
                10L, 202L, "Siddharth Rao", "Information Technology", "B.Tech IT", 2,
                "Updated bio", true, true, "Flexible",
                Collections.singletonList("React"), Collections.singletonList("Web Dev")
        );
        when(profileService.saveOrUpdateProfile(eq(testUser), any())).thenReturn(updatedResponse);

        String jsonPayload = """
                {
                    "fullName": "Siddharth Rao",
                    "department": "Information Technology",
                    "course": "B.Tech IT",
                    "currentYear": 2,
                    "bio": "Updated bio",
                    "discoverable": true,
                    "availability": "Flexible",
                    "skills": ["React"],
                    "interests": ["Web Dev"]
                }
                """;

        mockMvc.perform(put("/api/profile/me")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Siddharth Rao"))
                .andExpect(jsonPath("$.department").value("Information Technology"))
                .andExpect(jsonPath("$.currentYear").value(2))
                .andExpect(jsonPath("$.onboardingCompleted").value(true));
    }

    @Test
    void updateMyProfile_WithMissingRequiredFields_Returns400() throws Exception {
        String invalidPayload = """
                {
                    "fullName": "",
                    "department": "",
                    "course": "",
                    "currentYear": null
                }
                """;

        mockMvc.perform(put("/api/profile/me")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.fullName").value("Full name is required"))
                .andExpect(jsonPath("$.details.department").value("Department is required"))
                .andExpect(jsonPath("$.details.course").value("Course is required"))
                .andExpect(jsonPath("$.details.currentYear").value("Current year is required"));
    }

    @Test
    void updateMyProfile_WithInvalidYear_Returns400() throws Exception {
        String invalidYearPayload = """
                {
                    "fullName": "Valid Name",
                    "department": "CSE",
                    "course": "B.Tech",
                    "currentYear": 9
                }
                """;

        mockMvc.perform(put("/api/profile/me")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidYearPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.currentYear").value("Current year must be between 1 and 6"));
    }
}
