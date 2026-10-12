package com.vynk.message.controller;

import com.vynk.common.exception.ResourceNotFoundException;
import com.vynk.message.dto.ConversationSummaryResponse;
import com.vynk.message.dto.MessageResponse;
import com.vynk.message.dto.SendMessageRequest;
import com.vynk.message.service.MessageService;
import com.vynk.security.ratelimit.RateLimitingService;
import com.vynk.user.entity.User;
import com.vynk.user.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;
    private final UserRepository userRepository;
    private final RateLimitingService rateLimitingService;

    public MessageController(
            MessageService messageService,
            UserRepository userRepository,
            RateLimitingService rateLimitingService) {
        this.messageService = messageService;
        this.userRepository = userRepository;
        this.rateLimitingService = rateLimitingService;
    }

    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationSummaryResponse>> getConversations(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(messageService.getConversations(user));
    }

    @GetMapping("/{peerId}")
    public ResponseEntity<Page<MessageResponse>> getConversationHistory(
            @PathVariable("peerId") Long peerId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "50") int size,
            Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(messageService.getConversationHistory(user, peerId, page, size));
    }

    @PostMapping("/{peerId}")
    public ResponseEntity<MessageResponse> sendMessage(
            @PathVariable("peerId") Long peerId,
            @Valid @RequestBody SendMessageRequest request,
            Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        if (rateLimitingService != null) {
            rateLimitingService.checkLimit("messaging", user.getId().toString(), 60, 60);
        }
        MessageResponse response = messageService.sendMessage(user, peerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{peerId}/read")
    public ResponseEntity<Map<String, String>> markRead(
            @PathVariable("peerId") Long peerId,
            Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        messageService.markConversationRead(user, peerId);
        return ResponseEntity.ok(Map.of("message", "Conversation marked as read."));
    }

    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResourceNotFoundException("Authenticated user not found.");
        }
        return userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found for active session."));
    }
}
