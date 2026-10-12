package com.vynk;

import com.vynk.college.entity.College;
import com.vynk.connection.dto.CreateConnectionRequest;
import com.vynk.connection.entity.ConnectionRequest;
import com.vynk.connection.entity.ConnectionStatus;
import com.vynk.connection.repository.ConnectionRequestRepository;
import com.vynk.connection.service.ConnectionService;
import com.vynk.profile.entity.StudentProfile;
import com.vynk.profile.repository.StudentProfileRepository;
import com.vynk.safety.service.SafetyService;
import com.vynk.user.entity.AccountStatus;
import com.vynk.user.entity.User;
import com.vynk.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class ConnectionServiceTest {

    private ConnectionRequestRepository connectionRequestRepository;
    private UserRepository userRepository;
    private StudentProfileRepository profileRepository;
    private SafetyService safetyService;
    private ConnectionService connectionService;

    private College college;
    private User sender;
    private User receiver;
    private StudentProfile receiverProfile;

    @BeforeEach
    void setUp() throws Exception {
        connectionRequestRepository = Mockito.mock(ConnectionRequestRepository.class);
        userRepository = Mockito.mock(UserRepository.class);
        profileRepository = Mockito.mock(StudentProfileRepository.class);
        safetyService = Mockito.mock(SafetyService.class);

        connectionService = new ConnectionService(
                connectionRequestRepository,
                userRepository,
                profileRepository,
                safetyService
        );

        college = new College();
        setField(college, "id", 1L);

        sender = new User();
        setField(sender, "id", 10L);
        sender.setCollege(college);
        sender.setStatus(AccountStatus.ACTIVE);
        sender.setEmailVerified(true);

        receiver = new User();
        setField(receiver, "id", 20L);
        receiver.setCollege(college);
        receiver.setStatus(AccountStatus.ACTIVE);
        receiver.setEmailVerified(true);

        receiverProfile = new StudentProfile();
        receiverProfile.setUser(receiver);
        receiverProfile.setFullName("Receiver Student");
        receiverProfile.setDepartment("CSE");
        receiverProfile.setCourse("B.Tech");
        receiverProfile.setCurrentYear(2);
        receiverProfile.setBio("Bio");
        receiverProfile.setOnboardingCompleted(true);
        receiverProfile.setDiscoverable(true);

        when(userRepository.findById(20L)).thenReturn(Optional.of(receiver));
        when(profileRepository.findByUserId(20L)).thenReturn(Optional.of(receiverProfile));
        when(safetyService.isBlockedEitherWay(10L, 20L)).thenReturn(false);
    }

    @Test
    void shouldRejectSelfConnection() {
        CreateConnectionRequest req = new CreateConnectionRequest(10L, "Hello");
        assertThrows(IllegalArgumentException.class, () -> connectionService.sendConnectionRequest(sender, req));
    }

    @Test
    void shouldRejectWhenPendingLimitReached() {
        when(connectionRequestRepository.countBySenderIdAndStatus(10L, ConnectionStatus.PENDING)).thenReturn(15L);
        CreateConnectionRequest req = new CreateConnectionRequest(20L, "Let's connect");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> connectionService.sendConnectionRequest(sender, req));
        assertTrue(ex.getMessage().contains("Maximum limit of 15"));
    }

    @Test
    void shouldRejectDifferentColleges() throws Exception {
        College college2 = new College();
        setField(college2, "id", 2L);
        receiver.setCollege(college2);

        CreateConnectionRequest req = new CreateConnectionRequest(20L, "Connect");
        assertThrows(IllegalArgumentException.class, () -> connectionService.sendConnectionRequest(sender, req));
    }

    @Test
    void shouldEnforce24HourCooldownOnRejectedRequest() {
        ConnectionRequest rejected = new ConnectionRequest(sender, receiver, "Old note");
        rejected.setStatus(ConnectionStatus.REJECTED);
        rejected.setUpdatedAt(LocalDateTime.now().minusHours(2));

        when(connectionRequestRepository.findRecentRejectedBetween(any(), any(), any()))
                .thenReturn(List.of(rejected));

        CreateConnectionRequest req = new CreateConnectionRequest(20L, "Connect again");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> connectionService.sendConnectionRequest(sender, req));
        assertTrue(ex.getMessage().contains("cooldown") || ex.getMessage().contains("24 hours"));
    }

    @Test
    void shouldAcceptPendingConnectionOnlyByReceiver() throws Exception {
        ConnectionRequest cr = new ConnectionRequest(sender, receiver, "Note");
        setField(cr, "id", 100L);

        when(connectionRequestRepository.findById(100L)).thenReturn(Optional.of(cr));
        when(connectionRequestRepository.save(any(ConnectionRequest.class))).thenAnswer(i -> i.getArgument(0));

        // Sender cannot accept
        assertThrows(IllegalArgumentException.class, () -> connectionService.acceptConnection(sender, 100L));

        // Receiver can accept
        var res = connectionService.acceptConnection(receiver, 100L);
        assertEquals(ConnectionStatus.ACCEPTED, res.getStatus());
        assertEquals(ConnectionStatus.ACCEPTED, cr.getStatus());
    }

    @Test
    void shouldCancelPendingConnectionOnlyBySender() throws Exception {
        ConnectionRequest cr = new ConnectionRequest(sender, receiver, "Note");
        setField(cr, "id", 101L);

        when(connectionRequestRepository.findById(101L)).thenReturn(Optional.of(cr));
        when(connectionRequestRepository.save(any(ConnectionRequest.class))).thenAnswer(i -> i.getArgument(0));

        // Receiver cannot cancel
        assertThrows(IllegalArgumentException.class, () -> connectionService.cancelConnection(receiver, 101L));

        // Sender can cancel
        var res = connectionService.cancelConnection(sender, 101L);
        assertEquals(ConnectionStatus.CANCELLED, res.getStatus());
        assertEquals(ConnectionStatus.CANCELLED, cr.getStatus());
    }

    @Test
    void shouldRejectWhenAlreadyConnected() {
        when(connectionRequestRepository.existsAcceptedBetween(10L, 20L)).thenReturn(true);
        CreateConnectionRequest req = new CreateConnectionRequest(20L, "Hello");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> connectionService.sendConnectionRequest(sender, req));
        assertTrue(ex.getMessage().contains("already connected"));
    }

    @Test
    void shouldRejectWhenPendingRequestAlreadyExists() {
        when(connectionRequestRepository.existsPendingBetween(10L, 20L)).thenReturn(true);
        CreateConnectionRequest req = new CreateConnectionRequest(20L, "Hello");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> connectionService.sendConnectionRequest(sender, req));
        assertTrue(ex.getMessage().contains("pending connection request already exists"));
    }

    @Test
    void shouldRejectWhenUsersAreBlocked() {
        when(safetyService.isBlockedEitherWay(10L, 20L)).thenReturn(true);
        CreateConnectionRequest req = new CreateConnectionRequest(20L, "Hello");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> connectionService.sendConnectionRequest(sender, req));
        assertTrue(ex.getMessage().contains("Unable to send connection request"));
    }

    @Test
    void shouldRejectWhenReceiverNotDiscoverableOrNotOnboarded() {
        receiverProfile.setDiscoverable(false);
        CreateConnectionRequest req = new CreateConnectionRequest(20L, "Hello");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> connectionService.sendConnectionRequest(sender, req));
        assertTrue(ex.getMessage().contains("not currently discoverable"));
    }

    @Test
    void shouldRejectWhenAccountInactiveOrUnverified() {
        sender.setEmailVerified(false);
        CreateConnectionRequest req = new CreateConnectionRequest(20L, "Hello");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> connectionService.sendConnectionRequest(sender, req));
        assertTrue(ex.getMessage().contains("must be active and verified"));
    }

    @Test
    void shouldRejectAcceptWhenRequestIsNotPending() throws Exception {
        ConnectionRequest cr = new ConnectionRequest(sender, receiver, "Note");
        setField(cr, "id", 102L);
        cr.setStatus(ConnectionStatus.ACCEPTED); // already accepted

        when(connectionRequestRepository.findById(102L)).thenReturn(Optional.of(cr));

        assertThrows(IllegalStateException.class, () -> connectionService.acceptConnection(receiver, 102L));
    }

    @Test
    void shouldRejectCancelWhenRequestIsNotPending() throws Exception {
        ConnectionRequest cr = new ConnectionRequest(sender, receiver, "Note");
        setField(cr, "id", 103L);
        cr.setStatus(ConnectionStatus.REJECTED); // already rejected

        when(connectionRequestRepository.findById(103L)).thenReturn(Optional.of(cr));

        assertThrows(IllegalStateException.class, () -> connectionService.cancelConnection(sender, 103L));
    }

    @Test
    void shouldRejectRejectOnlyByReceiver() throws Exception {
        ConnectionRequest cr = new ConnectionRequest(sender, receiver, "Note");
        setField(cr, "id", 104L);

        when(connectionRequestRepository.findById(104L)).thenReturn(Optional.of(cr));
        when(connectionRequestRepository.save(any(ConnectionRequest.class))).thenAnswer(i -> i.getArgument(0));

        // Sender cannot reject
        assertThrows(IllegalArgumentException.class, () -> connectionService.rejectConnection(sender, 104L));

        // Receiver can reject
        var res = connectionService.rejectConnection(receiver, 104L);
        assertEquals(ConnectionStatus.REJECTED, res.getStatus());
    }

    @Test
    void shouldFilterBlockedUsersInPendingAndAccepted() throws Exception {
        ConnectionRequest cr1 = new ConnectionRequest(sender, receiver, "Note 1");
        setField(cr1, "id", 201L);

        when(connectionRequestRepository.findPendingIncoming(20L)).thenReturn(List.of(cr1));
        when(connectionRequestRepository.findPendingOutgoing(20L)).thenReturn(List.of());
        when(connectionRequestRepository.findAllAcceptedForUser(20L)).thenReturn(List.of(cr1));

        // When blocked
        when(safetyService.isBlockedEitherWay(20L, 10L)).thenReturn(true);

        var pending = connectionService.getPendingConnections(receiver);
        assertTrue(pending.getIncoming().isEmpty());
        assertTrue(pending.getOutgoing().isEmpty());

        var accepted = connectionService.getAcceptedConnections(receiver);
        assertTrue(accepted.isEmpty());
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
