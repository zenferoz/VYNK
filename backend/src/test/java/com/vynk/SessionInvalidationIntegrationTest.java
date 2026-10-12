package com.vynk;

import com.vynk.auth.controller.AuthController;
import com.vynk.auth.email.EmailService;
import com.vynk.auth.entity.PasswordResetToken;
import com.vynk.auth.repository.PasswordResetTokenRepository;
import com.vynk.auth.service.PasswordResetService;
import com.vynk.auth.service.RegistrationService;
import com.vynk.common.config.SecurityConfig;
import com.vynk.security.ratelimit.RateLimitingService;
import com.vynk.security.session.UserSessionInvalidationService;
import com.vynk.user.entity.AccountStatus;
import com.vynk.user.entity.User;
import com.vynk.user.entity.UserRole;
import com.vynk.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.context.web.WebAppConfiguration;

@WebAppConfiguration
@SpringJUnitConfig(classes = SessionInvalidationIntegrationTest.TestConfig.class)
public class SessionInvalidationIntegrationTest {

    @RestController
    static class ProtectedTestController {
        @GetMapping("/api/test-protected")
        public Map<String, String> protectedEndpoint() {
            return Map.of("message", "access granted");
        }
    }

    @Configuration
    @EnableWebMvc
    @Import(SecurityConfig.class)
    static class TestConfig {

        @Bean
        public ProtectedTestController protectedTestController() {
            return new ProtectedTestController();
        }

        @Bean
        public UserSessionInvalidationService userSessionInvalidationService() {
            return new UserSessionInvalidationService();
        }

        @Bean
        public AuthenticationManager authenticationManager() {
            AuthenticationManager mockAuthManager = mock(AuthenticationManager.class);
            when(mockAuthManager.authenticate(any(Authentication.class))).thenAnswer(invocation -> {
                Authentication auth = invocation.getArgument(0);
                String email = auth.getName();
                org.springframework.security.core.userdetails.User principal =
                        new org.springframework.security.core.userdetails.User(
                                email,
                                "hashedPassword",
                                List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
                        );
                return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
            });
            return mockAuthManager;
        }

        @Bean
        public UserRepository userRepository() {
            return mock(UserRepository.class);
        }

        @Bean
        public PasswordResetTokenRepository tokenRepository() {
            return mock(PasswordResetTokenRepository.class);
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
            PasswordEncoder encoder = mock(PasswordEncoder.class);
            when(encoder.encode(any())).thenReturn("newHashedPassword123");
            return encoder;
        }

        @Bean
        public EmailService emailService() {
            return mock(EmailService.class);
        }

        @Bean
        public PasswordResetService passwordResetService(
                PasswordResetTokenRepository tokenRepository,
                UserRepository userRepository,
                PasswordEncoder passwordEncoder,
                EmailService emailService,
                SessionRegistry sessionRegistry,
                UserSessionInvalidationService userSessionInvalidationService) {
            return new PasswordResetService(
                    tokenRepository,
                    userRepository,
                    passwordEncoder,
                    emailService,
                    "http://localhost:5173",
                    sessionRegistry,
                    userSessionInvalidationService
            );
        }

        @Bean
        public RegistrationService registrationService() {
            return mock(RegistrationService.class);
        }

        @Bean
        public RateLimitingService rateLimitingService() {
            return mock(RateLimitingService.class);
        }

        @Bean
        public AuthController authController(
                RegistrationService registrationService,
                AuthenticationManager authenticationManager,
                PasswordResetService passwordResetService,
                RateLimitingService rateLimitingService,
                org.springframework.security.web.authentication.session.SessionAuthenticationStrategy sessionAuthenticationStrategy) {
            return new AuthController(
                    registrationService,
                    authenticationManager,
                    passwordResetService,
                    rateLimitingService,
                    sessionAuthenticationStrategy
            );
        }
    }

    private MockMvc mockMvc;
    private SessionRegistry sessionRegistry;
    private PasswordResetService passwordResetService;
    private UserSessionInvalidationService invalidationService;
    private PasswordResetTokenRepository tokenRepository;
    private UserRepository userRepository;

    @BeforeEach
    void setUp(WebApplicationContext context) {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        this.sessionRegistry = context.getBean(SessionRegistry.class);
        this.passwordResetService = context.getBean(PasswordResetService.class);
        this.invalidationService = context.getBean(UserSessionInvalidationService.class);
        this.tokenRepository = context.getBean(PasswordResetTokenRepository.class);
        this.userRepository = context.getBean(UserRepository.class);
    }

    @Test
    @DisplayName("Login registers session in SessionRegistry, and password reset causes subsequent requests to be rejected")
    void loginRegistersSessionAndResetPasswordInvalidatesIt() throws Exception {
        String testEmail = "testuser@anurag.edu.in";
        User user = new User();
        user.setEmail(testEmail);
        user.setPasswordHash("hashedPassword");
        user.setStatus(AccountStatus.ACTIVE);
        user.setEmailVerified(true);
        user.setRole(UserRole.STUDENT);

        // 1. Perform custom login
        MockHttpSession session = (MockHttpSession) mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + testEmail + "\",\"password\":\"ValidPass123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Login successful."))
                .andReturn()
                .getRequest()
                .getSession(false);

        assertNotNull(session, "Session must be created upon successful login");

        // 2. Verify session is registered in SessionRegistry
        List<Object> allPrincipals = sessionRegistry.getAllPrincipals();
        assertFalse(allPrincipals.isEmpty(), "SessionRegistry principals list must not be empty after login");

        Object registeredPrincipal = allPrincipals.stream()
                .filter(p -> {
                    if (p instanceof org.springframework.security.core.userdetails.UserDetails ud) {
                        return ud.getUsername().equalsIgnoreCase(testEmail);
                    }
                    return false;
                })
                .findFirst()
                .orElse(null);

        assertNotNull(registeredPrincipal, "Logged-in user must be registered in SessionRegistry");

        List<SessionInformation> sessions = sessionRegistry.getAllSessions(registeredPrincipal, false);
        assertFalse(sessions.isEmpty(), "SessionRegistry must have active session for principal");
        SessionInformation sessionInfo = sessions.get(0);
        assertEquals(session.getId(), sessionInfo.getSessionId(), "Registered session ID must match login session ID");
        assertFalse(sessionInfo.isExpired(), "Session must not be expired before reset");

        // 3. Authenticated session can access protected endpoint
        mockMvc.perform(get("/api/test-protected").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("access granted"));

        // 4. Reset password
        String rawToken = "validResetToken123";
        String tokenHash = passwordResetService.hashToken(rawToken);
        PasswordResetToken resetToken = new PasswordResetToken(tokenHash, user, LocalDateTime.now().plusHours(1));

        when(tokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(resetToken));
        when(tokenRepository.markTokenUsed(eq(tokenHash), any())).thenReturn(1);
        when(userRepository.save(any(User.class))).thenReturn(user);

        passwordResetService.resetPassword(rawToken, "NewSecurePassword123");

        // Verify session was expired via SessionRegistry
        assertTrue(sessionInfo.isExpired(), "Session in SessionRegistry must be marked expired after password reset");

        // 5. Subsequent request using old session MUST be rejected with 401 Unauthorized
        mockMvc.perform(get("/api/test-protected").session(session))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("Unregistered session predating password reset cutoff is also rejected by SessionValidationFilter")
    void unregisteredSessionPredatingPasswordResetIsRejected() throws Exception {
        String testEmail = "unregistered@anurag.edu.in";

        // Simulate an existing session authenticated in the past without SessionRegistry
        MockHttpSession legacySession = new MockHttpSession();
        org.springframework.security.core.userdetails.User principal =
                new org.springframework.security.core.userdetails.User(
                        testEmail,
                        "oldHashedPassword",
                        List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
                );
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        org.springframework.security.core.context.SecurityContext context =
                org.springframework.security.core.context.SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        legacySession.setAttribute(
                org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                context
        );
        // Auth time in the past
        legacySession.setAttribute("VYNK_AUTH_TIME", Instant.now().minusSeconds(120));

        // Mark password reset for user
        invalidationService.markPasswordReset(testEmail);

        // Accessing protected endpoint with legacy session must be rejected with 401
        mockMvc.perform(get("/api/test-protected").session(legacySession))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }
}
