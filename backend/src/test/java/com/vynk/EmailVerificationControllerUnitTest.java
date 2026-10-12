package com.vynk;

import com.vynk.auth.controller.EmailVerificationController;
import com.vynk.auth.service.EmailVerificationService;
import com.vynk.security.ratelimit.RateLimitingService;
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
    private RateLimitingService rateLimitingService;

    @BeforeEach
    void setUp() {
        verificationService = Mockito.mock(EmailVerificationService.class);
        rateLimitingService = Mockito.mock(RateLimitingService.class);
        EmailVerificationController controller = new EmailVerificationController(verificationService, rateLimitingService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void postVerifyOtpWithValid6DigitCodeSucceeds() throws Exception {
        doNothing().when(verificationService).verifyOtp("student@test.edu", "123456");

        mockMvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"student@test.edu\",\"otp\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email verified successfully! Your VYNK account is now active."));

        verify(verificationService, times(1)).verifyOtp("student@test.edu", "123456");
        verify(rateLimitingService, times(1)).checkLimit(eq("verify-otp"), anyString(), eq(10), eq(60L));
    }

    @Test
    void postVerifyOtpWithInvalidCodeFormatReturns400() throws Exception {
        // Less than 6 digits
        mockMvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"student@test.edu\",\"otp\":\"1234\"}"))
                .andExpect(status().isBadRequest());

        // Non-numeric digits
        mockMvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"student@test.edu\",\"otp\":\"abcdef\"}"))
                .andExpect(status().isBadRequest());

        verify(verificationService, never()).verifyOtp(anyString(), anyString());
    }

    @Test
    void postVerifyOtpWithBlankEmailReturns400() throws Exception {
        mockMvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"\",\"otp\":\"123456\"}"))
                .andExpect(status().isBadRequest());

        verify(verificationService, never()).verifyOtp(anyString(), anyString());
    }

    @Test
    void postResendOtpWithValidEmailReturnsNeutralSuccessMessage() throws Exception {
        doNothing().when(verificationService).resendOtp("student@test.edu");

        mockMvc.perform(post("/api/auth/resend-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"student@test.edu\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("If an eligible unverified account exists for this email, a verification code has been sent."));

        verify(verificationService, times(1)).resendOtp("student@test.edu");
        verify(rateLimitingService, times(1)).checkLimit(eq("resend-otp"), anyString(), eq(3), eq(60L));
    }

    @Test
    void postResendOtpWithMalformedEmailReturns400() throws Exception {
        mockMvc.perform(post("/api/auth/resend-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-a-valid-email\"}"))
                .andExpect(status().isBadRequest());

        verify(verificationService, never()).resendOtp(anyString());
    }

    @Test
    void getVerifyEmailMustReturn405AndNeverActivateAccount() throws Exception {
        mockMvc.perform(get("/api/auth/verify-email?token=scanner-test-token"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.message").value("Email verification requires explicit confirmation via POST."));

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
    void postResendVerificationRoutesToResendOtp() throws Exception {
        doNothing().when(verificationService).resendOtp(eq("student@test.edu"));

        mockMvc.perform(post("/api/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"student@test.edu\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("If an eligible unverified account exists for this email, a verification code has been sent."));

        verify(verificationService, times(1)).resendOtp("student@test.edu");
    }
}
