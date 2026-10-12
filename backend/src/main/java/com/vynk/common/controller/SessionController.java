package com.vynk.common.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import com.vynk.common.exception.ResourceNotFoundException;
import com.vynk.profile.repository.StudentProfileRepository;
import com.vynk.user.entity.User;
import com.vynk.user.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/session")
public class SessionController {

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;

    public SessionController(
            UserRepository userRepository,
            StudentProfileRepository studentProfileRepository) {
        this.userRepository = userRepository;
        this.studentProfileRepository = studentProfileRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(
            Authentication authentication) {

        if (authentication == null || authentication.getName() == null) {
            throw new ResourceNotFoundException("Unauthenticated user session.");
        }

        User user = userRepository.findByEmail(authentication.getName())
                .or(() -> userRepository.findByEmailIgnoreCase(authentication.getName()))
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found."));

        boolean onboardingCompleted = studentProfileRepository.findByUserId(user.getId())
                .map(profile -> Boolean.TRUE.equals(profile.isOnboardingCompleted()))
                .orElse(false);

        Map<String, Object> sessionData = new LinkedHashMap<>();
        sessionData.put("message", "Authenticated session is working.");
        sessionData.put("email", user.getEmail());
        sessionData.put("userId", user.getId());
        sessionData.put("role", user.getRole().name());
        sessionData.put("collegeId", user.getCollege().getId());
        sessionData.put("collegeName", user.getCollege().getName());
        sessionData.put("onboardingCompleted", onboardingCompleted);

        return ResponseEntity.ok(sessionData);
    }
}