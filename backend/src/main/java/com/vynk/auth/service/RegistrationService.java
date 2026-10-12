package com.vynk.auth.service;

import com.vynk.auth.dto.RegisterRequest;
import com.vynk.college.entity.College;
import com.vynk.college.repository.CollegeRepository;
import com.vynk.user.entity.AccountStatus;
import com.vynk.user.entity.User;
import com.vynk.user.entity.UserRole;
import com.vynk.user.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class RegistrationService {

    private final UserRepository userRepository;
    private final CollegeRepository collegeRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;

    public RegistrationService(
            UserRepository userRepository,
            CollegeRepository collegeRepository,
            PasswordEncoder passwordEncoder,
            EmailVerificationService emailVerificationService) {
        this.userRepository = userRepository;
        this.collegeRepository = collegeRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailVerificationService = emailVerificationService;
    }

    @Transactional
    public void register(RegisterRequest request) {
        String email = request.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        // Check if user already exists
        User existingUser = userRepository.findByEmail(email).orElse(null);
        if (existingUser != null) {
            if (existingUser.isEmailVerified() || existingUser.getStatus() != AccountStatus.PENDING) {
                throw new IllegalArgumentException("An account with this email already exists.");
            }

            // Safe recovery/retry flow for unverified PENDING student
            College college = collegeRepository.findById(request.getCollegeId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid college ID."));

            if (!college.isActive()) {
                throw new IllegalArgumentException("This college is currently inactive.");
            }

            String emailDomain = email.substring(email.lastIndexOf('@') + 1);
            if (!emailDomain.equalsIgnoreCase(college.getEmailDomain().trim())) {
                throw new IllegalArgumentException("Please use your college email address.");
            }

            existingUser.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            existingUser.setCollege(college);
            User savedUser = userRepository.save(existingUser);

            emailVerificationService.createAndSendOtp(savedUser);
            return;
        }

        College college = collegeRepository
                .findById(request.getCollegeId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid college ID."));

        if (!college.isActive()) {
            throw new IllegalArgumentException("This college is currently inactive.");
        }

        String emailDomain = email.substring(email.lastIndexOf('@') + 1);

        if (!emailDomain.equalsIgnoreCase(college.getEmailDomain().trim())) {
            throw new IllegalArgumentException("Please use your college email address.");
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRole.STUDENT);
        user.setStatus(AccountStatus.PENDING);
        user.setEmailVerified(false);
        user.setCollege(college);

        User savedUser = userRepository.save(user);

        emailVerificationService.createAndSendOtp(savedUser);
    }
}
