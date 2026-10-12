package com.vynk;

import com.vynk.auth.email.EmailService;
import com.vynk.auth.entity.EmailVerificationOtp;
import com.vynk.auth.entity.EmailVerificationToken;
import com.vynk.auth.repository.EmailVerificationOtpRepository;
import com.vynk.auth.repository.EmailVerificationTokenRepository;
import com.vynk.auth.service.EmailVerificationService;
import com.vynk.college.entity.College;
import com.vynk.security.ratelimit.RateLimitExceededException;
import com.vynk.user.entity.AccountStatus;
import com.vynk.user.entity.User;
import com.vynk.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EmailVerificationServiceTest {

    private EmailVerificationOtpRepository otpRepository;
    private EmailVerificationTokenRepository tokenRepository;
    private UserRepository userRepository;
    private EmailService emailService;
    private EmailVerificationService verificationService;

    private User testUser;
    private College college;
    private final String testPepper = "test-secure-pepper-secret-2026";

    @BeforeEach
    void setUp() throws Exception {
        otpRepository = Mockito.mock(EmailVerificationOtpRepository.class);
        tokenRepository = Mockito.mock(EmailVerificationTokenRepository.class);
        userRepository = Mockito.mock(UserRepository.class);
        emailService = Mockito.mock(EmailService.class);

        verificationService = new EmailVerificationService(
                otpRepository,
                tokenRepository,
                userRepository,
                emailService,
                "http://localhost:5173",
                testPepper,
                5,  // 5 minute expiry
                60, // 60 second cooldown
                5   // 5 max attempts
        );

        college = new College();
        setField(college, "id", 1L);
        college.setName("Test University");
        college.setEmailDomain("test.edu");

        testUser = new User();
        setField(testUser, "id", 100L);
        testUser.setEmail("student@test.edu");
        testUser.setStatus(AccountStatus.PENDING);
        testUser.setEmailVerified(false);
        testUser.setCollege(college);
    }

    // =========================================================================
    // Pepper Requirement & Lifecycle Validation Tests
    // =========================================================================

    @Test
    void constructorThrowsIllegalStateExceptionWhenPepperIsNull() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                new EmailVerificationService(
                        otpRepository, tokenRepository, userRepository, emailService,
                        "http://localhost:5173", null, 5, 60, 5
                ));
        assertEquals("VYNK_OTP_PEPPER is required and must not be blank.", ex.getMessage());
    }

    @Test
    void constructorThrowsIllegalStateExceptionWhenPepperIsBlank() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                new EmailVerificationService(
                        otpRepository, tokenRepository, userRepository, emailService,
                        "http://localhost:5173", "   ", 5, 60, 5
                ));
        assertEquals("VYNK_OTP_PEPPER is required and must not be blank.", ex.getMessage());
    }

    // =========================================================================
    // OTP Generation and Hashing Tests
    // =========================================================================

    @Test
    void createAndSendOtpGenerates6DigitCodeAndHashesSecurely() {
        when(otpRepository.findByUserIdForUpdate(testUser.getId())).thenReturn(Optional.empty());

        String rawOtp = verificationService.createAndSendOtp(testUser);

        assertNotNull(rawOtp);
        assertEquals(6, rawOtp.length());
        assertTrue(rawOtp.matches("^[0-9]{6}$"));

        ArgumentCaptor<EmailVerificationOtp> captor = ArgumentCaptor.forClass(EmailVerificationOtp.class);
        verify(otpRepository).save(captor.capture());

        EmailVerificationOtp saved = captor.getValue();
        assertEquals(testUser, saved.getUser());
        assertFalse(saved.isUsed());
        assertEquals(0, saved.getAttempts());
        assertNotNull(saved.getExpiresAt());
        assertTrue(saved.getExpiresAt().isAfter(LocalDateTime.now()));

        // Verify the saved hash matches HMAC-SHA256 with pepper
        String expectedHash = verificationService.hashOtp(testUser.getId(), rawOtp);
        assertEquals(expectedHash, saved.getOtpHash());

        verify(emailService).sendVerificationOtp(eq("student@test.edu"), eq(rawOtp));
    }

    // =========================================================================
    // OTP Verification Tests
    // =========================================================================

    @Test
    void verifyOtpWithValidCodeActivatesUserAndMarksUsed() {
        String rawOtp = "654321";
        String otpHash = verificationService.hashOtp(testUser.getId(), rawOtp);

        EmailVerificationOtp otpRecord = new EmailVerificationOtp(
                testUser,
                otpHash,
                LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now().plusMinutes(4)
        );

        when(userRepository.findByEmail("student@test.edu")).thenReturn(Optional.of(testUser));
        when(otpRepository.findByUserIdForUpdate(testUser.getId())).thenReturn(Optional.of(otpRecord));

        verificationService.verifyOtp("student@test.edu", rawOtp);

        assertTrue(otpRecord.isUsed());
        verify(otpRepository).save(otpRecord);

        assertTrue(testUser.isEmailVerified());
        assertEquals(AccountStatus.ACTIVE, testUser.getStatus());
        verify(userRepository).save(testUser);
    }

    @Test
    void verifyOtpWithInvalidCodeIncrementsAttemptsAndRejects() {
        String validOtp = "123456";
        String invalidOtp = "999999";
        String otpHash = verificationService.hashOtp(testUser.getId(), validOtp);

        EmailVerificationOtp otpRecord = new EmailVerificationOtp(
                testUser,
                otpHash,
                LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now().plusMinutes(4)
        );
        otpRecord.setAttempts(1);

        when(userRepository.findByEmail("student@test.edu")).thenReturn(Optional.of(testUser));
        when(otpRepository.findByUserIdForUpdate(testUser.getId())).thenReturn(Optional.of(otpRecord));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> verificationService.verifyOtp("student@test.edu", invalidOtp));

        assertTrue(ex.getMessage().contains("Invalid verification code"));
        assertTrue(ex.getMessage().contains("3 attempts remaining"));
        assertEquals(2, otpRecord.getAttempts());
        assertFalse(otpRecord.isUsed());
        assertFalse(testUser.isEmailVerified());
        assertEquals(AccountStatus.PENDING, testUser.getStatus());
        verify(otpRepository).save(otpRecord);
    }

    @Test
    void verifyOtpLockoutAfterMaxAttemptsExceeded() {
        String validOtp = "123456";
        String invalidOtp = "000000";
        String otpHash = verificationService.hashOtp(testUser.getId(), validOtp);

        EmailVerificationOtp otpRecord = new EmailVerificationOtp(
                testUser,
                otpHash,
                LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now().plusMinutes(4)
        );
        otpRecord.setAttempts(4); // 4th attempt already used; next failure triggers lockout

        when(userRepository.findByEmail("student@test.edu")).thenReturn(Optional.of(testUser));
        when(otpRepository.findByUserIdForUpdate(testUser.getId())).thenReturn(Optional.of(otpRecord));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> verificationService.verifyOtp("student@test.edu", invalidOtp));

        assertTrue(ex.getMessage().contains("Maximum verification attempts exceeded"));
        assertEquals(5, otpRecord.getAttempts());
        assertFalse(testUser.isEmailVerified());
    }

    @Test
    void verifyOtpRejectsAlreadyLockedOutRecord() {
        String rawOtp = "123456";
        String otpHash = verificationService.hashOtp(testUser.getId(), rawOtp);

        EmailVerificationOtp otpRecord = new EmailVerificationOtp(
                testUser,
                otpHash,
                LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now().plusMinutes(4)
        );
        otpRecord.setAttempts(5); // Locked out

        when(userRepository.findByEmail("student@test.edu")).thenReturn(Optional.of(testUser));
        when(otpRepository.findByUserIdForUpdate(testUser.getId())).thenReturn(Optional.of(otpRecord));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> verificationService.verifyOtp("student@test.edu", rawOtp));

        assertTrue(ex.getMessage().contains("Maximum verification attempts exceeded"));
    }

    @Test
    void verifyOtpRejectsExpiredCode() {
        String rawOtp = "123456";
        String otpHash = verificationService.hashOtp(testUser.getId(), rawOtp);

        EmailVerificationOtp expiredOtp = new EmailVerificationOtp(
                testUser,
                otpHash,
                LocalDateTime.now().minusMinutes(10),
                LocalDateTime.now().minusMinutes(5) // Expired 5 mins ago
        );

        when(userRepository.findByEmail("student@test.edu")).thenReturn(Optional.of(testUser));
        when(otpRepository.findByUserIdForUpdate(testUser.getId())).thenReturn(Optional.of(expiredOtp));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> verificationService.verifyOtp("student@test.edu", rawOtp));

        assertTrue(ex.getMessage().contains("Verification code has expired"));
        assertFalse(testUser.isEmailVerified());
    }

    @Test
    void verifyOtpRejectsAlreadyUsedCode() {
        String rawOtp = "123456";
        String otpHash = verificationService.hashOtp(testUser.getId(), rawOtp);

        EmailVerificationOtp usedOtp = new EmailVerificationOtp(
                testUser,
                otpHash,
                LocalDateTime.now().minusMinutes(2),
                LocalDateTime.now().plusMinutes(3)
        );
        usedOtp.setUsed(true);

        when(userRepository.findByEmail("student@test.edu")).thenReturn(Optional.of(testUser));
        when(otpRepository.findByUserIdForUpdate(testUser.getId())).thenReturn(Optional.of(usedOtp));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> verificationService.verifyOtp("student@test.edu", rawOtp));

        assertTrue(ex.getMessage().contains("already been used"));
    }

    @Test
    void verifyOtpRejectsAlreadyVerifiedUser() {
        testUser.setEmailVerified(true);
        testUser.setStatus(AccountStatus.ACTIVE);

        when(userRepository.findByEmail("student@test.edu")).thenReturn(Optional.of(testUser));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> verificationService.verifyOtp("student@test.edu", "123456"));

        assertTrue(ex.getMessage().contains("already been verified"));
    }

    @Test
    void verifyOtpRejectsMalformedOtpFormat() {
        assertThrows(IllegalArgumentException.class,
                () -> verificationService.verifyOtp("student@test.edu", "12345"));
        assertThrows(IllegalArgumentException.class,
                () -> verificationService.verifyOtp("student@test.edu", "1234567"));
        assertThrows(IllegalArgumentException.class,
                () -> verificationService.verifyOtp("student@test.edu", "abcdef"));
        assertThrows(IllegalArgumentException.class,
                () -> verificationService.verifyOtp("student@test.edu", ""));
    }

    // =========================================================================
    // OTP Resend and Cooldown Tests
    // =========================================================================

    @Test
    void resendOtpDispatchesFreshCodeWhenCooldownElapsed() {
        EmailVerificationOtp existingOtp = new EmailVerificationOtp(
                testUser,
                "old-hash",
                LocalDateTime.now().minusSeconds(65), // Cooldown elapsed
                LocalDateTime.now().plusMinutes(3)
        );

        when(userRepository.findByEmail("student@test.edu")).thenReturn(Optional.of(testUser));
        when(otpRepository.findByUserIdForUpdate(testUser.getId())).thenReturn(Optional.of(existingOtp));

        verificationService.resendOtp("student@test.edu");

        verify(emailService, times(1)).sendVerificationOtp(eq("student@test.edu"), anyString());
    }

    @Test
    void resendOtpEnforcesCooldownAndThrowsRateLimitException() {
        EmailVerificationOtp recentOtp = new EmailVerificationOtp(
                testUser,
                "current-hash",
                LocalDateTime.now().minusSeconds(20), // 40 seconds remaining on cooldown
                LocalDateTime.now().plusMinutes(4)
        );

        when(userRepository.findByEmail("student@test.edu")).thenReturn(Optional.of(testUser));
        when(otpRepository.findByUserIdForUpdate(testUser.getId())).thenReturn(Optional.of(recentOtp));

        RateLimitExceededException ex = assertThrows(RateLimitExceededException.class,
                () -> verificationService.resendOtp("student@test.edu"));

        assertTrue(ex.getMessage().contains("Please wait"));
        assertTrue(ex.getRetryAfterSeconds() > 0 && ex.getRetryAfterSeconds() <= 60);
        verify(emailService, never()).sendVerificationOtp(anyString(), anyString());
    }

    @Test
    void resendOtpSilentlyIgnoresUnknownOrActiveUsers() {
        // Unknown user
        when(userRepository.findByEmail("unknown@test.edu")).thenReturn(Optional.empty());
        assertDoesNotThrow(() -> verificationService.resendOtp("unknown@test.edu"));

        // Already active user
        User activeUser = new User();
        activeUser.setEmail("active@test.edu");
        activeUser.setStatus(AccountStatus.ACTIVE);
        activeUser.setEmailVerified(true);
        when(userRepository.findByEmail("active@test.edu")).thenReturn(Optional.of(activeUser));
        assertDoesNotThrow(() -> verificationService.resendOtp("active@test.edu"));

        verify(emailService, never()).sendVerificationOtp(anyString(), anyString());
    }

    // =========================================================================
    // Legacy Verification Link Tests (Backward Compatibility)
    // =========================================================================

    @Test
    void validTokenActivatesOnlyIntendedAccount() {
        String rawToken = "sample-secure-random-token-12345";
        String tokenHash = verificationService.hashToken(rawToken);

        EmailVerificationToken tokenEntity = new EmailVerificationToken(
                tokenHash,
                testUser,
                LocalDateTime.now().plusHours(24)
        );

        when(tokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(tokenEntity));
        when(tokenRepository.markTokenUsed(eq(tokenHash), any(LocalDateTime.class))).thenReturn(1);

        verificationService.verifyEmail(rawToken);

        assertTrue(testUser.isEmailVerified());
        assertEquals(AccountStatus.ACTIVE, testUser.getStatus());
        verify(tokenRepository, times(1)).markTokenUsed(eq(tokenHash), any(LocalDateTime.class));
    }

    @Test
    void invalidTokenIsRejected() {
        String rawToken = "completely-invalid-nonexistent-token";
        String tokenHash = verificationService.hashToken(rawToken);

        when(tokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> verificationService.verifyEmail(rawToken));

        assertTrue(ex.getMessage().contains("Invalid or expired"));
        assertFalse(testUser.isEmailVerified());
        assertEquals(AccountStatus.PENDING, testUser.getStatus());
    }

    @Test
    void expiredTokenIsRejected() {
        String rawToken = "expired-token-123";
        String tokenHash = verificationService.hashToken(rawToken);

        EmailVerificationToken tokenEntity = new EmailVerificationToken(
                tokenHash,
                testUser,
                LocalDateTime.now().minusHours(1)
        );

        when(tokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(tokenEntity));
        when(tokenRepository.markTokenUsed(eq(tokenHash), any(LocalDateTime.class))).thenReturn(0);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> verificationService.verifyEmail(rawToken));

        assertTrue(ex.getMessage().contains("expired"));
        assertFalse(testUser.isEmailVerified());
    }

    @Test
    void alreadyUsedTokenIsRejected() {
        String rawToken = "reused-token-123";
        String tokenHash = verificationService.hashToken(rawToken);

        EmailVerificationToken tokenEntity = new EmailVerificationToken(
                tokenHash,
                testUser,
                LocalDateTime.now().plusHours(2)
        );
        tokenEntity.setUsed(true);

        when(tokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(tokenEntity));
        when(tokenRepository.markTokenUsed(eq(tokenHash), any(LocalDateTime.class))).thenReturn(0);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> verificationService.verifyEmail(rawToken));

        assertTrue(ex.getMessage().contains("already been used"));
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
