package com.vynk;

import com.vynk.matching.service.SemanticInferenceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SemanticInferenceClientTest {

    private SemanticInferenceClient client;

    @BeforeEach
    void setUp() {
        client = new SemanticInferenceClient();
        ReflectionTestUtils.setField(client, "enabled", true);
        ReflectionTestUtils.setField(client, "serviceUrl", "http://127.0.0.1:9999"); // unreachable port
        ReflectionTestUtils.setField(client, "timeoutMs", 200);
    }

    @Test
    void returnsEmptyWhenDisabled() {
        ReflectionTestUtils.setField(client, "enabled", false);
        Optional<List<Double>> result = client.computeSimilarities("bio test", List.of("cand bio"));
        assertTrue(result.isEmpty());
    }

    @Test
    void returnsEmptyWhenInputIsNull() {
        assertTrue(client.computeSimilarities(null, List.of("cand bio")).isEmpty());
        assertTrue(client.computeSimilarities("bio test", null).isEmpty());
        assertTrue(client.computeSimilarities("bio test", List.of()).isEmpty());
    }

    @Test
    void fallsBackGracefullyWhenServiceUnreachable() {
        // Will attempt connection to port 9999, fail gracefully, and return Optional.empty()
        Optional<List<Double>> result = client.computeSimilarities("bio test", List.of("cand bio"));
        assertTrue(result.isEmpty(), "Should gracefully return Optional.empty on connection failure");
    }
}
