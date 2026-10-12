package com.vynk.connection.controller;

import com.vynk.common.exception.ResourceNotFoundException;
import com.vynk.connection.dto.ConnectionResponse;
import com.vynk.connection.dto.CreateConnectionRequest;
import com.vynk.connection.dto.PendingConnectionsResponse;
import com.vynk.connection.service.ConnectionService;
import com.vynk.user.entity.User;
import com.vynk.user.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/connections")
public class ConnectionController {

    private final ConnectionService connectionService;
    private final UserRepository userRepository;
    private final com.vynk.security.ratelimit.RateLimitingService rateLimitingService;

    public ConnectionController(ConnectionService connectionService, UserRepository userRepository) {
        this(connectionService, userRepository, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public ConnectionController(
            ConnectionService connectionService,
            UserRepository userRepository,
            com.vynk.security.ratelimit.RateLimitingService rateLimitingService) {
        this.connectionService = connectionService;
        this.userRepository = userRepository;
        this.rateLimitingService = rateLimitingService;
    }

    @PostMapping("/request")
    public ResponseEntity<ConnectionResponse> sendRequest(
            @Valid @RequestBody CreateConnectionRequest request,
            Authentication authentication) {
        User currentUser = getAuthenticatedUser(authentication);
        if (rateLimitingService != null) {
            rateLimitingService.checkLimit("connection", currentUser.getId().toString(), 20, 60);
        }
        ConnectionResponse response = connectionService.sendConnectionRequest(currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/pending")
    public ResponseEntity<PendingConnectionsResponse> getPending(Authentication authentication) {
        User currentUser = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(connectionService.getPendingConnections(currentUser));
    }

    @GetMapping
    public ResponseEntity<List<ConnectionResponse>> getAccepted(Authentication authentication) {
        User currentUser = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(connectionService.getAcceptedConnections(currentUser));
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<ConnectionResponse> accept(
            @PathVariable("id") Long id,
            Authentication authentication) {
        User currentUser = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(connectionService.acceptConnection(currentUser, id));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ConnectionResponse> reject(
            @PathVariable("id") Long id,
            Authentication authentication) {
        User currentUser = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(connectionService.rejectConnection(currentUser, id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ConnectionResponse> cancel(
            @PathVariable("id") Long id,
            Authentication authentication) {
        User currentUser = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(connectionService.cancelConnection(currentUser, id));
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
