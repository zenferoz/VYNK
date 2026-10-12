package com.vynk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vynk.auth.dto.ForgotPasswordRequest;
import com.vynk.auth.dto.LoginRequest;
import com.vynk.auth.dto.RegisterRequest;
import com.vynk.auth.dto.ResetPasswordRequest;
import com.vynk.auth.dto.VerifyOtpRequest;
import com.vynk.auth.email.EmailService;
import com.vynk.auth.entity.EmailVerificationOtp;
import com.vynk.auth.entity.PasswordResetToken;
import com.vynk.auth.repository.EmailVerificationOtpRepository;
import com.vynk.auth.repository.PasswordResetTokenRepository;
import com.vynk.auth.service.EmailVerificationService;
import com.vynk.college.entity.College;
import com.vynk.college.repository.CollegeRepository;
import com.vynk.user.entity.AccountStatus;
import com.vynk.user.entity.User;
import com.vynk.user.entity.UserRole;
import com.vynk.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:vynk_otp_flow_db;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "vynk.otp.pepper=test-secure-pepper-for-sandbox-flow-2026",
        "vynk.otp.expiry-minutes=5",
        "vynk.otp.resend-cooldown-seconds=60",
        "vynk.otp.max-attempts=5",
        "vynk.resend.simulation-mode=false",
        "vynk.ratelimit.enabled=false"
})
public class EmailOtpFlowIntegrationTest {

    @TestConfiguration
    static class TestEmailServiceConfig {
        static final AtomicReference<String> lastCapturedOtp = new AtomicReference<>();
        static final AtomicReference<String> lastCapturedResetLink = new AtomicReference<>();
        static final AtomicReference<String> lastCapturedRecipient = new AtomicReference<>();

        @Bean
        @Primary
        public EmailService testEmailService() {
            return new EmailService(
                    "re_dummy_test_key",
                    "onboarding@resend.dev",
                    "https://api.resend.com/emails",
                    10,
                    false,
                    new ObjectMapper(),
                    null
            ) {
                @Override
                public void sendVerificationOtp(String recipientEmail, String otp) {
                    lastCapturedRecipient.set(recipientEmail);
                    lastCapturedOtp.set(otp);
                }

                @Override
                public void sendPasswordResetEmail(String recipientEmail, String resetLink) {
                    lastCapturedRecipient.set(recipientEmail);
                    lastCapturedResetLink.set(resetLink);
                }
            };
        }
    }

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CollegeRepository collegeRepository;

    @Autowired
    private EmailVerificationOtpRepository otpRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailVerificationService emailVerificationService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        TestEmailServiceConfig.lastCapturedOtp.set(null);
        TestEmailServiceConfig.lastCapturedResetLink.set(null);
        TestEmailServiceConfig.lastCapturedRecipient.set(null);
    }

    @Test
    @DisplayName("Immediate Registration -> ACTIVE Account -> Instant Login Lifecycle without OTP")
    void testImmediateRegistrationAndLoginLifecycle() throws Exception {
        String studentEmail = "student101@anurag.edu.in";
        String rawPassword = "SecurePassword123!";

        College anurag = collegeRepository.findByActiveTrue().stream()
                .filter(c -> "anurag.edu.in".equalsIgnoreCase(c.getEmailDomain()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Anurag University must be seeded"));

        // 1. Submit Registration
        RegisterRequest registerReq = new RegisterRequest();
        registerReq.setEmail(studentEmail);
        registerReq.setPassword(rawPassword);
        registerReq.setCollegeId(anurag.getId());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Registration successful. You can now log in."));

        // 2. Verify account is immediately ACTIVE and emailVerified = true
        User user = userRepository.findByEmail(studentEmail).orElseThrow();
        assertEquals(AccountStatus.ACTIVE, user.getStatus());
        assertTrue(user.isEmailVerified());

        // 3. Verify no OTP email was dispatched and no OTP record was created
        assertNull(TestEmailServiceConfig.lastCapturedOtp.get(), "Registration must not dispatch OTP email");
        assertTrue(otpRepository.findByUserId(user.getId()).isEmpty(), "No OTP record should be generated during direct registration");

        // 4. Verify login succeeds immediately with 200 OK
        LoginRequest loginReq = new LoginRequest();
        loginReq.setEmail(studentEmail);
        loginReq.setPassword(rawPassword);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Login successful."))
                .andExpect(jsonPath("$.email").value(studentEmail));
    }

    @Test
    @DisplayName("Duplicate Registration Prevention: rejects re-registration with existing email")
    void testDuplicateRegistrationRejected() throws Exception {
        String studentEmail = "duplicate.student@anurag.edu.in";
        String rawPassword = "Password123!";

        College anurag = collegeRepository.findByActiveTrue().stream()
                .filter(c -> "anurag.edu.in".equalsIgnoreCase(c.getEmailDomain()))
                .findFirst()
                .orElseThrow();

        RegisterRequest req = new RegisterRequest();
        req.setEmail(studentEmail);
        req.setPassword(rawPassword);
        req.setCollegeId(anurag.getId());

        // First registration succeeds
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        // Second registration with the same email is rejected with 409 Conflict
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("An account with this email already exists."));
    }

    @Test
    @DisplayName("Strict Institutional Domain Validation: rejects unauthorized domains")
    void testRegistrationEnforcesCollegeDomain() throws Exception {
        College anurag = collegeRepository.findByActiveTrue().stream()
                .filter(c -> "anurag.edu.in".equalsIgnoreCase(c.getEmailDomain()))
                .findFirst()
                .orElseThrow();

        RegisterRequest req = new RegisterRequest();
        req.setEmail("unauthorized.student@gmail.com"); // Non-institutional domain
        req.setPassword("Password123!");
        req.setCollegeId(anurag.getId());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Legacy PENDING Account Protection: cannot login without verification and cannot be hijacked by re-registration")
    void testLegacyPendingAccountProtectionAndCompatibility() throws Exception {
        String legacyEmail = "legacy.pending@anurag.edu.in";
        String legacyPassword = "OriginalLegacyPass123!";

        College anurag = collegeRepository.findByActiveTrue().stream()
                .filter(c -> "anurag.edu.in".equalsIgnoreCase(c.getEmailDomain()))
                .findFirst()
                .orElseThrow();

        // Seed a legacy unverified PENDING user directly in DB
        User legacyUser = new User();
        legacyUser.setEmail(legacyEmail);
        legacyUser.setPasswordHash(passwordEncoder.encode(legacyPassword));
        legacyUser.setRole(UserRole.STUDENT);
        legacyUser.setStatus(AccountStatus.PENDING);
        legacyUser.setEmailVerified(false);
        legacyUser.setCollege(anurag);
        legacyUser = userRepository.save(legacyUser);

        // 1. Attempt login -> must be rejected with 403 Forbidden
        LoginRequest loginReq = new LoginRequest();
        loginReq.setEmail(legacyEmail);
        loginReq.setPassword(legacyPassword);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("This account is not currently allowed to log in."));

        // 2. Re-registering with this email must be rejected and must NOT activate the account
        RegisterRequest reRegReq = new RegisterRequest();
        reRegReq.setEmail(legacyEmail);
        reRegReq.setPassword("AttackerNewPass999!");
        reRegReq.setCollegeId(anurag.getId());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reRegReq)))
                .andExpect(status().isConflict());

        // Verify account is STILL PENDING and emailVerified is STILL false
        User refreshedUser = userRepository.findByEmail(legacyEmail).orElseThrow();
        assertEquals(AccountStatus.PENDING, refreshedUser.getStatus());
        assertFalse(refreshedUser.isEmailVerified());

        // 3. Existing OTP endpoints remain available to safely verify legacy accounts if needed
        String rawOtp = emailVerificationService.createAndSendOtp(legacyUser);

        VerifyOtpRequest verifyReq = new VerifyOtpRequest();
        verifyReq.setEmail(legacyEmail);
        verifyReq.setOtp(rawOtp);

        mockMvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isOk());

        // Now legacy user is ACTIVE and can log in
        User activatedUser = userRepository.findByEmail(legacyEmail).orElseThrow();
        assertEquals(AccountStatus.ACTIVE, activatedUser.getStatus());
        assertTrue(activatedUser.isEmailVerified());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Password Reset Compatibility: active registered user can reset password, unverified account cannot")
    void testPasswordResetCompatibility() throws Exception {
        String studentEmail = "pwdreset.student@anurag.edu.in";
        String initialPassword = "InitialSecurePassword123!";
        String newPassword = "BrandNewPassword2026!";

        College anurag = collegeRepository.findByActiveTrue().stream()
                .filter(c -> "anurag.edu.in".equalsIgnoreCase(c.getEmailDomain()))
                .findFirst()
                .orElseThrow();

        // Register active user
        RegisterRequest registerReq = new RegisterRequest();
        registerReq.setEmail(studentEmail);
        registerReq.setPassword(initialPassword);
        registerReq.setCollegeId(anurag.getId());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        // 1. Request password reset
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + studentEmail + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("If an eligible account exists for this email, password reset instructions have been sent."));

        String resetLink = TestEmailServiceConfig.lastCapturedResetLink.get();
        assertNotNull(resetLink, "Reset link should be generated for active student");
        assertTrue(resetLink.contains("token="));
        String rawToken = resetLink.substring(resetLink.indexOf("token=") + 6);

        // 2. Execute password reset
        ResetPasswordRequest resetReq = new ResetPasswordRequest();
        resetReq.setToken(rawToken);
        resetReq.setNewPassword(newPassword);

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Your password has been successfully reset. You can now log in."));

        // 3. Login with old password fails
        LoginRequest oldLogin = new LoginRequest();
        oldLogin.setEmail(studentEmail);
        oldLogin.setPassword(initialPassword);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(oldLogin)))
                .andExpect(status().isUnauthorized());

        // 4. Login with new password succeeds
        LoginRequest newLogin = new LoginRequest();
        newLogin.setEmail(studentEmail);
        newLogin.setPassword(newPassword);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Login successful."));
    }
}
