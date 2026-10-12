package com.vynk;

import com.vynk.auth.controller.EmailVerificationController;
import com.vynk.auth.service.EmailVerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EmailVerificationControllerUnitTest {

    private MockMvc mockMvc;
    private EmailVerificationService verificationService;

    @BeforeEach
    void setUp() {
        verificationService = Mockito.mock(EmailVerificationService.class);
        EmailVerificationController controller = new EmailVerificationController(verificationService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getVerifyEmailMustReturn405AndNeverActivateAccount() throws Exception {
        // Scanners or crawlers opening GET /api/auth/verify-email must be rejected
        mockMvc.perform(get("/api/auth/verify-email?token=scanner-test-token"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.message").value("Email verification requires explicit confirmation via POST."));

        // Verify that verification service was NEVER called
        verify(verificationService, never()).verifyEmail(anyString());
    }

    @Test
    void postVerifyEmailWithValidTokenActivatesAccount() throws Exception {
        doNothing().when(verificationService).verifyEmail("valid-token-123");

        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"valid-token-123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email verified successfully! Your VYNK account is now active."));

        verify(verificationService, times(1)).verifyEmail("valid-token-123");
    }

    @Test
    void postVerifyEmailWithBlankTokenReturns400() throws Exception {
        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"   \"}"))
                .andExpect(status().isBadRequest());

        verify(verificationService, never()).verifyEmail(anyString());
    }

    @Test
    void postResendVerificationReturnsUniformNeutralMessageAcrossCalls() throws Exception {
        doNothing().when(verificationService).resendVerificationToken(eq("student@test.edu"));

        // First call
        mockMvc.perform(post("/api/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"student@test.edu\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("If an eligible unverified account exists for this email, a verification link has been sent."));

        // Second call (simulating repeated call or cooldown)
        mockMvc.perform(post("/api/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"student@test.edu\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("If an eligible unverified account exists for this email, a verification link has been sent."));

        verify(verificationService, times(2)).resendVerificationToken("student@test.edu");
    }

    @Test
    void postResendVerificationWithMalformedEmailReturns400() throws Exception {
        mockMvc.perform(post("/api/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-a-valid-email\"}"))
                .andExpect(status().isBadRequest());

        verify(verificationService, never()).resendVerificationToken(anyString());
    }
}
