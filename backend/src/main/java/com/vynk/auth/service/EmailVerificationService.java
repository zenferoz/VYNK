
package com.vynk.auth.service;

import com.vynk.auth.email.EmailService;
import com.vynk.auth.entity.EmailVerificationToken;
import com.vynk.auth.repository.EmailVerificationTokenRepository;
import com.vynk.user.entity.AccountStatus;
import com.vynk.user.entity.User;
import com.vynk.user.repository.UserRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Locale;

@Service
public class EmailVerificationService {

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final String frontendBaseUrl;
    private final SecureRandom secureRandom = new SecureRandom();

    public EmailVerificationService(
            EmailVerificationTokenRepository tokenRepository,
            UserRepository userRepository,
            EmailService emailService,
            @Value("${vynk.frontend.base-url:http://localhost:5173}")
            String frontendBaseUrl) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @Transactional
    public String createVerificationToken(User user) {

        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        String rawToken = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        String tokenHash = hashToken(rawToken);

        EmailVerificationToken verificationToken = tokenRepository
                .findByUserId(user.getId())
                .orElseGet(() -> new EmailVerificationToken(
                        tokenHash,
                        user,
                        LocalDateTime.now().plusHours(24)
                ));

        verificationToken.setTokenHash(tokenHash);
        verificationToken.setExpiresAt(LocalDateTime.now().plusHours(24));
        verificationToken.setUsed(false);

        tokenRepository.save(verificationToken);

        return rawToken;
    }

    @Transactional
    public void verifyEmail(String rawToken) {

        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException(
                    "Verification token is required.");
        }

        String tokenHash = hashToken(rawToken.trim());

        EmailVerificationToken verificationToken =
                tokenRepository.findByTokenHash(tokenHash)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Invalid or expired verification token."));

        if (verificationToken.isUsed() && verificationToken.getUser().isEmailVerified()) {
            throw new IllegalArgumentException(
                    "This email has already been verified.");
        }

        // Atomic conditional token consumption: guarantees single-use even under concurrent requests
        int updated = tokenRepository.markTokenUsed(tokenHash, LocalDateTime.now());
        if (updated == 0) {
            if (verificationToken.isUsed()) {
                throw new IllegalArgumentException(
                        "This verification token has already been used.");
            }
            if (!verificationToken.getExpiresAt().isAfter(LocalDateTime.now())) {
                throw new IllegalArgumentException(
                        "Verification token has expired.");
            }
            throw new IllegalArgumentException(
                    "Invalid or expired verification token.");
        }

        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        user.setStatus(AccountStatus.ACTIVE);
    }

    @Transactional
    public void resendVerificationToken(String email) {

        if (email == null || email.isBlank()) {
            return;
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        userRepository.findByEmail(normalizedEmail)
                .or(() -> userRepository.findByEmailIgnoreCase(normalizedEmail))
                .ifPresent(user -> {
            if (user.getStatus() == AccountStatus.PENDING && !user.isEmailVerified()) {
                // Concurrency-safe atomic check using pessimistic lock on the user's token record
                EmailVerificationToken existing = tokenRepository.findByUserIdForUpdate(user.getId()).orElse(null);
                if (existing != null && existing.getExpiresAt() != null) {
                    LocalDateTime approxCreated = existing.getExpiresAt().minusHours(24);
                    if (approxCreated.plusSeconds(60).isAfter(LocalDateTime.now())) {
                        // Cooldown is active: return silently to prevent account enumeration side-channels
                        return;
                    }
                }

                String rawToken = createVerificationToken(user);
                String verificationLink = frontendBaseUrl.replaceAll("/+$", "")
                        + "/verify-email?token="
                        + java.net.URLEncoder.encode(rawToken, StandardCharsets.UTF_8);

                final String recipient = user.getEmail();
                final String link = verificationLink;

                if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) {
                    org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                        new org.springframework.transaction.support.TransactionSynchronization() {
                            @Override
                            public void afterCommit() {
                                try {
                                    emailService.sendVerificationEmail(recipient, link);
                                } catch (Exception ex) {
                                    org.slf4j.LoggerFactory.getLogger(EmailVerificationService.class)
                                            .error("Failed to deliver resend verification email to {} after transaction commit: {}", recipient, ex.getMessage());
                                }
                            }
                        }
                    );
                } else {
                    try {
                        emailService.sendVerificationEmail(recipient, link);
                    } catch (Exception ex) {
                        org.slf4j.LoggerFactory.getLogger(EmailVerificationService.class)
                                .error("Failed to deliver resend verification email to {}: {}", recipient, ex.getMessage());
                    }
                }
            }
        });
    }

    public String hashToken(String token) {

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    token.getBytes(StandardCharsets.UTF_8)
            );

            return java.util.HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is unavailable.", exception);
        }
    }
}
