package com.vynk;

import com.vynk.auth.email.EmailService;
import com.vynk.auth.entity.PasswordResetToken;
import com.vynk.auth.repository.PasswordResetTokenRepository;
import com.vynk.auth.service.PasswordResetService;
import com.vynk.user.entity.AccountStatus;
import com.vynk.user.entity.User;
import com.vynk.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PasswordResetServiceTest {

    private PasswordResetTokenRepository tokenRepository;
    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private EmailService emailService;
    private org.springframework.security.core.session.SessionRegistry sessionRegistry;
    private PasswordResetService resetService;

    private User activeUser;

    @BeforeEach
    void setUp() throws Exception {
        tokenRepository = mock(PasswordResetTokenRepository.class);
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        emailService = mock(EmailService.class);
        sessionRegistry = mock(org.springframework.security.core.session.SessionRegistry.class);

        resetService = new PasswordResetService(
                tokenRepository,
                userRepository,
                passwordEncoder,
                emailService,
                "http://localhost:5173",
                sessionRegistry
        );

        activeUser = new User();
        var idField = User.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(activeUser, 42L);

        activeUser.setEmail("student@college.edu");
        activeUser.setStatus(AccountStatus.ACTIVE);
        activeUser.setEmailVerified(true);
        activeUser.setPasswordHash("oldHash");

        when(passwordEncoder.encode(anyString())).thenAnswer(i -> "encoded_" + i.getArgument(0));
    }

    @Test
    void requestPasswordResetForUnknownEmailReturnsNeutrallyWithoutError() {
        when(userRepository.findByEmailIgnoreCase("unknown@college.edu")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> resetService.requestPasswordReset("unknown@college.edu"));

        verify(tokenRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    void requestPasswordResetForUnverifiedUserReturnsNeutrallyWithoutDispatch() {
        User unverifiedUser = new User();
        unverifiedUser.setEmail("pending@college.edu");
        unverifiedUser.setStatus(AccountStatus.PENDING);
        unverifiedUser.setEmailVerified(false);

        when(userRepository.findByEmailIgnoreCase("pending@college.edu")).thenReturn(Optional.of(unverifiedUser));

        assertDoesNotThrow(() -> resetService.requestPasswordReset("pending@college.edu"));

        verify(tokenRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    void requestPasswordResetCreatesHashedTokenAndDispatchesEmail() {
        when(userRepository.findByEmailIgnoreCase("student@college.edu")).thenReturn(Optional.of(activeUser));
        when(tokenRepository.findByUserIdForUpdate(42L)).thenReturn(Optional.empty());

        resetService.requestPasswordReset("student@college.edu");

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());

        PasswordResetToken savedToken = tokenCaptor.getValue();
        assertNotNull(savedToken.getTokenHash());
        assertEquals(64, savedToken.getTokenHash().length());
        assertFalse(savedToken.isUsed());
        assertTrue(savedToken.getExpiresAt().isAfter(LocalDateTime.now()));

        verify(emailService).sendPasswordResetEmail(eq("student@college.edu"), contains("/reset-password?token="));
    }

    @Test
    void requestPasswordResetEnforcesCooldown() {
        PasswordResetToken existingToken = new PasswordResetToken("hash", activeUser, LocalDateTime.now().plusHours(1));
        when(userRepository.findByEmailIgnoreCase("student@college.edu")).thenReturn(Optional.of(activeUser));
        when(tokenRepository.findByUserIdForUpdate(42L)).thenReturn(Optional.of(existingToken));

        resetService.requestPasswordReset("student@college.edu");

        verify(tokenRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    void resetPasswordSucceedsWithValidTokenAndEncodesPassword() {
        String rawToken = "sampleRawToken123";
        String tokenHash = resetService.hashToken(rawToken);

        PasswordResetToken token = new PasswordResetToken(tokenHash, activeUser, LocalDateTime.now().plusMinutes(30));
        when(tokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(token));
        when(tokenRepository.markTokenUsed(eq(tokenHash), any())).thenReturn(1);

        resetService.resetPassword(rawToken, "SuperSecret99");

        verify(tokenRepository).markTokenUsed(eq(tokenHash), any());
        assertEquals("encoded_SuperSecret99", activeUser.getPasswordHash());
        verify(userRepository).save(activeUser);
    }

    @Test
    void resetPasswordRejectsExpiredToken() {
        String rawToken = "expiredRawToken";
        String tokenHash = resetService.hashToken(rawToken);

        PasswordResetToken token = new PasswordResetToken(tokenHash, activeUser, LocalDateTime.now().minusMinutes(5));
        when(tokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(token));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                resetService.resetPassword(rawToken, "NewPass1234")
        );
        assertTrue(ex.getMessage().contains("expired"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPasswordRejectsAlreadyUsedToken() {
        String rawToken = "usedRawToken";
        String tokenHash = resetService.hashToken(rawToken);

        PasswordResetToken token = new PasswordResetToken(tokenHash, activeUser, LocalDateTime.now().plusMinutes(20));
        token.setUsed(true);
        when(tokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(token));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                resetService.resetPassword(rawToken, "NewPass1234")
        );
        assertTrue(ex.getMessage().contains("already been used"));
    }

    @Test
    void resetPasswordRejectsInvalidToken() {
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                resetService.resetPassword("nonexistentToken", "NewPass1234")
        );
    }

    @Test
    void resetPasswordRejectsWeakOrInvalidPassword() {
        assertThrows(IllegalArgumentException.class, () ->
                resetService.resetPassword("someToken", "short")
        );
    }

    @Test
    void resetPasswordExpiresActiveSessionsViaSessionRegistry() {
        String rawToken = "sessionToken";
        String tokenHash = resetService.hashToken(rawToken);

        PasswordResetToken token = new PasswordResetToken(tokenHash, activeUser, LocalDateTime.now().plusMinutes(20));
        when(tokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(token));
        when(tokenRepository.markTokenUsed(eq(tokenHash), any())).thenReturn(1);

        org.springframework.security.core.userdetails.UserDetails userDetails =
                mock(org.springframework.security.core.userdetails.UserDetails.class);
        when(userDetails.getUsername()).thenReturn(activeUser.getEmail());

        org.springframework.security.core.session.SessionInformation sessionInfo =
                mock(org.springframework.security.core.session.SessionInformation.class);

        when(sessionRegistry.getAllPrincipals()).thenReturn(java.util.List.of(userDetails));
        when(sessionRegistry.getAllSessions(userDetails, false)).thenReturn(java.util.List.of(sessionInfo));

        resetService.resetPassword(rawToken, "NewPass1234");

        verify(sessionInfo).expireNow();
    }
}
