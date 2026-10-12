package com.vynk;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.ResourcePropertySource;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class CustomConfigurationPropertiesTest {

    @Test
    @DisplayName("Verify custom vynk.* configuration properties are loaded and have correct defaults")
    void customPropertiesResolveCorrectly() throws IOException {
        StandardEnvironment environment = new StandardEnvironment();
        ResourcePropertySource propertySource = new ResourcePropertySource(new ClassPathResource("application.properties"));
        environment.getPropertySources().addFirst(propertySource);

        // vynk.ratelimit.* properties
        assertEquals("true", environment.getProperty("vynk.ratelimit.enabled"));
        assertEquals("10", environment.getProperty("vynk.ratelimit.login.max"));
        assertEquals("60", environment.getProperty("vynk.ratelimit.login.window"));
        assertEquals("5", environment.getProperty("vynk.ratelimit.register.max"));
        assertEquals("60", environment.getProperty("vynk.ratelimit.register.window"));
        assertEquals("3", environment.getProperty("vynk.ratelimit.resend.max"));
        assertEquals("60", environment.getProperty("vynk.ratelimit.resend.window"));
        assertEquals("3", environment.getProperty("vynk.ratelimit.forgot.max"));
        assertEquals("60", environment.getProperty("vynk.ratelimit.forgot.window"));
        assertEquals("5", environment.getProperty("vynk.ratelimit.reset.max"));
        assertEquals("60", environment.getProperty("vynk.ratelimit.reset.window"));
        assertEquals("20", environment.getProperty("vynk.ratelimit.connections.max"));
        assertEquals("60", environment.getProperty("vynk.ratelimit.connections.window"));
        assertEquals("60", environment.getProperty("vynk.ratelimit.messaging.max"));
        assertEquals("60", environment.getProperty("vynk.ratelimit.messaging.window"));
        assertEquals("20", environment.getProperty("vynk.ratelimit.posts.max"));
        assertEquals("60", environment.getProperty("vynk.ratelimit.posts.window"));
        assertEquals("10", environment.getProperty("vynk.ratelimit.reports.max"));
        assertEquals("60", environment.getProperty("vynk.ratelimit.reports.window"));
        assertEquals("30", environment.getProperty("vynk.ratelimit.matching.max"));
        assertEquals("60", environment.getProperty("vynk.ratelimit.matching.window"));
        assertEquals("10", environment.getProperty("vynk.ratelimit.community.max"));
        assertEquals("86400", environment.getProperty("vynk.ratelimit.community.window"));
        assertEquals("30", environment.getProperty("vynk.ratelimit.discussions.max"));
        assertEquals("3600", environment.getProperty("vynk.ratelimit.discussions.window"));

        // vynk.ai.semantic.* properties
        assertEquals("true", environment.getProperty("vynk.ai.semantic.enabled"));
        assertEquals("http://127.0.0.1:8001", environment.getProperty("vynk.ai.semantic.service-url"));
        assertEquals("1500", environment.getProperty("vynk.ai.semantic.timeout-ms"));

        // vynk.files.* properties
        String expectedUploadDir = System.getProperty("user.home") + "/.vynk/uploads";
        assertEquals(expectedUploadDir, environment.getProperty("vynk.files.upload-dir"));
        assertEquals("5242880", environment.getProperty("vynk.files.max-size-bytes"));

        // vynk.resend.* properties
        assertEquals("onboarding@resend.dev", environment.getProperty("vynk.resend.from-email"));
        assertEquals("https://api.resend.com/emails", environment.getProperty("vynk.resend.api-url"));
        assertEquals("10", environment.getProperty("vynk.resend.timeout-seconds"));
        assertEquals("false", environment.getProperty("vynk.resend.simulation-mode"));

        // vynk.frontend.base-url
        assertEquals("http://localhost:5173", environment.getProperty("vynk.frontend.base-url"));

        // vynk.otp.* properties (must have NO hardcoded fallback in production properties)
        assertEquals("${VYNK_OTP_PEPPER:}", propertySource.getProperty("vynk.otp.pepper"));
        assertEquals("5", environment.getProperty("vynk.otp.expiry-minutes"));
        assertEquals("60", environment.getProperty("vynk.otp.resend-cooldown-seconds"));
        assertEquals("5", environment.getProperty("vynk.otp.max-attempts"));
    }
}
