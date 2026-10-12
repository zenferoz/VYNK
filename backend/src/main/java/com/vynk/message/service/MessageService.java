package com.vynk.message.service;

import com.vynk.common.exception.ResourceNotFoundException;
import com.vynk.connection.repository.ConnectionRequestRepository;
import com.vynk.message.dto.ConversationSummaryResponse;
import com.vynk.message.dto.MessageResponse;
import com.vynk.message.dto.SendMessageRequest;
import com.vynk.message.entity.Message;
import com.vynk.message.repository.MessageRepository;
import com.vynk.notification.entity.NotificationType;
import com.vynk.notification.service.NotificationService;
import com.vynk.profile.entity.StudentProfile;
import com.vynk.profile.repository.StudentProfileRepository;
import com.vynk.safety.service.SafetyService;
import com.vynk.user.entity.User;
import com.vynk.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConnectionRequestRepository connectionRequestRepository;
    private final UserRepository userRepository;
    private final StudentProfileRepository profileRepository;
    private final SafetyService safetyService;
    private final NotificationService notificationService;

    public MessageService(
            MessageRepository messageRepository,
            ConnectionRequestRepository connectionRequestRepository,
            UserRepository userRepository,
            StudentProfileRepository profileRepository,
            SafetyService safetyService,
            NotificationService notificationService) {
        this.messageRepository = messageRepository;
        this.connectionRequestRepository = connectionRequestRepository;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.safetyService = safetyService;
        this.notificationService = notificationService;
    }

    @Transactional
    public MessageResponse sendMessage(User currentUser, Long peerId, SendMessageRequest request) {
        if (currentUser.getId().equals(peerId)) {
            throw new IllegalArgumentException("Cannot message yourself.");
        }

        User recipient = userRepository.findById(peerId)
                .orElseThrow(() -> new ResourceNotFoundException("Recipient user not found."));

        if (safetyService.isBlockedEitherWay(currentUser.getId(), peerId)) {
            throw new IllegalArgumentException("Cannot message this user.");
        }

        if (!connectionRequestRepository.existsAcceptedBetween(currentUser.getId(), peerId)) {
            throw new IllegalArgumentException("Messaging is only available between accepted connections.");
        }

        String content = request.getContent() != null ? request.getContent().trim() : "";
        if (content.isEmpty()) {
            throw new IllegalArgumentException("Message content cannot be blank.");
        }
        if (content.length() > 2000) {
            throw new IllegalArgumentException("Message exceeds maximum length of 2000 characters.");
        }

        Message message = new Message(currentUser, recipient, content);
        Message saved = messageRepository.save(message);

        // Notify recipient
        StudentProfile senderProfile = profileRepository.findByUserId(currentUser.getId()).orElse(null);
        String senderName = senderProfile != null ? senderProfile.getFullName() : "A connected student";
        String snippet = content.length() > 50 ? content.substring(0, 47) + "..." : content;
        notificationService.createNotification(
                recipient,
                currentUser,
                NotificationType.NEW_MESSAGE,
                "New message from " + senderName,
                snippet,
                "/messages?peer=" + currentUser.getId()
        );

        return new MessageResponse(saved, currentUser.getId());
    }

    @Transactional(readOnly = true)
    public Page<MessageResponse> getConversationHistory(User currentUser, Long peerId, int page, int size) {
        if (currentUser.getId().equals(peerId)) {
            throw new IllegalArgumentException("Cannot open conversation with yourself.");
        }

        if (safetyService.isBlockedEitherWay(currentUser.getId(), peerId)) {
            throw new IllegalArgumentException("Cannot access messages with this user.");
        }

        if (!connectionRequestRepository.existsAcceptedBetween(currentUser.getId(), peerId)) {
            throw new IllegalArgumentException("Messaging is only available between accepted connections.");
        }

        int boundedPage = Math.max(0, page);
        int boundedSize = Math.min(100, Math.max(1, size));

        return messageRepository.findConversationHistory(
                currentUser.getId(),
                peerId,
                PageRequest.of(boundedPage, boundedSize)
        ).map(m -> new MessageResponse(m, currentUser.getId()));
    }

    @Transactional(readOnly = true)
    public List<ConversationSummaryResponse> getConversations(User currentUser) {
        Long myId = currentUser.getId();

        // 1. Get all accepted connection partners
        var acceptedConnections = connectionRequestRepository.findAllAcceptedForUser(myId);
        Set<Long> connectedPeerIds = new HashSet<>();
        for (var conn : acceptedConnections) {
            Long peerId = conn.getSender().getId().equals(myId)
                    ? conn.getReceiver().getId()
                    : conn.getSender().getId();
            connectedPeerIds.add(peerId);
        }

        // Filter out any blocked peers
        Set<Long> blockedIds = safetyService.getAllBlockedUserIdsFor(myId);
        connectedPeerIds.removeAll(blockedIds);

        if (connectedPeerIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. Batch fetch profiles
        Map<Long, StudentProfile> profileMap = new HashMap<>();
        List<StudentProfile> profiles = profileRepository.findByUserIdIn(connectedPeerIds);
        for (StudentProfile p : profiles) {
            profileMap.put(p.getUser().getId(), p);
        }

        // 3. Batch fetch unread counts grouped by sender
        Map<Long, Long> unreadMap = new HashMap<>();
        List<Object[]> unreadCounts = messageRepository.countUnreadGroupedBySender(myId);
        for (Object[] row : unreadCounts) {
            Long senderId = (Long) row[0];
            Long count = (Long) row[1];
            unreadMap.put(senderId, count);
        }

        // 4. Batch fetch latest message for each peer
        Map<Long, Message> latestMessageMap = new HashMap<>();
        List<Message> latestMessages = messageRepository.findLatestMessagesForPeers(myId, connectedPeerIds);
        for (Message m : latestMessages) {
            Long peerId = m.getSender().getId().equals(myId)
                    ? m.getRecipient().getId()
                    : m.getSender().getId();
            latestMessageMap.put(peerId, m);
        }

        List<ConversationSummaryResponse> summaries = new ArrayList<>();

        for (Long peerId : connectedPeerIds) {
            StudentProfile profile = profileMap.get(peerId);
            String name = profile != null ? profile.getFullName() : "Student #" + peerId;
            String dept = profile != null ? profile.getDepartment() : null;

            Message latest = latestMessageMap.get(peerId);
            long unread = unreadMap.getOrDefault(peerId, 0L);

            summaries.add(new ConversationSummaryResponse(
                    peerId,
                    name,
                    dept,
                    latest != null ? latest.getContent() : null,
                    latest != null ? latest.getCreatedAt() : null,
                    unread
            ));
        }

        // Sort by lastMessageAt DESC (nulls last)
        summaries.sort((a, b) -> {
            if (a.getLastMessageAt() == null && b.getLastMessageAt() == null) {
                return a.getPeerName().compareToIgnoreCase(b.getPeerName());
            }
            if (a.getLastMessageAt() == null) return 1;
            if (b.getLastMessageAt() == null) return -1;
            return b.getLastMessageAt().compareTo(a.getLastMessageAt());
        });

        return summaries;
    }

    @Transactional
    public void markConversationRead(User currentUser, Long peerId) {
        messageRepository.markConversationAsRead(currentUser.getId(), peerId, LocalDateTime.now());
    }
}
