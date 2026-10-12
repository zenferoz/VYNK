package com.vynk;

import com.vynk.auth.email.EmailService;
import com.vynk.auth.entity.EmailVerificationToken;
import com.vynk.auth.repository.EmailVerificationTokenRepository;
import com.vynk.auth.service.EmailVerificationService;
import com.vynk.college.entity.College;
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

    private EmailVerificationTokenRepository tokenRepository;
    private UserRepository userRepository;
    private EmailService emailService;
    private EmailVerificationService verificationService;

    private User testUser;
    private College college;

    @BeforeEach
    void setUp() throws Exception {
        tokenRepository = Mockito.mock(EmailVerificationTokenRepository.class);
        userRepository = Mockito.mock(UserRepository.class);
        emailService = Mockito.mock(EmailService.class);

        verificationService = new EmailVerificationService(
                tokenRepository,
                userRepository,
                emailService,
                "http://localhost:5173"
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
                LocalDateTime.now().minusHours(1) // Expired 1 hour ago
        );

        when(tokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(tokenEntity));
        // Atomic update returns 0 because token is expired
        when(tokenRepository.markTokenUsed(eq(tokenHash), any(LocalDateTime.class))).thenReturn(0);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> verificationService.verifyEmail(rawToken));

        assertTrue(ex.getMessage().contains("expired"));
        assertFalse(testUser.isEmailVerified());
    }

    @Test
    void alreadyUsedTokenIsRejected() {
        String rawToken = "used-token-123";
        String tokenHash = verificationService.hashToken(rawToken);

        EmailVerificationToken tokenEntity = new EmailVerificationToken(
                tokenHash,
                testUser,
                LocalDateTime.now().plusHours(12)
        );
        tokenEntity.setUsed(true);

        when(tokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(tokenEntity));
        when(tokenRepository.markTokenUsed(eq(tokenHash), any(LocalDateTime.class))).thenReturn(0);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> verificationService.verifyEmail(rawToken));

        assertTrue(ex.getMessage().contains("already been used"));
    }

    @Test
    void blankOrNullTokenFailsSafely() {
        assertThrows(IllegalArgumentException.class, () -> verificationService.verifyEmail(null));
        assertThrows(IllegalArgumentException.class, () -> verificationService.verifyEmail("   "));
    }

    @Test
    void atomicSingleUsePreventsConcurrentDoubleConsumption() {
        String rawToken = "concurrent-race-test-token";
        String tokenHash = verificationService.hashToken(rawToken);

        EmailVerificationToken tokenEntity = new EmailVerificationToken(
                tokenHash,
                testUser,
                LocalDateTime.now().plusHours(24)
        );

        when(tokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(tokenEntity));

        // First thread succeeds: returns 1
        when(tokenRepository.markTokenUsed(eq(tokenHash), any(LocalDateTime.class)))
                .thenReturn(1) // 1st invocation
                .thenReturn(0); // 2nd invocation (concurrent race)

        // First thread consumes token
        verificationService.verifyEmail(rawToken);
        assertTrue(testUser.isEmailVerified());

        // Mark entity as used to simulate post-commit state
        tokenEntity.setUsed(true);

        // Second concurrent thread tries to consume same token: rejected
        assertThrows(IllegalArgumentException.class, () -> verificationService.verifyEmail(rawToken));
    }

    @Test
    void tokenDigestStorageIntegrity() {
        when(tokenRepository.findByUserId(testUser.getId())).thenReturn(Optional.empty());

        String rawToken = verificationService.createVerificationToken(testUser);

        assertNotNull(rawToken);
        assertFalse(rawToken.isBlank());

        ArgumentCaptor<EmailVerificationToken> captor = ArgumentCaptor.forClass(EmailVerificationToken.class);
        verify(tokenRepository).save(captor.capture());

        EmailVerificationToken savedToken = captor.getValue();
        assertNotEquals(rawToken, savedToken.getTokenHash(), "Raw token must never be stored in database");
        assertEquals(64, savedToken.getTokenHash().length(), "Stored token must be a 64-char SHA-256 hex digest");
        assertEquals(verificationService.hashToken(rawToken), savedToken.getTokenHash());
        assertFalse(savedToken.isUsed());
    }

    @Test
    void resendVerificationTokenForPendingUser() {
        when(userRepository.findByEmail("student@test.edu")).thenReturn(Optional.of(testUser));
        when(tokenRepository.findByUserIdForUpdate(testUser.getId())).thenReturn(Optional.empty());

        verificationService.resendVerificationToken("student@test.edu");

        verify(emailService, times(1)).sendVerificationEmail(eq("student@test.edu"), contains("/verify-email?token="));
    }

    @Test
    void resendVerificationIgnoresNonExistentOrAlreadyVerifiedUserSilently() {
        // Already verified user
        testUser.setEmailVerified(true);
        testUser.setStatus(AccountStatus.ACTIVE);
        when(userRepository.findByEmail("student@test.edu")).thenReturn(Optional.of(testUser));

        verificationService.resendVerificationToken("student@test.edu");

        // Should NOT send email to already verified user
        verify(emailService, never()).sendVerificationEmail(anyString(), anyString());

        // Non-existent user
        when(userRepository.findByEmail("unknown@test.edu")).thenReturn(Optional.empty());
        verificationService.resendVerificationToken("unknown@test.edu");
        verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
    }

    @Test
    void resendVerificationEnforcesCooldownSilentlyWithoutThrowing() {
        EmailVerificationToken existing = new EmailVerificationToken(
                "hash-123",
                testUser,
                LocalDateTime.now().plusHours(24) // created seconds ago
        );

        when(userRepository.findByEmail("student@test.edu")).thenReturn(Optional.of(testUser));
        when(tokenRepository.findByUserIdForUpdate(testUser.getId())).thenReturn(Optional.of(existing));

        // Must complete silently without throwing exception to prevent enumeration
        assertDoesNotThrow(() -> verificationService.resendVerificationToken("student@test.edu"));

        // Must NOT send an email during cooldown
        verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
    }

    @Test
    void resendVerificationConcurrentRequestsOnlyOneSendsEmail() {
        when(userRepository.findByEmail("student@test.edu")).thenReturn(Optional.of(testUser));

        EmailVerificationToken initialToken = new EmailVerificationToken(
                "old-hash",
                testUser,
                LocalDateTime.now().minusHours(1)
        );

        EmailVerificationToken updatedToken = new EmailVerificationToken(
                "new-hash",
                testUser,
                LocalDateTime.now().plusHours(24)
        );

        // Simulates concurrent thread order under row lock:
        // Thread 1 receives initial token, updates it; Thread 2 unblocks and receives updated token on cooldown
        when(tokenRepository.findByUserIdForUpdate(testUser.getId()))
                .thenReturn(Optional.of(initialToken))
                .thenReturn(Optional.of(updatedToken));

        verificationService.resendVerificationToken("student@test.edu");
        verificationService.resendVerificationToken("student@test.edu");

        verify(emailService, times(1)).sendVerificationEmail(eq("student@test.edu"), anyString());
    }

    @Test
    void resendVerificationUniformBehaviorAcrossAllAccountStates() {
        // 1. Nonexistent user
        when(userRepository.findByEmail("unknown@test.edu")).thenReturn(Optional.empty());
        assertDoesNotThrow(() -> verificationService.resendVerificationToken("unknown@test.edu"));

        // 2. Active user
        User activeUser = new User();
        activeUser.setEmail("active@test.edu");
        activeUser.setStatus(AccountStatus.ACTIVE);
        activeUser.setEmailVerified(true);
        when(userRepository.findByEmail("active@test.edu")).thenReturn(Optional.of(activeUser));
        assertDoesNotThrow(() -> verificationService.resendVerificationToken("active@test.edu"));

        // 3. Cooldown pending user
        EmailVerificationToken activeCooldown = new EmailVerificationToken(
                "hash-cooldown",
                testUser,
                LocalDateTime.now().plusHours(24)
        );
        when(userRepository.findByEmail("pending@test.edu")).thenReturn(Optional.of(testUser));
        when(tokenRepository.findByUserIdForUpdate(testUser.getId())).thenReturn(Optional.of(activeCooldown));
        assertDoesNotThrow(() -> verificationService.resendVerificationToken("pending@test.edu"));

        // All 3 cases completed silently with zero emails sent
        verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
