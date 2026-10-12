package com.vynk.post.controller;

import com.vynk.common.exception.ResourceNotFoundException;
import com.vynk.post.dto.CreatePostRequest;
import com.vynk.post.dto.PostResponse;
import com.vynk.post.service.PostService;
import com.vynk.security.ratelimit.RateLimitingService;
import com.vynk.user.entity.User;
import com.vynk.user.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;
    private final UserRepository userRepository;
    private final RateLimitingService rateLimitingService;

    public PostController(
            PostService postService,
            UserRepository userRepository,
            RateLimitingService rateLimitingService) {
        this.postService = postService;
        this.userRepository = userRepository;
        this.rateLimitingService = rateLimitingService;
    }

    @GetMapping
    public ResponseEntity<Page<PostResponse>> getFeed(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(postService.getFeed(user, page, size));
    }

    @PostMapping
    public ResponseEntity<PostResponse> createPost(
            @Valid @RequestBody CreatePostRequest request,
            Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        if (rateLimitingService != null) {
            rateLimitingService.checkLimit("posts", user.getId().toString(), 20, 60);
        }
        PostResponse response = postService.createPost(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PostResponse> updatePost(
            @PathVariable("id") Long id,
            @Valid @RequestBody CreatePostRequest request,
            Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        PostResponse response = postService.updatePost(user, id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deletePost(
            @PathVariable("id") Long id,
            Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        postService.deletePost(user, id);
        return ResponseEntity.ok(Map.of("message", "Post deleted successfully."));
    }

    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResourceNotFoundException("Authenticated user not found.");
        }
        return userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found for active session."));
    }
}
