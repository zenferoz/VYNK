package com.vynk.connection.service;

import com.vynk.common.exception.ResourceNotFoundException;
import com.vynk.connection.dto.ConnectionResponse;
import com.vynk.connection.dto.CreateConnectionRequest;
import com.vynk.connection.dto.PendingConnectionsResponse;
import com.vynk.connection.entity.ConnectionRequest;
import com.vynk.connection.entity.ConnectionStatus;
import com.vynk.connection.repository.ConnectionRequestRepository;
import com.vynk.profile.entity.StudentProfile;
import com.vynk.profile.repository.StudentProfileRepository;
import com.vynk.safety.service.SafetyService;
import com.vynk.user.entity.User;
import com.vynk.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ConnectionService {

    private static final int MAX_PENDING_OUTGOING = 15;
    private static final int COOLDOWN_HOURS = 24;

    private final ConnectionRequestRepository connectionRequestRepository;
    private final UserRepository userRepository;
    private final StudentProfileRepository profileRepository;
    private final SafetyService safetyService;
    private final com.vynk.notification.service.NotificationService notificationService;

    public ConnectionService(
            ConnectionRequestRepository connectionRequestRepository,
            UserRepository userRepository,
            StudentProfileRepository profileRepository,
            SafetyService safetyService
    ) {
        this(connectionRequestRepository, userRepository, profileRepository, safetyService, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public ConnectionService(
            ConnectionRequestRepository connectionRequestRepository,
            UserRepository userRepository,
            StudentProfileRepository profileRepository,
            SafetyService safetyService,
            com.vynk.notification.service.NotificationService notificationService
    ) {
        this.connectionRequestRepository = connectionRequestRepository;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.safetyService = safetyService;
        this.notificationService = notificationService;
    }

    @Transactional
    public ConnectionResponse sendConnectionRequest(User sender, CreateConnectionRequest request) {
        if (sender == null) {
            throw new IllegalArgumentException("User must be authenticated");
        }
        if (request.getReceiverId() == null) {
            throw new IllegalArgumentException("Receiver ID is required");
        }
        if (sender.getId().equals(request.getReceiverId())) {
            throw new IllegalArgumentException("Cannot send a connection request to yourself");
        }

        // Limit pending outgoing requests
        long pendingOutgoingCount = connectionRequestRepository.countBySenderIdAndStatus(sender.getId(), ConnectionStatus.PENDING);
        if (pendingOutgoingCount >= MAX_PENDING_OUTGOING) {
            throw new IllegalArgumentException("Maximum limit of " + MAX_PENDING_OUTGOING + " pending outgoing connection requests reached. Please wait for responses or cancel pending requests.");
        }

        User receiver = userRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        // College isolation check
        if (sender.getCollege() == null || receiver.getCollege() == null ||
                !sender.getCollege().getId().equals(receiver.getCollege().getId())) {
            throw new IllegalArgumentException("Cannot connect with students from another college");
        }

        // Verification & active checks
        if (sender.getStatus() != com.vynk.user.entity.AccountStatus.ACTIVE || !sender.isEmailVerified() ||
                receiver.getStatus() != com.vynk.user.entity.AccountStatus.ACTIVE || !receiver.isEmailVerified()) {
            throw new IllegalArgumentException("Both accounts must be active and verified to connect");
        }

        // Discoverability check on receiver
        StudentProfile receiverProfile = profileRepository.findByUserId(receiver.getId()).orElse(null);
        if (receiverProfile == null || !receiverProfile.isOnboardingCompleted() || !receiverProfile.isDiscoverable()) {
            throw new IllegalArgumentException("This student is not currently discoverable");
        }

        // Blocking check in both directions
        if (safetyService.isBlockedEitherWay(sender.getId(), receiver.getId())) {
            throw new IllegalArgumentException("Unable to send connection request to this student");
        }

        // Check if already connected (ACCEPTED)
        if (connectionRequestRepository.existsAcceptedBetween(sender.getId(), receiver.getId())) {
            throw new IllegalArgumentException("You are already connected with this student");
        }

        // Check if pending request exists in either direction
        if (connectionRequestRepository.existsPendingBetween(sender.getId(), receiver.getId())) {
            throw new IllegalArgumentException("A pending connection request already exists between you and this student");
        }

        // Check cooldown if rejected within last 24h
        List<ConnectionRequest> recentRejected = connectionRequestRepository.findRecentRejectedBetween(
                sender.getId(), receiver.getId(), LocalDateTime.now().minusHours(COOLDOWN_HOURS));
        if (!recentRejected.isEmpty()) {
            throw new IllegalArgumentException("Connection request was recently rejected. Please wait " + COOLDOWN_HOURS + " hours before retrying.");
        }

        String note = request.getPurposeNote() != null ? request.getPurposeNote().trim() : null;
        if (note != null && note.length() > 300) {
            note = note.substring(0, 300);
        }

        ConnectionRequest connectionRequest = new ConnectionRequest(sender, receiver, note);
        ConnectionRequest saved = connectionRequestRepository.save(connectionRequest);

        if (notificationService != null) {
            StudentProfile senderProfile = profileRepository.findByUserId(sender.getId()).orElse(null);
            String name = senderProfile != null ? senderProfile.getFullName() : "A student";
            notificationService.createNotification(
                    receiver,
                    sender,
                    com.vynk.notification.entity.NotificationType.CONNECTION_REQUEST,
                    "New Connection Request",
                    name + " sent you a connection request.",
                    "/connections?tab=pending"
            );
        }

        return mapToResponse(saved, sender.getId(), "OUTGOING");
    }

    @Transactional
    public ConnectionResponse acceptConnection(User currentUser, Long requestId) {
        ConnectionRequest cr = connectionRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Connection request not found"));

        if (!cr.getReceiver().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("You are not authorized to accept this connection request");
        }

        if (cr.getStatus() != ConnectionStatus.PENDING) {
            throw new IllegalStateException("Connection request is not pending (status: " + cr.getStatus() + ")");
        }

        if (safetyService.isBlockedEitherWay(cr.getSender().getId(), cr.getReceiver().getId())) {
            throw new IllegalArgumentException("Cannot accept request due to safety restrictions");
        }

        cr.setStatus(ConnectionStatus.ACCEPTED);
        ConnectionRequest saved = connectionRequestRepository.save(cr);

        if (notificationService != null) {
            StudentProfile accepterProfile = profileRepository.findByUserId(currentUser.getId()).orElse(null);
            String name = accepterProfile != null ? accepterProfile.getFullName() : "A student";
            notificationService.createNotification(
                    cr.getSender(),
                    currentUser,
                    com.vynk.notification.entity.NotificationType.CONNECTION_ACCEPTED,
                    "Connection Accepted",
                    name + " accepted your connection request.",
                    "/connections"
            );
        }

        return mapToResponse(saved, currentUser.getId(), "INCOMING");
    }

    @Transactional
    public ConnectionResponse rejectConnection(User currentUser, Long requestId) {
        ConnectionRequest cr = connectionRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Connection request not found"));

        if (!cr.getReceiver().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("You are not authorized to reject this connection request");
        }

        if (cr.getStatus() != ConnectionStatus.PENDING) {
            throw new IllegalStateException("Connection request is not pending (status: " + cr.getStatus() + ")");
        }

        cr.setStatus(ConnectionStatus.REJECTED);
        ConnectionRequest saved = connectionRequestRepository.save(cr);
        return mapToResponse(saved, currentUser.getId(), "INCOMING");
    }

    @Transactional
    public ConnectionResponse cancelConnection(User currentUser, Long requestId) {
        ConnectionRequest cr = connectionRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Connection request not found"));

        if (!cr.getSender().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("You are not authorized to cancel this connection request");
        }

        if (cr.getStatus() != ConnectionStatus.PENDING) {
            throw new IllegalStateException("Connection request is not pending (status: " + cr.getStatus() + ")");
        }

        cr.setStatus(ConnectionStatus.CANCELLED);
        ConnectionRequest saved = connectionRequestRepository.save(cr);
        return mapToResponse(saved, currentUser.getId(), "OUTGOING");
    }

    @Transactional(readOnly = true)
    public PendingConnectionsResponse getPendingConnections(User currentUser) {
        List<ConnectionRequest> incoming = connectionRequestRepository.findPendingIncoming(currentUser.getId());
        List<ConnectionRequest> outgoing = connectionRequestRepository.findPendingOutgoing(currentUser.getId());

        List<ConnectionResponse> incomingResponses = incoming.stream()
                .filter(cr -> !safetyService.isBlockedEitherWay(currentUser.getId(), cr.getSender().getId()))
                .map(cr -> mapToResponse(cr, currentUser.getId(), "INCOMING"))
                .collect(Collectors.toList());

        List<ConnectionResponse> outgoingResponses = outgoing.stream()
                .filter(cr -> !safetyService.isBlockedEitherWay(currentUser.getId(), cr.getReceiver().getId()))
                .map(cr -> mapToResponse(cr, currentUser.getId(), "OUTGOING"))
                .collect(Collectors.toList());

        return new PendingConnectionsResponse(incomingResponses, outgoingResponses);
    }

    @Transactional(readOnly = true)
    public List<ConnectionResponse> getAcceptedConnections(User currentUser) {
        List<ConnectionRequest> accepted = connectionRequestRepository.findAllAcceptedForUser(currentUser.getId());

        return accepted.stream()
                .filter(cr -> {
                    Long otherUserId = cr.getSender().getId().equals(currentUser.getId())
                            ? cr.getReceiver().getId()
                            : cr.getSender().getId();
                    return !safetyService.isBlockedEitherWay(currentUser.getId(), otherUserId);
                })
                .map(cr -> {
                    String direction = cr.getSender().getId().equals(currentUser.getId()) ? "OUTGOING" : "INCOMING";
                    return mapToResponse(cr, currentUser.getId(), direction);
                })
                .collect(Collectors.toList());
    }

    private ConnectionResponse mapToResponse(ConnectionRequest cr, Long currentUserId, String direction) {
        User otherUser = cr.getSender().getId().equals(currentUserId) ? cr.getReceiver() : cr.getSender();
        StudentProfile otherProfile = profileRepository.findByUserId(otherUser.getId()).orElse(null);

        String fullName = otherProfile != null ? otherProfile.getFullName() : otherUser.getEmail();
        String department = otherProfile != null ? otherProfile.getDepartment() : null;
        Integer currentYear = otherProfile != null ? otherProfile.getCurrentYear() : null;

        return new ConnectionResponse(
                cr.getId(),
                otherUser.getId(),
                fullName,
                department,
                currentYear,
                cr.getStatus(),
                cr.getPurposeNote(),
                cr.getCreatedAt(),
                cr.getUpdatedAt(),
                direction
        );
    }
}
