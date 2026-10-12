package com.vynk.goal.controller;

import com.vynk.common.exception.ResourceNotFoundException;
import com.vynk.goal.dto.NetworkingGoalRequest;
import com.vynk.goal.dto.NetworkingGoalResponse;
import com.vynk.goal.service.NetworkingGoalService;
import com.vynk.user.entity.User;
import com.vynk.user.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/goals")
public class NetworkingGoalController {

    private final NetworkingGoalService goalService;
    private final UserRepository userRepository;

    public NetworkingGoalController(NetworkingGoalService goalService, UserRepository userRepository) {
        this.goalService = goalService;
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<List<NetworkingGoalResponse>> getMyGoals(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(goalService.getGoalsForUser(user.getId()));
    }

    @PostMapping
    public ResponseEntity<NetworkingGoalResponse> createGoal(
            @Valid @RequestBody NetworkingGoalRequest request,
            Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        NetworkingGoalResponse response = goalService.createGoal(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<NetworkingGoalResponse> updateGoal(
            @PathVariable("id") Long id,
            @Valid @RequestBody NetworkingGoalRequest request,
            Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        NetworkingGoalResponse response = goalService.updateGoal(user, id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deactivateGoal(
            @PathVariable("id") Long id,
            Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        goalService.deactivateGoal(user, id);
        return ResponseEntity.ok(Map.of("message", "Networking goal deactivated successfully."));
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
