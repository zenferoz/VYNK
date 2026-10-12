package com.vynk.profile.controller;

import com.vynk.common.exception.ResourceNotFoundException;
import com.vynk.profile.dto.StudentDiscoveryResponse;
import com.vynk.profile.service.StudentProfileService;
import com.vynk.user.entity.User;
import com.vynk.user.repository.UserRepository;
import com.vynk.matching.service.PurposeMatchingService;
import com.vynk.security.ratelimit.RateLimitingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/students")
public class StudentDiscoveryController {

    private final StudentProfileService studentProfileService;
    private final PurposeMatchingService matchingService;
    private final UserRepository userRepository;
    private final RateLimitingService rateLimitingService;

    public StudentDiscoveryController(
            StudentProfileService studentProfileService,
            PurposeMatchingService matchingService,
            UserRepository userRepository) {
        this(studentProfileService, matchingService, userRepository, null);
    }

    @Autowired
    public StudentDiscoveryController(
            StudentProfileService studentProfileService,
            PurposeMatchingService matchingService,
            UserRepository userRepository,
            @Autowired(required = false) RateLimitingService rateLimitingService) {
        this.studentProfileService = studentProfileService;
        this.matchingService = matchingService;
        this.userRepository = userRepository;
        this.rateLimitingService = rateLimitingService;
    }

    @GetMapping
    public ResponseEntity<List<StudentDiscoveryResponse>> getDiscoverableStudents(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        List<StudentDiscoveryResponse> students = studentProfileService
                .getDiscoverableStudentsInCollege(user.getCollege().getId(), user.getId());

        return ResponseEntity.ok(students);
    }

    @GetMapping("/matches")
    public ResponseEntity<List<com.vynk.matching.dto.StudentMatchResponse>> getTopMatches(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        if (rateLimitingService != null) {
            rateLimitingService.checkLimit("matching", user.getId().toString(), 30, 60);
        }
        return ResponseEntity.ok(matchingService.getTopMatches(user));
    }

    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResourceNotFoundException("Unauthenticated user session.");
        }
        return userRepository.findByEmail(authentication.getName())
                .or(() -> userRepository.findByEmailIgnoreCase(authentication.getName()))
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found."));
    }
}
