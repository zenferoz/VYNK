package com.vynk;

import com.vynk.security.ratelimit.RateLimitExceededException;
import com.vynk.security.ratelimit.RateLimitingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitingServiceTest {

    private RateLimitingService rateLimitingService;

    @BeforeEach
    void setUp() {
        rateLimitingService = new RateLimitingService();
        ReflectionTestUtils.setField(rateLimitingService, "enabled", true);
    }

    @Test
    void allowsRequestsWithinLimit() {
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 5; i++) {
                rateLimitingService.checkLimit("login", "127.0.0.1", 5, 60);
            }
        });
    }

    @Test
    void throwsRateLimitExceededWhenLimitExceeded() {
        for (int i = 0; i < 3; i++) {
            rateLimitingService.checkLimit("resend", "127.0.0.1", 3, 60);
        }

        RateLimitExceededException ex = assertThrows(
                RateLimitExceededException.class,
                () -> rateLimitingService.checkLimit("resend", "127.0.0.1", 3, 60)
        );

        assertTrue(ex.getRetryAfterSeconds() > 0);
        assertTrue(ex.getMessage().contains("Too many requests"));
    }

    @Test
    void resetsBucketSuccessfully() {
        for (int i = 0; i < 2; i++) {
            rateLimitingService.checkLimit("action", "user1", 2, 60);
        }

        assertThrows(RateLimitExceededException.class, () ->
                rateLimitingService.checkLimit("action", "user1", 2, 60)
        );

        rateLimitingService.resetBucket("action", "user1");

        assertDoesNotThrow(() ->
                rateLimitingService.checkLimit("action", "user1", 2, 60)
        );
    }

    @Test
    void bypassesWhenDisabled() {
        ReflectionTestUtils.setField(rateLimitingService, "enabled", false);
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 10; i++) {
                rateLimitingService.checkLimit("login", "127.0.0.1", 2, 60);
            }
        });
    }

    @Test
    void cleansUpExpiredBuckets() {
        rateLimitingService.checkLimit("test", "key1", 5, 60);
        assertTrue(rateLimitingService.getActiveBucketCount() > 0);

        rateLimitingService.cleanupExpiredBuckets();
        assertEquals(1, rateLimitingService.getActiveBucketCount());

        rateLimitingService.resetBucket("test", "key1");
        assertEquals(0, rateLimitingService.getActiveBucketCount());
    }
}
