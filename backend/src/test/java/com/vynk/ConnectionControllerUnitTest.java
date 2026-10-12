package com.vynk;

import com.vynk.common.exception.GlobalExceptionHandler;
import com.vynk.common.exception.ResourceNotFoundException;
import com.vynk.connection.controller.ConnectionController;
import com.vynk.connection.dto.ConnectionResponse;
import com.vynk.connection.dto.CreateConnectionRequest;
import com.vynk.connection.dto.PendingConnectionsResponse;
import com.vynk.connection.entity.ConnectionStatus;
import com.vynk.connection.service.ConnectionService;
import com.vynk.user.entity.User;
import com.vynk.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Field;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ConnectionControllerUnitTest {

    private MockMvc mockMvc;
    private ConnectionService connectionService;
    private UserRepository userRepository;

    private User currentUser;
    private Principal principal;

    @BeforeEach
    void setUp() throws Exception {
        connectionService = Mockito.mock(ConnectionService.class);
        userRepository = Mockito.mock(UserRepository.class);

        ConnectionController controller = new ConnectionController(connectionService, userRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        currentUser = new User();
        Field idField = User.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(currentUser, 100L);
        currentUser.setEmail("current@college.edu");

        principal = new UsernamePasswordAuthenticationToken("current@college.edu", "password");
        when(userRepository.findByEmail("current@college.edu")).thenReturn(Optional.of(currentUser));
    }

    @Test
    void shouldSendConnectionRequestSuccessfully() throws Exception {
        ConnectionResponse response = new ConnectionResponse(
                1L, 200L, "Peer Student", "CSE", 3,
                ConnectionStatus.PENDING, "Let's collaborate on hackathon",
                LocalDateTime.now(), LocalDateTime.now(), "OUTGOING"
        );

        when(connectionService.sendConnectionRequest(eq(currentUser), any(CreateConnectionRequest.class)))
                .thenReturn(response);

        String json = """
                {
                  "receiverId": 200,
                  "purposeNote": "Let's collaborate on hackathon"
                }
                """;

        mockMvc.perform(post("/api/connections/request")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value(200))
                .andExpect(jsonPath("$.fullName").value("Peer Student"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.direction").value("OUTGOING"));
    }

    @Test
    void shouldRejectSendRequestWhenReceiverIdMissing() throws Exception {
        String json = """
                {
                  "purposeNote": "Missing receiver id"
                }
                """;

        mockMvc.perform(post("/api/connections/request")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.receiverId").exists());
    }

    @Test
    void shouldGetPendingConnections() throws Exception {
        ConnectionResponse inc = new ConnectionResponse(
                10L, 50L, "Incoming Peer", "IT", 2,
                ConnectionStatus.PENDING, "DSA Study",
                LocalDateTime.now(), LocalDateTime.now(), "INCOMING"
        );
        ConnectionResponse out = new ConnectionResponse(
                11L, 60L, "Outgoing Peer", "ECE", 4,
                ConnectionStatus.PENDING, "Project",
                LocalDateTime.now(), LocalDateTime.now(), "OUTGOING"
        );

        PendingConnectionsResponse pending = new PendingConnectionsResponse(List.of(inc), List.of(out));
        when(connectionService.getPendingConnections(currentUser)).thenReturn(pending);

        mockMvc.perform(get("/api/connections/pending")
                        .principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incoming[0].id").value(10))
                .andExpect(jsonPath("$.incoming[0].fullName").value("Incoming Peer"))
                .andExpect(jsonPath("$.outgoing[0].id").value(11))
                .andExpect(jsonPath("$.outgoing[0].fullName").value("Outgoing Peer"));
    }

    @Test
    void shouldGetAcceptedConnections() throws Exception {
        ConnectionResponse conn = new ConnectionResponse(
                15L, 70L, "Connected Peer", "CSE", 3,
                ConnectionStatus.ACCEPTED, null,
                LocalDateTime.now(), LocalDateTime.now(), "OUTGOING"
        );

        when(connectionService.getAcceptedConnections(currentUser)).thenReturn(List.of(conn));

        mockMvc.perform(get("/api/connections")
                        .principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(15))
                .andExpect(jsonPath("$[0].fullName").value("Connected Peer"))
                .andExpect(jsonPath("$[0].status").value("ACCEPTED"));
    }

    @Test
    void shouldAcceptConnectionRequest() throws Exception {
        ConnectionResponse accepted = new ConnectionResponse(
                20L, 80L, "Peer", "CSE", 2,
                ConnectionStatus.ACCEPTED, null,
                LocalDateTime.now(), LocalDateTime.now(), "INCOMING"
        );

        when(connectionService.acceptConnection(currentUser, 20L)).thenReturn(accepted);

        mockMvc.perform(post("/api/connections/20/accept")
                        .principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    void shouldReturn404WhenAcceptingNonexistentRequest() throws Exception {
        when(connectionService.acceptConnection(currentUser, 999L))
                .thenThrow(new ResourceNotFoundException("Connection request not found"));

        mockMvc.perform(post("/api/connections/999/accept")
                        .principal(principal))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Connection request not found"));
    }

    @Test
    void shouldRejectConnectionRequest() throws Exception {
        ConnectionResponse rejected = new ConnectionResponse(
                21L, 81L, "Peer", "CSE", 2,
                ConnectionStatus.REJECTED, null,
                LocalDateTime.now(), LocalDateTime.now(), "INCOMING"
        );

        when(connectionService.rejectConnection(currentUser, 21L)).thenReturn(rejected);

        mockMvc.perform(post("/api/connections/21/reject")
                        .principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void shouldCancelConnectionRequest() throws Exception {
        ConnectionResponse cancelled = new ConnectionResponse(
                22L, 82L, "Peer", "CSE", 2,
                ConnectionStatus.CANCELLED, null,
                LocalDateTime.now(), LocalDateTime.now(), "OUTGOING"
        );

        when(connectionService.cancelConnection(currentUser, 22L)).thenReturn(cancelled);

        mockMvc.perform(post("/api/connections/22/cancel")
                        .principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}
