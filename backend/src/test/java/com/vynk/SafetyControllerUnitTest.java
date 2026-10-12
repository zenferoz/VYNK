package com.vynk;

import com.vynk.common.exception.GlobalExceptionHandler;
import com.vynk.common.exception.ResourceNotFoundException;
import com.vynk.safety.controller.SafetyController;
import com.vynk.safety.entity.UserBlock;
import com.vynk.safety.service.SafetyService;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SafetyControllerUnitTest {

    private MockMvc mockMvc;
    private SafetyService safetyService;
    private UserRepository userRepository;

    private User currentUser;
    private Principal principal;

    @BeforeEach
    void setUp() throws Exception {
        safetyService = Mockito.mock(SafetyService.class);
        userRepository = Mockito.mock(UserRepository.class);

        SafetyController controller = new SafetyController(safetyService, userRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        currentUser = new User();
        Field idField = User.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(currentUser, 100L);
        currentUser.setEmail("current@college.edu");

        principal = new UsernamePasswordAuthenticationToken("current@college.edu", "password");
        when(userRepository.findByEmail("current@college.edu")).thenReturn(Optional.of(currentUser));
    }

    @Test
    void shouldBlockUserSuccessfully() throws Exception {
        mockMvc.perform(post("/api/users/200/block")
                        .principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User blocked successfully."));
    }

    @Test
    void shouldRejectSelfBlocking() throws Exception {
        doThrow(new IllegalArgumentException("You cannot block your own profile."))
                .when(safetyService).blockUser(currentUser, 100L);

        mockMvc.perform(post("/api/users/100/block")
                        .principal(principal))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("You cannot block your own profile."));
    }

    @Test
    void shouldUnblockUserSuccessfully() throws Exception {
        mockMvc.perform(delete("/api/users/200/block")
                        .principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User unblocked successfully."));
    }

    @Test
    void shouldGetBlockedUsers() throws Exception {
        User blockedUser = new User();
        Field idField = User.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(blockedUser, 300L);

        UserBlock block = new UserBlock(currentUser, blockedUser);
        Field blockIdField = UserBlock.class.getDeclaredField("id");
        blockIdField.setAccessible(true);
        blockIdField.set(block, 1L);
        block.setCreatedAt(LocalDateTime.of(2026, 10, 11, 10, 0));

        when(safetyService.getBlockedUsersBy(100L)).thenReturn(List.of(block));

        mockMvc.perform(get("/api/users/blocked")
                        .principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].blockedUserId").value(300))
                .andExpect(jsonPath("$[0].createdAt").exists());
    }

    @Test
    void shouldReportUserSuccessfully() throws Exception {
        String json = """
                {
                  "reason": "HARASSMENT",
                  "description": "Inappropriate messages"
                }
                """;

        mockMvc.perform(post("/api/users/400/report")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Report submitted successfully."));
    }

    @Test
    void shouldRejectReportWithMissingReason() throws Exception {
        String json = """
                {
                  "description": "Missing reason"
                }
                """;

        mockMvc.perform(post("/api/users/400/report")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.reason").exists());
    }

    @Test
    void shouldReturn404WhenReportingNonexistentUser() throws Exception {
        doThrow(new ResourceNotFoundException("Target user not found."))
                .when(safetyService).reportUser(eq(currentUser), eq(999L), eq("SPAM"), eq("test"));

        String json = """
                {
                  "reason": "SPAM",
                  "description": "test"
                }
                """;

        mockMvc.perform(post("/api/users/999/report")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Target user not found."));
    }
}
