package com.vynk.matching.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SemanticInferenceClient {

    private static final Logger log = LoggerFactory.getLogger(SemanticInferenceClient.class);

    @Value("${vynk.ai.semantic.enabled:true}")
    private boolean enabled;

    @Value("${vynk.ai.semantic.service-url:http://127.0.0.1:8001}")
    private String serviceUrl;

    @Value("${vynk.ai.semantic.timeout-ms:1500}")
    private int timeoutMs;

    private final HttpClient httpClient;
    private final Map<String, Double> similarityCache = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 1000;

    public SemanticInferenceClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(1000))
                .build();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Optional<List<Double>> computeSimilarities(String sourceText, List<String> candidateTexts) {
        if (!enabled || sourceText == null || candidateTexts == null || candidateTexts.isEmpty()) {
            return Optional.empty();
        }

        // Check if all pairs are cached
        List<Double> cachedResults = new ArrayList<>();
        boolean allCached = true;
        for (String candText : candidateTexts) {
            String cacheKey = getCacheKey(sourceText, candText);
            Double cached = similarityCache.get(cacheKey);
            if (cached != null) {
                cachedResults.add(cached);
            } else {
                allCached = false;
                break;
            }
        }

        if (allCached) {
            return Optional.of(cachedResults);
        }

        try {
            // Build minimal JSON payload safely
            StringBuilder json = new StringBuilder();
            json.append("{\"source_text\":").append(escapeJson(sourceText)).append(",\"candidate_texts\":[");
            for (int i = 0; i < candidateTexts.size(); i++) {
                if (i > 0) json.append(",");
                json.append(escapeJson(candidateTexts.get(i)));
            }
            json.append("]}");

            String url = serviceUrl.replaceAll("/+$", "") + "/semantic-similarity";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofMillis(timeoutMs))
                    .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                List<Double> scores = parseSimilarities(response.body());
                if (scores.size() == candidateTexts.size()) {
                    // Populate bounded cache
                    if (similarityCache.size() > MAX_CACHE_SIZE) {
                        similarityCache.clear();
                    }
                    for (int i = 0; i < candidateTexts.size(); i++) {
                        similarityCache.put(getCacheKey(sourceText, candidateTexts.get(i)), scores.get(i));
                    }
                    return Optional.of(scores);
                }
            } else {
                log.warn("Semantic inference returned HTTP status {}: {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.warn("Semantic inference service unreachable or timed out ({}). Falling back to deterministic matching.", e.getMessage());
        }

        return Optional.empty();
    }

    private String getCacheKey(String s1, String s2) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash1 = md.digest((s1 != null ? s1 : "").getBytes(java.nio.charset.StandardCharsets.UTF_8));
            byte[] hash2 = md.digest((s2 != null ? s2 : "").getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash1, 0, 8) + ":" + java.util.HexFormat.of().formatHex(hash2, 0, 8);
        } catch (java.security.NoSuchAlgorithmException e) {
            return (s1 != null ? s1.hashCode() : 0) + ":" + (s2 != null ? s2.hashCode() : 0);
        }
    }

    private String escapeJson(String str) {
        if (str == null) return "\"\"";
        StringBuilder sb = new StringBuilder("\"");
        for (char c : str.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < ' ') {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append("\"");
        return sb.toString();
    }

    private List<Double> parseSimilarities(String json) {
        List<Double> result = new ArrayList<>();
        int keyIndex = json.indexOf("\"similarities\":");
        if (keyIndex == -1) return result;

        int startBracket = json.indexOf('[', keyIndex);
        int endBracket = json.indexOf(']', startBracket);
        if (startBracket == -1 || endBracket == -1) return result;

        String arrayContent = json.substring(startBracket + 1, endBracket).trim();
        if (arrayContent.isEmpty()) return result;

        String[] parts = arrayContent.split(",");
        for (String p : parts) {
            try {
                result.add(Double.parseDouble(p.trim()));
            } catch (NumberFormatException ignored) {
                result.add(0.0);
            }
        }
        return result;
    }
}
