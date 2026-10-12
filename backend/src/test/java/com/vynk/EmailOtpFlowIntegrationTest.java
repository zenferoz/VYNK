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
import com.vynk.college.entity.College;
import com.vynk.college.repository.CollegeRepository;
import com.vynk.user.entity.AccountStatus;
import com.vynk.user.entity.User;
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
        "vynk.resend.simulation-mode=false"
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
    @DisplayName("End-to-End Registration -> OTP Delivery -> Activation -> Login Flow")
    void testCompleteOtpRegistrationAndLoginLifecycle() throws Exception {
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
                .andExpect(jsonPath("$.message").value("Registration successful. Email verification is pending."));

        // 2. Verify student account state is unverified and PENDING
        User user = userRepository.findByEmail(studentEmail).orElseThrow();
        assertEquals(AccountStatus.PENDING, user.getStatus());
        assertFalse(user.isEmailVerified());

        // 3. Verify OTP was created, HMAC-SHA256 hashed, and dispatched to EmailService
        EmailVerificationOtp otpRecord = otpRepository.findByUserId(user.getId()).orElseThrow();
        assertNotNull(otpRecord.getOtpHash());
        assertFalse(otpRecord.isUsed());
        assertEquals(0, otpRecord.getAttempts());

        String capturedOtp = TestEmailServiceConfig.lastCapturedOtp.get();
        assertNotNull(capturedOtp, "OTP should have been sent via EmailService");
        assertEquals(6, capturedOtp.length());
        assertTrue(capturedOtp.matches("^[0-9]{6}$"));

        // 4. Attempt login before email verification -> Must fail with 403 Forbidden
        LoginRequest loginReq = new LoginRequest();
        loginReq.setEmail(studentEmail);
        loginReq.setPassword(rawPassword);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("This account is not currently allowed to log in."));

        // 5. Submit incorrect OTP -> Must return 400 with remaining attempts
        VerifyOtpRequest wrongOtpReq = new VerifyOtpRequest();
        wrongOtpReq.setEmail(studentEmail);
        wrongOtpReq.setOtp("000000");

        mockMvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongOtpReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid verification code. 4 attempts remaining."));

        // Verify attempt count incremented
        otpRecord = otpRepository.findByUserId(user.getId()).orElseThrow();
        assertEquals(1, otpRecord.getAttempts());
        assertFalse(otpRecord.isUsed());

        // 6. Resend Cooldown Test: Attempting to resend within 60s cooldown must fail
        mockMvc.perform(post("/api/auth/resend-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + studentEmail + "\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Please wait")));

        // 7. Test lockout after exceeding maximum attempts (5)
        otpRecord.setAttempts(4);
        otpRepository.save(otpRecord);

        mockMvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongOtpReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Maximum verification attempts exceeded. Please request a new code."));

        // 8. Test cooldown reset & resend fresh OTP
        otpRecord.setCreatedAt(LocalDateTime.now().minusSeconds(65));
        otpRepository.save(otpRecord);

        mockMvc.perform(post("/api/auth/resend-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + studentEmail + "\"}"))
                .andExpect(status().isOk());

        String freshOtp = TestEmailServiceConfig.lastCapturedOtp.get();
        assertNotNull(freshOtp);

        // 9. Test expiry handling
        EmailVerificationOtp freshRecord = otpRepository.findByUserId(user.getId()).orElseThrow();
        freshRecord.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        otpRepository.save(freshRecord);

        VerifyOtpRequest validOtpReq = new VerifyOtpRequest();
        validOtpReq.setEmail(studentEmail);
        validOtpReq.setOtp(freshOtp);

        mockMvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validOtpReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Verification code has expired. Please request a new code."));

        // 10. Restore valid expiration and submit correct OTP
        freshRecord.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        otpRepository.save(freshRecord);

        mockMvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validOtpReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email verified successfully! Your VYNK account is now active."));

        // 11. Verify user is now ACTIVE and emailVerified = true
        user = userRepository.findByEmail(studentEmail).orElseThrow();
        assertEquals(AccountStatus.ACTIVE, user.getStatus());
        assertTrue(user.isEmailVerified());

        freshRecord = otpRepository.findByUserId(user.getId()).orElseThrow();
        assertTrue(freshRecord.isUsed());

        // 12. Reusing already-used OTP must fail
        mockMvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validOtpReq)))
                .andExpect(status().isBadRequest());

        // 13. Login with credentials now succeeds (200 OK)
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Login successful."))
                .andExpect(jsonPath("$.email").value(studentEmail));

        // 14. Password Reset flow for active verified user
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + studentEmail + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("If an eligible account exists for this email, password reset instructions have been sent."));

        String resetLink = TestEmailServiceConfig.lastCapturedResetLink.get();
        assertNotNull(resetLink);
        assertTrue(resetLink.contains("token="));
        String rawToken = resetLink.substring(resetLink.indexOf("token=") + 6);

        // 15. Execute password reset
        String newPassword = "BrandNewPassword2026!";
        ResetPasswordRequest resetReq = new ResetPasswordRequest();
        resetReq.setToken(rawToken);
        resetReq.setNewPassword(newPassword);

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Your password has been successfully reset. You can now log in."));

        // 16. Verify login with old password fails (401 Unauthorized)
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password."));

        // 17. Verify login with new password succeeds (200 OK)
        LoginRequest newLoginReq = new LoginRequest();
        newLoginReq.setEmail(studentEmail);
        newLoginReq.setPassword(newPassword);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newLoginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Login successful."));
    }

    @Test
    @DisplayName("Development Sandbox Integration: Verified Account Email Path without Weakening Production")
    void testDevelopmentSandboxIntegrationPath() throws Exception {
        // In local development sandbox testing:
        // When using Resend's free tier sandbox (onboarding@resend.dev), Resend strictly restricts delivery
        // to the verified account owner's email address (e.g. sandbox-owner@example.com).
        // Production validation requires college email domains.
        // In local test environments (isolated H2 DB), developers can seed a local sandbox college
        // or test the direct API integration path without altering production data or relaxing domain checks.
        String sandboxEmail = "sandbox-owner@example.edu";
        College sandboxCollege = collegeRepository.findByActiveTrue().stream()
                .filter(c -> "example.edu".equalsIgnoreCase(c.getEmailDomain()))
                .findFirst()
                .orElseGet(() -> {
                    College c = new College();
                    c.setName("Sandbox Test College");
                    c.setEmailDomain("example.edu");
                    c.setActive(true);
                    return collegeRepository.save(c);
                });

        RegisterRequest req = new RegisterRequest();
        req.setEmail(sandboxEmail);
        req.setPassword("SandboxPass123!");
        req.setCollegeId(sandboxCollege.getId());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        assertEquals(sandboxEmail, TestEmailServiceConfig.lastCapturedRecipient.get());
        String otp = TestEmailServiceConfig.lastCapturedOtp.get();
        assertNotNull(otp);

        VerifyOtpRequest verifyReq = new VerifyOtpRequest();
        verifyReq.setEmail(sandboxEmail);
        verifyReq.setOtp(otp);

        mockMvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isOk());

        User sandboxUser = userRepository.findByEmail(sandboxEmail).orElseThrow();
        assertEquals(AccountStatus.ACTIVE, sandboxUser.getStatus());
        assertTrue(sandboxUser.isEmailVerified());
    }

    @Test
    @DisplayName("Registration enforces college domain validation and rejects non-matching email")
    void testRegistrationEnforcesCollegeDomain() throws Exception {
        College anurag = collegeRepository.findByActiveTrue().stream()
                .filter(c -> "anurag.edu.in".equalsIgnoreCase(c.getEmailDomain()))
                .findFirst()
                .orElseThrow();

        RegisterRequest req = new RegisterRequest();
        req.setEmail("unauthorized@gmail.com"); // Non-college domain
        req.setPassword("Password123!");
        req.setCollegeId(anurag.getId());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}
