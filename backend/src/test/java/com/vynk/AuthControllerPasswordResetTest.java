package com.vynk;

import com.vynk.auth.controller.AuthController;
import com.vynk.auth.dto.ForgotPasswordRequest;
import com.vynk.auth.dto.ResetPasswordRequest;
import com.vynk.auth.service.PasswordResetService;
import com.vynk.auth.service.RegistrationService;
import com.vynk.security.ratelimit.RateLimitingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthControllerPasswordResetTest {

    private RegistrationService registrationService;
    private AuthenticationManager authenticationManager;
    private PasswordResetService passwordResetService;
    private RateLimitingService rateLimitingService;
    private AuthController authController;

    @BeforeEach
    void setUp() {
        registrationService = mock(RegistrationService.class);
        authenticationManager = mock(AuthenticationManager.class);
        passwordResetService = mock(PasswordResetService.class);
        rateLimitingService = mock(RateLimitingService.class);

        authController = new AuthController(
                registrationService,
                authenticationManager,
                passwordResetService,
                rateLimitingService
        );
    }

    @Test
    void forgotPasswordReturnsNeutralSuccessResponse() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");

        ForgotPasswordRequest req = new ForgotPasswordRequest("student@college.edu");
        ResponseEntity<Map<String, String>> response = authController.forgotPassword(req, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().get("message").contains("If an eligible account exists"));

        verify(passwordResetService).requestPasswordReset("student@college.edu");
        verify(rateLimitingService).checkLimit(eq("forgot-password"), eq("127.0.0.1"), eq(3), eq(60L));
    }

    @Test
    void resetPasswordSucceedsWithValidRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");

        ResetPasswordRequest req = new ResetPasswordRequest("validToken", "NewPassword123");
        ResponseEntity<Map<String, String>> response = authController.resetPassword(req, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().get("message").contains("successfully reset"));

        verify(passwordResetService).resetPassword("validToken", "NewPassword123");
        verify(rateLimitingService).checkLimit(eq("reset-password"), eq("127.0.0.1"), eq(5), eq(60L));
    }

    @Test
    void forgotPasswordIgnoresUntrustedXForwardedForHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.23");
        request.addHeader("X-Forwarded-For", "203.0.113.195, 10.0.0.1");

        ForgotPasswordRequest req = new ForgotPasswordRequest("student@college.edu");
        ResponseEntity<Map<String, String>> response = authController.forgotPassword(req, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        // Verify rate limiting used direct remote address, ignoring forged X-Forwarded-For
        verify(rateLimitingService).checkLimit(eq("forgot-password"), eq("198.51.100.23"), eq(3), eq(60L));
        verify(rateLimitingService, never()).checkLimit(any(), eq("203.0.113.195"), anyInt(), anyLong());
    }

    @Test
    void resetPasswordIgnoresUntrustedXForwardedForHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.24");
        request.addHeader("X-Forwarded-For", "192.0.2.1");

        ResetPasswordRequest req = new ResetPasswordRequest("validToken", "NewPassword123");
        ResponseEntity<Map<String, String>> response = authController.resetPassword(req, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        // Verify rate limiting used direct remote address, ignoring forged X-Forwarded-For
        verify(rateLimitingService).checkLimit(eq("reset-password"), eq("198.51.100.24"), eq(5), eq(60L));
        verify(rateLimitingService, never()).checkLimit(any(), eq("192.0.2.1"), anyInt(), anyLong());
    }
}
