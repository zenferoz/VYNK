package com.vynk.security.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitingService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingService.class);

    @Value("${vynk.ratelimit.enabled:true}")
    private boolean enabled;

    private final Map<String, LinkedList<Long>> requestBuckets = new ConcurrentHashMap<>();

    public void checkLimit(String action, String key, int maxRequests, long windowSeconds) {
        if (!enabled) {
            return;
        }

        String bucketKey = action + ":" + key;
        long now = Instant.now().getEpochSecond();
        long windowStart = now - windowSeconds;

        LinkedList<Long> timestamps = requestBuckets.computeIfAbsent(bucketKey, k -> new LinkedList<>());

        synchronized (timestamps) {
            // Evict expired entries
            Iterator<Long> iterator = timestamps.iterator();
            while (iterator.hasNext()) {
                if (iterator.next() < windowStart) {
                    iterator.remove();
                } else {
                    break;
                }
            }

            if (timestamps.size() >= maxRequests) {
                long oldest = timestamps.peekFirst() != null ? timestamps.peekFirst() : windowStart;
                long retryAfter = Math.max(1, (oldest + windowSeconds) - now);
                log.warn("Rate limit exceeded for action [{}] and key [{}] ({} reqs in {}s)",
                        action, key, maxRequests, windowSeconds);
                throw new RateLimitExceededException(
                        "Too many requests. Please wait " + retryAfter + " seconds before trying again.",
                        retryAfter
                );
            }

            timestamps.addLast(now);
        }

        // Opportunistic sweep if map grows large
        if (requestBuckets.size() > 5000) {
            cleanupExpiredBuckets();
        }
    }

    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 60000)
    public void cleanupExpiredBuckets() {
        long now = Instant.now().getEpochSecond();
        requestBuckets.entrySet().removeIf(entry -> {
            LinkedList<Long> list = entry.getValue();
            synchronized (list) {
                return list.isEmpty() || (list.peekLast() != null && (now - list.peekLast() > 3600));
            }
        });
    }

    public int getActiveBucketCount() {
        return requestBuckets.size();
    }

    public void resetBucket(String action, String key) {
        requestBuckets.remove(action + ":" + key);
    }
}
