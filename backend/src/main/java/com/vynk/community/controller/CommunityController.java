package com.vynk.community.controller;

import com.vynk.common.exception.ResourceNotFoundException;
import com.vynk.community.dto.*;
import com.vynk.community.service.CommunityService;
import com.vynk.user.entity.User;
import com.vynk.user.repository.UserRepository;
import com.vynk.security.ratelimit.RateLimitingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/communities")
public class CommunityController {

    private final CommunityService communityService;
    private final UserRepository userRepository;
    private final RateLimitingService rateLimitingService;

    public CommunityController(CommunityService communityService, UserRepository userRepository) {
        this(communityService, userRepository, null);
    }

    @Autowired
    public CommunityController(
            CommunityService communityService,
            UserRepository userRepository,
            @Autowired(required = false) RateLimitingService rateLimitingService) {
        this.communityService = communityService;
        this.userRepository = userRepository;
        this.rateLimitingService = rateLimitingService;
    }

    @GetMapping
    public ResponseEntity<Page<CommunityResponse>> getCommunities(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(communityService.getCommunities(user, page, size));
    }

    @PostMapping
    public ResponseEntity<CommunityResponse> createCommunity(
            @Valid @RequestBody CreateCommunityRequest request,
            Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        if (rateLimitingService != null) {
            rateLimitingService.checkLimit("community_create", user.getId().toString(), 10, 86400);
        }
        CommunityResponse response = communityService.createCommunity(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommunityResponse> getCommunityDetail(
            @PathVariable("id") Long id,
            Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(communityService.getCommunityDetail(user, id));
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<Map<String, String>> joinCommunity(
            @PathVariable("id") Long id,
            Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        communityService.joinCommunity(user, id);
        return ResponseEntity.ok(Map.of("message", "Joined community successfully."));
    }

    @PostMapping("/{id}/leave")
    public ResponseEntity<Map<String, String>> leaveCommunity(
            @PathVariable("id") Long id,
            Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        communityService.leaveCommunity(user, id);
        return ResponseEntity.ok(Map.of("message", "Left community successfully."));
    }

    @GetMapping("/{id}/discussions")
    public ResponseEntity<Page<DiscussionResponse>> getDiscussions(
            @PathVariable("id") Long id,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "30") int size,
            Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(communityService.getDiscussions(user, id, page, size));
    }

    @PostMapping("/{id}/discussions")
    public ResponseEntity<DiscussionResponse> createDiscussion(
            @PathVariable("id") Long id,
            @Valid @RequestBody CreateDiscussionRequest request,
            Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        if (rateLimitingService != null) {
            rateLimitingService.checkLimit("discussion_create", user.getId().toString(), 30, 3600);
        }
        DiscussionResponse response = communityService.createDiscussion(user, id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{id}/discussions/{discussionId}")
    public ResponseEntity<Map<String, String>> deleteDiscussion(
            @PathVariable("id") Long id,
            @PathVariable("discussionId") Long discussionId,
            Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        communityService.deleteDiscussion(user, id, discussionId);
        return ResponseEntity.ok(Map.of("message", "Discussion deleted successfully."));
    }

    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResourceNotFoundException("Authenticated user not found.");
        }
        return userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found for active session."));
    }
}
