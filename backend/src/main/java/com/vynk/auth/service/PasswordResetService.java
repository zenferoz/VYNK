package com.vynk.auth.service;

import com.vynk.auth.email.EmailService;
import com.vynk.auth.entity.PasswordResetToken;
import com.vynk.auth.repository.PasswordResetTokenRepository;
import com.vynk.user.entity.AccountStatus;
import com.vynk.user.entity.User;
import com.vynk.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final String frontendBaseUrl;
    private final org.springframework.security.core.session.SessionRegistry sessionRegistry;
    private final com.vynk.security.session.UserSessionInvalidationService userSessionInvalidationService;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetService(
            PasswordResetTokenRepository tokenRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            @Value("${vynk.frontend.base-url:http://localhost:5173}") String frontendBaseUrl) {
        this(tokenRepository, userRepository, passwordEncoder, emailService, frontendBaseUrl, null, null);
    }

    public PasswordResetService(
            PasswordResetTokenRepository tokenRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            @Value("${vynk.frontend.base-url:http://localhost:5173}") String frontendBaseUrl,
            org.springframework.security.core.session.SessionRegistry sessionRegistry) {
        this(tokenRepository, userRepository, passwordEncoder, emailService, frontendBaseUrl, sessionRegistry, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public PasswordResetService(
            PasswordResetTokenRepository tokenRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            @Value("${vynk.frontend.base-url:http://localhost:5173}") String frontendBaseUrl,
            @org.springframework.beans.factory.annotation.Autowired(required = false)
            org.springframework.security.core.session.SessionRegistry sessionRegistry,
            @org.springframework.beans.factory.annotation.Autowired(required = false)
            com.vynk.security.session.UserSessionInvalidationService userSessionInvalidationService) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.frontendBaseUrl = frontendBaseUrl;
        this.sessionRegistry = sessionRegistry;
        this.userSessionInvalidationService = userSessionInvalidationService;
    }

    @Transactional
    public void requestPasswordReset(String email) {
        if (email == null || email.isBlank()) {
            return;
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        userRepository.findByEmailIgnoreCase(normalizedEmail).ifPresent(user -> {
            // Only active and verified accounts may reset passwords
            if (user.getStatus() != AccountStatus.ACTIVE || !user.isEmailVerified()) {
                return;
            }

            // Cooldown check: prevent duplicate dispatch within 60 seconds
            PasswordResetToken existing = tokenRepository.findByUserIdForUpdate(user.getId()).orElse(null);
            if (existing != null && existing.getExpiresAt() != null) {
                LocalDateTime approxCreated = existing.getExpiresAt().minusHours(1);
                if (approxCreated.plusSeconds(60).isAfter(LocalDateTime.now())) {
                    return;
                }
            }

            // Generate cryptographically secure token
            byte[] randomBytes = new byte[32];
            secureRandom.nextBytes(randomBytes);
            String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
            String tokenHash = hashToken(rawToken);

            PasswordResetToken resetToken = existing != null ? existing : new PasswordResetToken(tokenHash, user, LocalDateTime.now().plusHours(1));
            resetToken.setTokenHash(tokenHash);
            resetToken.setExpiresAt(LocalDateTime.now().plusHours(1));
            resetToken.setUsed(false);

            tokenRepository.save(resetToken);

            String resetLink = frontendBaseUrl.replaceAll("/+$", "")
                    + "/reset-password?token="
                    + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);

            final String recipient = user.getEmail();
            final String link = resetLink;

            // Execute SMTP dispatch post-commit outside active database transaction
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            emailService.sendPasswordResetEmail(recipient, link);
                        } catch (Exception ex) {
                            log.error("Failed to deliver password reset email to {} after transaction commit: {}", recipient, ex.getMessage());
                        }
                    }
                });
            } else {
                try {
                    emailService.sendPasswordResetEmail(recipient, link);
                } catch (Exception ex) {
                    log.error("Failed to deliver password reset email to {}: {}", recipient, ex.getMessage());
                }
            }
        });
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException("Password reset token is required.");
        }

        if (newPassword == null || newPassword.length() < 8 || newPassword.length() > 72) {
            throw new IllegalArgumentException("Password must contain 8 to 72 characters.");
        }

        String tokenHash = hashToken(rawToken.trim());

        PasswordResetToken resetToken = tokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired password reset token."));

        if (resetToken.isUsed()) {
            throw new IllegalArgumentException("This password reset token has already been used.");
        }

        if (!resetToken.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Password reset token has expired.");
        }

        // Atomic conditional token consumption: guarantees single-use even under concurrent attempts
        int updated = tokenRepository.markTokenUsed(tokenHash, LocalDateTime.now());
        if (updated == 0) {
            throw new IllegalArgumentException("Invalid or expired password reset token.");
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Invalidate all active sessions for this user across all devices
        if (sessionRegistry != null) {
            for (Object principal : sessionRegistry.getAllPrincipals()) {
                boolean matches = false;
                if (principal instanceof org.springframework.security.core.userdetails.UserDetails ud) {
                    matches = ud.getUsername().equalsIgnoreCase(user.getEmail());
                } else if (principal instanceof String pName) {
                    matches = pName.equalsIgnoreCase(user.getEmail());
                } else if (principal instanceof java.security.Principal p) {
                    matches = p.getName().equalsIgnoreCase(user.getEmail());
                }
                if (matches) {
                    for (org.springframework.security.core.session.SessionInformation sessionInfo : sessionRegistry.getAllSessions(principal, false)) {
                        sessionInfo.expireNow();
                    }
                }
            }
        }

        // Record password reset cut-off to invalidate any active sessions not tracked in SessionRegistry
        if (userSessionInvalidationService != null) {
            userSessionInvalidationService.markPasswordReset(user.getEmail());
        }

        log.info("Password successfully reset for user ID: {}", user.getId());
    }

    public String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }
}
