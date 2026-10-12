package com.vynk;

import com.vynk.common.config.SecurityConfig;
import com.vynk.security.session.UserSessionInvalidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebAppConfiguration
@SpringJUnitConfig(classes = CorsConfigurationIntegrationTest.TestConfig.class)
public class CorsConfigurationIntegrationTest {

    private static final String PRODUCTION_ORIGIN = "https://frontend-surya-c640.vercel.app";
    private static final String LOCAL_ORIGIN = "http://localhost:5173";
    private static final String DISALLOWED_ORIGIN = "https://evil-attacker.com";

    @RestController
    static class TestApiController {
        @GetMapping("/api/colleges")
        public ResponseEntity<List<Map<String, Object>>> getColleges() {
            return ResponseEntity.ok(List.of(Map.of("id", 1, "name", "Test College")));
        }

        @GetMapping("/api/session/me")
        public ResponseEntity<Map<String, Object>> getSession() {
            return ResponseEntity.ok(Map.of("status", "authenticated"));
        }
    }

    @Configuration
    @EnableWebMvc
    @Import(SecurityConfig.class)
    static class TestConfig {
        @Bean
        public TestApiController testApiController() {
            return new TestApiController();
        }

        @Bean
        public UserSessionInvalidationService userSessionInvalidationService() {
            return new UserSessionInvalidationService();
        }
    }

    private MockMvc mockMvc;

    @BeforeEach
    void setUp(WebApplicationContext context) {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("Preflight OPTIONS for /api/colleges from production origin returns 200 with CORS headers")
    void preflightCollegesAllowedOriginProduction() throws Exception {
        mockMvc.perform(options("/api/colleges")
                        .header(HttpHeaders.ORIGIN, PRODUCTION_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", PRODUCTION_ORIGIN))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
                .andExpect(header().string("Access-Control-Max-Age", "3600"));
    }

    @Test
    @DisplayName("Actual GET for /api/colleges from production origin returns 200 with CORS headers")
    void actualRequestCollegesAllowedOriginProduction() throws Exception {
        mockMvc.perform(get("/api/colleges")
                        .header(HttpHeaders.ORIGIN, PRODUCTION_ORIGIN))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", PRODUCTION_ORIGIN))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    @DisplayName("Preflight OPTIONS for /api/session/me from production origin returns 200 with CORS headers")
    void preflightSessionMeAllowedOriginProduction() throws Exception {
        mockMvc.perform(options("/api/session/me")
                        .header(HttpHeaders.ORIGIN, PRODUCTION_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", PRODUCTION_ORIGIN))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    @DisplayName("Actual unauthenticated GET for /api/session/me returns 401 with CORS headers attached")
    void actualRequestSessionMeUnauthenticatedRetainsCorsHeaders() throws Exception {
        mockMvc.perform(get("/api/session/me")
                        .header(HttpHeaders.ORIGIN, PRODUCTION_ORIGIN))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Access-Control-Allow-Origin", PRODUCTION_ORIGIN))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    @DisplayName("Preflight OPTIONS from localhost:5173 returns 200 with CORS headers")
    void preflightLocalhostAllowed() throws Exception {
        mockMvc.perform(options("/api/colleges")
                        .header(HttpHeaders.ORIGIN, LOCAL_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", LOCAL_ORIGIN))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    @DisplayName("Preflight OPTIONS from disallowed origin returns 403 without CORS headers")
    void preflightDisallowedOriginRejected() throws Exception {
        mockMvc.perform(options("/api/colleges")
                        .header(HttpHeaders.ORIGIN, DISALLOWED_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    @DisplayName("Actual GET from disallowed origin returns 403 without CORS headers")
    void actualRequestDisallowedOriginRejected() throws Exception {
        mockMvc.perform(get("/api/colleges")
                        .header(HttpHeaders.ORIGIN, DISALLOWED_ORIGIN))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
