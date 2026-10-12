package com.vynk.auth.service;

import com.vynk.auth.email.EmailService;
import com.vynk.auth.entity.EmailVerificationOtp;
import com.vynk.auth.entity.EmailVerificationToken;
import com.vynk.auth.repository.EmailVerificationOtpRepository;
import com.vynk.auth.repository.EmailVerificationTokenRepository;
import com.vynk.security.ratelimit.RateLimitExceededException;
import com.vynk.user.entity.AccountStatus;
import com.vynk.user.entity.User;
import com.vynk.user.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Service
public class EmailVerificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);

    private final EmailVerificationOtpRepository otpRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final String frontendBaseUrl;
    private final String pepper;
    private final int expiryMinutes;
    private final int resendCooldownSeconds;
    private final int maxAttempts;
    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    public EmailVerificationService(
            EmailVerificationOtpRepository otpRepository,
            EmailVerificationTokenRepository tokenRepository,
            UserRepository userRepository,
            EmailService emailService,
            @Value("${vynk.frontend.base-url:http://localhost:5173}") String frontendBaseUrl,
            @Value("${vynk.otp.pepper:}") String pepper,
            @Value("${vynk.otp.expiry-minutes:5}") int expiryMinutes,
            @Value("${vynk.otp.resend-cooldown-seconds:60}") int resendCooldownSeconds,
            @Value("${vynk.otp.max-attempts:5}") int maxAttempts) {
        this.otpRepository = otpRepository;
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.frontendBaseUrl = frontendBaseUrl;

        if (pepper == null || pepper.isBlank()) {
            throw new IllegalStateException("VYNK_OTP_PEPPER is required and must not be blank.");
        }
        this.pepper = pepper.trim();
        this.expiryMinutes = expiryMinutes > 0 ? expiryMinutes : 5;
        this.resendCooldownSeconds = resendCooldownSeconds > 0 ? resendCooldownSeconds : 60;
        this.maxAttempts = maxAttempts > 0 ? maxAttempts : 5;
    }

    /**
     * Generates, securely hashes, stores, and dispatches a 6-digit OTP to the student.
     *
     * @param user Unverified student user entity
     * @return Raw 6-digit OTP string
     */
    @Transactional
    public String createAndSendOtp(User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("Valid user with identifier is required to generate OTP.");
        }

        int codeNum = secureRandom.nextInt(1_000_000);
        String rawOtp = String.format("%06d", codeNum);
        String otpHash = hashOtp(user.getId(), rawOtp);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(expiryMinutes);

        EmailVerificationOtp otpRecord;
        if (otpRepository != null) {
            otpRecord = otpRepository.findByUserIdForUpdate(user.getId())
                    .orElseGet(() -> new EmailVerificationOtp(user, otpHash, now, expiresAt));

            otpRecord.setOtpHash(otpHash);
            otpRecord.setCreatedAt(now);
            otpRecord.setExpiresAt(expiresAt);
            otpRecord.setAttempts(0);
            otpRecord.setUsed(false);

            otpRepository.save(otpRecord);
        }

        final String recipient = user.getEmail();
        final String otpCode = rawOtp;

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            emailService.sendVerificationOtp(recipient, otpCode);
                        }
                    }
            );
        } else {
            emailService.sendVerificationOtp(recipient, otpCode);
        }

        return rawOtp;
    }

    /**
     * Validates a student's submitted 6-digit OTP, enforcing expiration, attempt counts, and single-use.
     */
    @Transactional
    public void verifyOtp(String email, String rawOtp) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("College email is required.");
        }
        if (rawOtp == null || !rawOtp.trim().matches("^[0-9]{6}$")) {
            throw new IllegalArgumentException("Verification code must be exactly 6 numeric digits.");
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmail(normalizedEmail)
                .or(() -> userRepository.findByEmailIgnoreCase(normalizedEmail))
                .orElseThrow(() -> new IllegalArgumentException("Invalid verification code or email."));

        if (user.isEmailVerified()) {
            throw new IllegalArgumentException("This email has already been verified. Please sign in.");
        }

        if (otpRepository == null) {
            throw new IllegalStateException("OTP repository is not configured.");
        }

        EmailVerificationOtp otpRecord = otpRepository.findByUserIdForUpdate(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("No active verification code found. Please request a new code."));

        if (otpRecord.isUsed()) {
            throw new IllegalArgumentException("This verification code has already been used. Please request a new code.");
        }

        if (otpRecord.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Verification code has expired. Please request a new code.");
        }

        if (otpRecord.getAttempts() >= maxAttempts) {
            throw new IllegalArgumentException("Maximum verification attempts exceeded. Please request a new code.");
        }

        String expectedHash = hashOtp(user.getId(), rawOtp.trim());
        boolean matches = MessageDigest.isEqual(
                expectedHash.getBytes(StandardCharsets.UTF_8),
                otpRecord.getOtpHash().getBytes(StandardCharsets.UTF_8)
        );

        if (!matches) {
            int currentAttempts = otpRecord.getAttempts() + 1;
            otpRecord.setAttempts(currentAttempts);
            otpRepository.save(otpRecord);

            int remaining = maxAttempts - currentAttempts;
            if (remaining <= 0) {
                throw new IllegalArgumentException("Maximum verification attempts exceeded. Please request a new code.");
            } else {
                throw new IllegalArgumentException("Invalid verification code. " + remaining + " attempt" + (remaining == 1 ? "" : "s") + " remaining.");
            }
        }

        // Successfully verified: invalidate OTP and activate student account
        otpRecord.setUsed(true);
        otpRepository.save(otpRecord);

        user.setEmailVerified(true);
        user.setStatus(AccountStatus.ACTIVE);
        userRepository.save(user);

        log.info("Student account verified successfully via OTP for user ID [{}]", user.getId());
    }

    /**
     * Resends a fresh 6-digit verification OTP, enforcing 60-second cooldown and anti-enumeration protections.
     */
    @Transactional
    public void resendOtp(String email) {
        if (email == null || email.isBlank()) {
            return;
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        userRepository.findByEmail(normalizedEmail)
                .or(() -> userRepository.findByEmailIgnoreCase(normalizedEmail))
                .ifPresent(user -> {
                    if (user.getStatus() == AccountStatus.PENDING && !user.isEmailVerified()) {
                        if (otpRepository != null) {
                            EmailVerificationOtp existing = otpRepository.findByUserIdForUpdate(user.getId()).orElse(null);
                            if (existing != null && existing.getCreatedAt() != null) {
                                LocalDateTime cooldownEnd = existing.getCreatedAt().plusSeconds(resendCooldownSeconds);
                                if (cooldownEnd.isAfter(LocalDateTime.now())) {
                                    long waitSecs = Duration.between(LocalDateTime.now(), cooldownEnd).getSeconds() + 1;
                                    throw new RateLimitExceededException(
                                            "Please wait " + waitSecs + " seconds before requesting a new code.",
                                            waitSecs
                                    );
                                }
                            }
                        }
                        createAndSendOtp(user);
                    }
                });
    }

    /**
     * Computes HMAC-SHA256 with the server-side pepper and user ID as salt to prevent rainbow-table search.
     */
    public String hashOtp(Long userId, String rawOtp) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(pepper.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hmacBytes = mac.doFinal((userId + ":" + rawOtp).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hmacBytes);
        } catch (Exception ex) {
            throw new IllegalStateException("HmacSHA256 is unavailable for OTP hashing.", ex);
        }
    }

    // =========================================================================
    // Legacy Verification Link Support (Preserved for backward compatibility)
    // =========================================================================

    @Transactional
    public String createVerificationToken(User user) {
        if (tokenRepository == null) {
            return "";
        }
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
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
        if (tokenRepository == null) {
            throw new IllegalStateException("Token repository is not configured.");
        }
        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException("Verification token is required.");
        }

        String tokenHash = hashToken(rawToken.trim());
        EmailVerificationToken verificationToken = tokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired verification token."));

        if (verificationToken.isUsed() && verificationToken.getUser().isEmailVerified()) {
            throw new IllegalArgumentException("This email has already been verified.");
        }

        int updated = tokenRepository.markTokenUsed(tokenHash, LocalDateTime.now());
        if (updated == 0) {
            if (verificationToken.isUsed()) {
                throw new IllegalArgumentException("This verification token has already been used.");
            }
            if (!verificationToken.getExpiresAt().isAfter(LocalDateTime.now())) {
                throw new IllegalArgumentException("Verification token has expired.");
            }
            throw new IllegalArgumentException("Invalid or expired verification token.");
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
                        if (tokenRepository != null) {
                            EmailVerificationToken existing = tokenRepository.findByUserIdForUpdate(user.getId()).orElse(null);
                            if (existing != null && existing.getExpiresAt() != null) {
                                LocalDateTime approxCreated = existing.getExpiresAt().minusHours(24);
                                if (approxCreated.plusSeconds(60).isAfter(LocalDateTime.now())) {
                                    return;
                                }
                            }
                        }

                        String rawToken = createVerificationToken(user);
                        String verificationLink = frontendBaseUrl.replaceAll("/+$", "")
                                + "/verify-email?token="
                                + java.net.URLEncoder.encode(rawToken, StandardCharsets.UTF_8);

                        final String recipient = user.getEmail();
                        final String link = verificationLink;

                        if (TransactionSynchronizationManager.isActualTransactionActive()) {
                            TransactionSynchronizationManager.registerSynchronization(
                                    new TransactionSynchronization() {
                                        @Override
                                        public void afterCommit() {
                                            try {
                                                emailService.sendVerificationEmail(recipient, link);
                                            } catch (Exception ex) {
                                                log.error("Failed to deliver resend verification email to [{}] after commit: {}",
                                                        EmailService.maskEmail(recipient), ex.getMessage());
                                            }
                                        }
                                    }
                            );
                        } else {
                            try {
                                emailService.sendVerificationEmail(recipient, link);
                            } catch (Exception ex) {
                                log.error("Failed to deliver resend verification email to [{}]: {}",
                                        EmailService.maskEmail(recipient), ex.getMessage());
                            }
                        }
                    }
                });
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
