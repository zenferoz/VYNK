package com.vynk.safety.controller;

import com.vynk.common.exception.ResourceNotFoundException;
import com.vynk.safety.dto.BlockedUserResponse;
import com.vynk.safety.dto.CreateReportRequest;
import com.vynk.safety.service.SafetyService;
import com.vynk.user.entity.User;
import com.vynk.user.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class SafetyController {

    private final SafetyService safetyService;
    private final UserRepository userRepository;
    private final com.vynk.security.ratelimit.RateLimitingService rateLimitingService;

    public SafetyController(SafetyService safetyService, UserRepository userRepository) {
        this(safetyService, userRepository, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public SafetyController(
            SafetyService safetyService,
            UserRepository userRepository,
            com.vynk.security.ratelimit.RateLimitingService rateLimitingService) {
        this.safetyService = safetyService;
        this.userRepository = userRepository;
        this.rateLimitingService = rateLimitingService;
    }

    @PostMapping("/{userId}/block")
    public ResponseEntity<Map<String, String>> blockUser(
            @PathVariable("userId") Long targetUserId,
            Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        safetyService.blockUser(user, targetUserId);
        return ResponseEntity.ok(Map.of("message", "User blocked successfully."));
    }

    @DeleteMapping("/{userId}/block")
    public ResponseEntity<Map<String, String>> unblockUser(
            @PathVariable("userId") Long targetUserId,
            Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        safetyService.unblockUser(user, targetUserId);
        return ResponseEntity.ok(Map.of("message", "User unblocked successfully."));
    }

    @GetMapping("/blocked")
    public ResponseEntity<List<BlockedUserResponse>> getBlockedUsers(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        List<BlockedUserResponse> responses = safetyService.getBlockedUsersBy(user.getId()).stream()
                .map(BlockedUserResponse::new)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/{userId}/report")
    public ResponseEntity<Map<String, String>> reportUser(
            @PathVariable("userId") Long targetUserId,
            @Valid @RequestBody CreateReportRequest request,
            Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        if (rateLimitingService != null) {
            rateLimitingService.checkLimit("report", user.getId().toString(), 10, 60);
        }
        safetyService.reportUser(user, targetUserId, request.getReason(), request.getDescription());
        return ResponseEntity.ok(Map.of("message", "Report submitted successfully."));
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
