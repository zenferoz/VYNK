package com.vynk;

import com.vynk.connection.repository.ConnectionRequestRepository;
import com.vynk.message.dto.SendMessageRequest;
import com.vynk.message.entity.Message;
import com.vynk.message.repository.MessageRepository;
import com.vynk.message.service.MessageService;
import com.vynk.notification.service.NotificationService;
import com.vynk.profile.repository.StudentProfileRepository;
import com.vynk.safety.service.SafetyService;
import com.vynk.user.entity.User;
import com.vynk.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MessageServiceTest {

    private MessageRepository messageRepository;
    private ConnectionRequestRepository connectionRequestRepository;
    private UserRepository userRepository;
    private StudentProfileRepository profileRepository;
    private SafetyService safetyService;
    private NotificationService notificationService;
    private MessageService messageService;

    private User user1;
    private User user2;

    @BeforeEach
    void setUp() throws Exception {
        messageRepository = mock(MessageRepository.class);
        connectionRequestRepository = mock(ConnectionRequestRepository.class);
        userRepository = mock(UserRepository.class);
        profileRepository = mock(StudentProfileRepository.class);
        safetyService = mock(SafetyService.class);
        notificationService = mock(NotificationService.class);

        messageService = new MessageService(
                messageRepository,
                connectionRequestRepository,
                userRepository,
                profileRepository,
                safetyService,
                notificationService
        );

        user1 = new User();
        setField(user1, "id", 1L);
        user1.setEmail("user1@college.edu");

        user2 = new User();
        setField(user2, "id", 2L);
        user2.setEmail("user2@college.edu");

        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void sendsMessageWhenConnectedAndNotBlocked() throws Exception {
        when(safetyService.isBlockedEitherWay(1L, 2L)).thenReturn(false);
        when(connectionRequestRepository.existsAcceptedBetween(1L, 2L)).thenReturn(true);
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
            Message m = invocation.getArgument(0);
            setField(m, "id", 100L);
            return m;
        });

        SendMessageRequest req = new SendMessageRequest("Hey there, want to work on project?");
        var response = messageService.sendMessage(user1, 2L, req);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals("Hey there, want to work on project?", response.getContent());
        assertTrue(response.isMine());
        verify(notificationService, times(1)).createNotification(eq(user2), eq(user1), any(), any(), any(), any());
    }

    @Test
    void rejectsMessagingSelf() {
        SendMessageRequest req = new SendMessageRequest("Hello self");
        assertThrows(IllegalArgumentException.class, () ->
                messageService.sendMessage(user1, 1L, req)
        );
    }

    @Test
    void rejectsMessagingWhenNotAcceptedConnection() {
        when(safetyService.isBlockedEitherWay(1L, 2L)).thenReturn(false);
        when(connectionRequestRepository.existsAcceptedBetween(1L, 2L)).thenReturn(false);

        SendMessageRequest req = new SendMessageRequest("Hello");
        assertThrows(IllegalArgumentException.class, () ->
                messageService.sendMessage(user1, 2L, req)
        );
    }

    @Test
    void rejectsMessagingWhenBlockedEitherWay() {
        when(safetyService.isBlockedEitherWay(1L, 2L)).thenReturn(true);

        SendMessageRequest req = new SendMessageRequest("Hello");
        assertThrows(IllegalArgumentException.class, () ->
                messageService.sendMessage(user1, 2L, req)
        );
    }

    @Test
    void marksConversationAsRead() {
        messageService.markConversationRead(user1, 2L);
        verify(messageRepository, times(1)).markConversationAsRead(eq(1L), eq(2L), any());
    }

    @Test
    void getConversationsUsesBatchLookupsAndReturnsSummaries() throws Exception {
        com.vynk.connection.entity.ConnectionRequest conn = new com.vynk.connection.entity.ConnectionRequest();
        setField(conn, "sender", user1);
        setField(conn, "receiver", user2);

        when(connectionRequestRepository.findAllAcceptedForUser(1L)).thenReturn(java.util.List.of(conn));
        when(safetyService.getAllBlockedUserIdsFor(1L)).thenReturn(java.util.Collections.emptySet());

        com.vynk.profile.entity.StudentProfile p2 = new com.vynk.profile.entity.StudentProfile();
        setField(p2, "user", user2);
        p2.setFullName("Student Two");
        p2.setDepartment("CSE");
        when(profileRepository.findByUserIdIn(anyCollection())).thenReturn(java.util.List.of(p2));

        when(messageRepository.countUnreadGroupedBySender(1L)).thenReturn(java.util.Collections.singletonList(new Object[]{2L, 3L}));

        Message latest = new Message(user2, user1, "Latest message");
        when(messageRepository.findLatestMessagesForPeers(eq(1L), anyCollection())).thenReturn(java.util.List.of(latest));

        var list = messageService.getConversations(user1);
        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("Student Two", list.get(0).getPeerName());
        assertEquals(3L, list.get(0).getUnreadCount());
        assertEquals("Latest message", list.get(0).getLastMessage());

        verify(profileRepository, times(1)).findByUserIdIn(anyCollection());
        verify(messageRepository, times(1)).countUnreadGroupedBySender(1L);
        verify(messageRepository, times(1)).findLatestMessagesForPeers(eq(1L), anyCollection());
        verify(messageRepository, never()).findLatestMessageBetween(anyLong(), anyLong());
    }

    @Test
    void getConversationsHandlesMultiplePeersBothDirectionsAndEmptyConversations() throws Exception {
        User user3 = new User();
        setField(user3, "id", 3L);
        user3.setEmail("user3@college.edu");

        User user4 = new User();
        setField(user4, "id", 4L);
        user4.setEmail("user4@college.edu");

        // Connections: user1 <-> user2, user3 <-> user1, user1 <-> user4
        com.vynk.connection.entity.ConnectionRequest conn2 = new com.vynk.connection.entity.ConnectionRequest();
        setField(conn2, "sender", user1);
        setField(conn2, "receiver", user2);

        com.vynk.connection.entity.ConnectionRequest conn3 = new com.vynk.connection.entity.ConnectionRequest();
        setField(conn3, "sender", user3);
        setField(conn3, "receiver", user1);

        com.vynk.connection.entity.ConnectionRequest conn4 = new com.vynk.connection.entity.ConnectionRequest();
        setField(conn4, "sender", user1);
        setField(conn4, "receiver", user4);

        when(connectionRequestRepository.findAllAcceptedForUser(1L)).thenReturn(java.util.List.of(conn2, conn3, conn4));
        when(safetyService.getAllBlockedUserIdsFor(1L)).thenReturn(java.util.Collections.emptySet());

        // Profiles
        com.vynk.profile.entity.StudentProfile p2 = new com.vynk.profile.entity.StudentProfile();
        setField(p2, "user", user2);
        p2.setFullName("Alice Smith");
        p2.setDepartment("CSE");

        com.vynk.profile.entity.StudentProfile p3 = new com.vynk.profile.entity.StudentProfile();
        setField(p3, "user", user3);
        p3.setFullName("Bob Jones");
        p3.setDepartment("ECE");

        com.vynk.profile.entity.StudentProfile p4 = new com.vynk.profile.entity.StudentProfile();
        setField(p4, "user", user4);
        p4.setFullName("Charlie Brown");
        p4.setDepartment("ME");

        when(profileRepository.findByUserIdIn(anyCollection())).thenReturn(java.util.List.of(p2, p3, p4));

        // Unread counts: user2 has 5 unread, others have 0
        when(messageRepository.countUnreadGroupedBySender(1L))
                .thenReturn(java.util.Collections.singletonList(new Object[]{2L, 5L}));

        // Messages:
        // Peer 2 -> User 1 (peer sent to me) 10 minutes ago
        Message msgFromPeer2 = new Message(user2, user1, "Hey from peer 2");
        setField(msgFromPeer2, "createdAt", java.time.LocalDateTime.now().minusMinutes(10));

        // User 1 -> Peer 3 (me sent to peer) 5 minutes ago (newer)
        Message msgToPeer3 = new Message(user1, user3, "Project update sent");
        setField(msgToPeer3, "createdAt", java.time.LocalDateTime.now().minusMinutes(5));

        // Peer 4 has no messages (empty conversation)
        when(messageRepository.findLatestMessagesForPeers(eq(1L), anyCollection()))
                .thenReturn(java.util.List.of(msgFromPeer2, msgToPeer3));

        var summaries = messageService.getConversations(user1);
        assertNotNull(summaries);
        assertEquals(3, summaries.size());

        // Expected sort: Bob (T-5m) -> Alice (T-10m) -> Charlie (null message, nulls last)
        assertEquals(3L, summaries.get(0).getPeerId());
        assertEquals("Bob Jones", summaries.get(0).getPeerName());
        assertEquals("ECE", summaries.get(0).getPeerDepartment());
        assertEquals("Project update sent", summaries.get(0).getLastMessage());
        assertNotNull(summaries.get(0).getLastMessageAt());
        assertEquals(0L, summaries.get(0).getUnreadCount());

        assertEquals(2L, summaries.get(1).getPeerId());
        assertEquals("Alice Smith", summaries.get(1).getPeerName());
        assertEquals("CSE", summaries.get(1).getPeerDepartment());
        assertEquals("Hey from peer 2", summaries.get(1).getLastMessage());
        assertNotNull(summaries.get(1).getLastMessageAt());
        assertEquals(5L, summaries.get(1).getUnreadCount());

        assertEquals(4L, summaries.get(2).getPeerId());
        assertEquals("Charlie Brown", summaries.get(2).getPeerName());
        assertEquals("ME", summaries.get(2).getPeerDepartment());
        assertNull(summaries.get(2).getLastMessage());
        assertNull(summaries.get(2).getLastMessageAt());
        assertEquals(0L, summaries.get(2).getUnreadCount());

        // Batch query verification: only single batch queries, zero per-peer queries
        verify(messageRepository, times(1)).findLatestMessagesForPeers(eq(1L), anyCollection());
        verify(messageRepository, never()).findLatestMessageBetween(anyLong(), anyLong());
        verify(profileRepository, times(1)).findByUserIdIn(anyCollection());
        verify(messageRepository, times(1)).countUnreadGroupedBySender(1L);
    }

    @Test
    void getConversationsExcludesBlockedPeers() throws Exception {
        com.vynk.connection.entity.ConnectionRequest conn = new com.vynk.connection.entity.ConnectionRequest();
        setField(conn, "sender", user1);
        setField(conn, "receiver", user2);

        when(connectionRequestRepository.findAllAcceptedForUser(1L)).thenReturn(java.util.List.of(conn));
        when(safetyService.getAllBlockedUserIdsFor(1L)).thenReturn(java.util.Set.of(2L));

        var list = messageService.getConversations(user1);
        assertNotNull(list);
        assertTrue(list.isEmpty());

        verify(messageRepository, never()).findLatestMessagesForPeers(anyLong(), anyCollection());
        verify(messageRepository, never()).findLatestMessageBetween(anyLong(), anyLong());
        verify(profileRepository, never()).findByUserIdIn(anyCollection());
    }
}
