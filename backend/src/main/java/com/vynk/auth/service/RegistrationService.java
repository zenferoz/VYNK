
package com.vynk.auth.service;

import com.vynk.auth.dto.RegisterRequest;
import com.vynk.auth.email.EmailService;
import com.vynk.college.entity.College;
import com.vynk.college.repository.CollegeRepository;
import com.vynk.user.entity.AccountStatus;
import com.vynk.user.entity.User;
import com.vynk.user.entity.UserRole;
import com.vynk.user.repository.UserRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {

    private final UserRepository userRepository;
    private final CollegeRepository collegeRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;
    private final EmailService emailService;
    private final String frontendBaseUrl;

    public RegistrationService(
            UserRepository userRepository,
            CollegeRepository collegeRepository,
            PasswordEncoder passwordEncoder,
            EmailVerificationService emailVerificationService,
            EmailService emailService,
            @Value("${vynk.frontend.base-url:http://localhost:5173}")
            String frontendBaseUrl) {

        this.userRepository = userRepository;
        this.collegeRepository = collegeRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailVerificationService = emailVerificationService;
        this.emailService = emailService;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @Transactional
    public void register(RegisterRequest request) {

        String email = request.getEmail()
                .trim()
                .toLowerCase(java.util.Locale.ROOT);

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException(
                    "An account with this email already exists.");
        }

        College college = collegeRepository
                .findById(request.getCollegeId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Invalid college ID."));

        if (!college.isActive()) {
            throw new IllegalArgumentException(
                    "This college is currently inactive.");
        }

        String emailDomain =
                email.substring(email.lastIndexOf('@') + 1);

        if (!emailDomain.equalsIgnoreCase(
                college.getEmailDomain().trim())) {
            throw new IllegalArgumentException(
                    "Please use your college email address.");
        }

        User user = new User();

        user.setEmail(email);
        user.setPasswordHash(
                passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRole.STUDENT);
        user.setStatus(AccountStatus.PENDING);
        user.setEmailVerified(false);
        user.setCollege(college);

        User savedUser = userRepository.save(user);

        String rawToken =
                emailVerificationService.createVerificationToken(
                        savedUser);

        String verificationLink =
                frontendBaseUrl.replaceAll("/+$", "")
                + "/verify-email?token="
                + java.net.URLEncoder.encode(
                        rawToken,
                        java.nio.charset.StandardCharsets.UTF_8);

        final String recipient = savedUser.getEmail();
        final String link = verificationLink;

        if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            emailService.sendVerificationEmail(recipient, link);
                        } catch (Exception ex) {
                            org.slf4j.LoggerFactory.getLogger(RegistrationService.class)
                                    .error("Failed to send verification email to {} after transaction commit: {}", recipient, ex.getMessage());
                        }
                    }
                }
            );
        } else {
            try {
                emailService.sendVerificationEmail(recipient, link);
            } catch (Exception ex) {
                org.slf4j.LoggerFactory.getLogger(RegistrationService.class)
                        .error("Failed to send verification email to {}: {}", recipient, ex.getMessage());
            }
        }
    }
}
