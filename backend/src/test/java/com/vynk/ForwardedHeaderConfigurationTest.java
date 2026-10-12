package com.vynk;

import com.vynk.auth.controller.AuthController;
import com.vynk.auth.dto.ForgotPasswordRequest;
import com.vynk.auth.service.PasswordResetService;
import com.vynk.auth.service.RegistrationService;
import com.vynk.security.ratelimit.RateLimitingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class ForwardedHeaderConfigurationTest {

    @Test
    @DisplayName("Verify application.properties configures server.forward-headers-strategy to default to 'none'")
    void defaultForwardHeadersStrategyIsNone() throws IOException {
        StandardEnvironment environment = new StandardEnvironment();
        ResourcePropertySource propertySource = new ResourcePropertySource(new ClassPathResource("application.properties"));
        environment.getPropertySources().addFirst(propertySource);

        String strategy = environment.getProperty("server.forward-headers-strategy");
        assertEquals("none", strategy, "server.forward-headers-strategy must default to 'none' for local and direct-access safety");
    }

    @Test
    @DisplayName("Forged X-Forwarded-For header cannot change rate-limit IP when direct remote address is resolved")
    void forgedForwardedHeadersDoNotInfluenceRateLimit() {
        RegistrationService registrationService = mock(RegistrationService.class);
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        PasswordResetService passwordResetService = mock(PasswordResetService.class);
        RateLimitingService rateLimitingService = mock(RateLimitingService.class);

        AuthController authController = new AuthController(
                registrationService,
                authenticationManager,
                passwordResetService,
                rateLimitingService
        );

        MockHttpServletRequest request = new MockHttpServletRequest();
        String genuineClientIp = "198.51.100.42";
        request.setRemoteAddr(genuineClientIp);
        request.addHeader("X-Forwarded-For", "203.0.113.195, 10.0.0.1");
        request.addHeader("Forwarded", "for=203.0.113.195;proto=https");

        ForgotPasswordRequest forgotRequest = new ForgotPasswordRequest("student@college.edu");
        authController.forgotPassword(forgotRequest, request);

        // Verify rate limiting was invoked using the direct remote address
        verify(rateLimitingService).checkLimit(eq("forgot-password"), eq(genuineClientIp), eq(3), eq(60L));

        // Verify the spoofed IP was NEVER passed to the rate limiting service
        verify(rateLimitingService, never()).checkLimit(any(), eq("203.0.113.195"), anyInt(), anyLong());
    }
}
