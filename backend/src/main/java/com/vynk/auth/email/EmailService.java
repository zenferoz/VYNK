package com.vynk.auth.email;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String fromEmail;
    private final String apiUrl;
    private final long timeoutSeconds;
    private final boolean simulationMode;

    @Autowired
    public EmailService(
            @Value("${vynk.resend.api-key:${RESEND_API_KEY:}}") String apiKey,
            @Value("${vynk.resend.from-email:${RESEND_FROM_EMAIL:onboarding@resend.dev}}") String fromEmail,
            @Value("${vynk.resend.api-url:${RESEND_API_URL:https://api.resend.com/emails}}") String apiUrl,
            @Value("${vynk.resend.timeout-seconds:${RESEND_TIMEOUT_SECONDS:10}}") long timeoutSeconds,
            @Value("${vynk.resend.simulation-mode:${VYNK_EMAIL_SIMULATION_MODE:false}}") boolean simulationMode,
            @Autowired(required = false) ObjectMapper objectMapper) {
        this(apiKey, fromEmail, apiUrl, timeoutSeconds, simulationMode,
                objectMapper != null ? objectMapper : new ObjectMapper(),
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(timeoutSeconds > 0 ? timeoutSeconds : 10)).build());
    }

    public EmailService(
            String apiKey,
            String fromEmail,
            String apiUrl,
            long timeoutSeconds,
            boolean simulationMode,
            ObjectMapper objectMapper,
            HttpClient httpClient) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.fromEmail = fromEmail != null && !fromEmail.isBlank() ? fromEmail.trim() : "onboarding@resend.dev";
        this.apiUrl = apiUrl != null && !apiUrl.isBlank() ? apiUrl.trim() : "https://api.resend.com/emails";
        this.timeoutSeconds = timeoutSeconds > 0 ? timeoutSeconds : 10;
        this.simulationMode = simulationMode;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
        this.httpClient = httpClient != null ? httpClient : HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    public EmailService(
            String apiKey,
            String fromEmail,
            String apiUrl,
            long timeoutSeconds,
            ObjectMapper objectMapper,
            HttpClient httpClient) {
        this(apiKey, fromEmail, apiUrl, timeoutSeconds, false, objectMapper, httpClient);
    }

    /**
     * Sends a 6-digit verification code for new student account registration.
     */
    public void sendVerificationOtp(String recipientEmail, String otp) {
        String subject = "Your VYNK Verification Code";
        String textContent = "Welcome to VYNK!\n\n"
                + "Your 6-digit verification code is: " + otp + "\n\n"
                + "This code expires in 5 minutes.\n"
                + "For your security, never share this code with anyone.\n\n"
                + "If you did not create a VYNK account, you can safely ignore this email.";

        String htmlContent = "<!DOCTYPE html>"
                + "<html><head><meta charset=\"utf-8\"></head><body style=\"font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #fafafa; padding: 24px; color: #111827;\">"
                + "<div style=\"max-width: 480px; margin: 0 auto; background: #ffffff; border: 1px solid #e5e7eb; border-radius: 8px; padding: 32px;\">"
                + "<h1 style=\"font-size: 20px; font-weight: 700; margin: 0 0 16px 0; color: #111827;\">Verify Your College Email</h1>"
                + "<p style=\"font-size: 14px; line-height: 1.5; color: #4b5563; margin: 0 0 24px 0;\">Welcome to VYNK. Enter the 6-digit verification code below to activate your student account:</p>"
                + "<div style=\"background: #000000; border-radius: 6px; padding: 18px; text-align: center; margin: 0 0 24px 0;\">"
                + "<span style=\"font-size: 32px; font-weight: 700; letter-spacing: 8px; font-family: monospace; color: #ffffff;\">" + otp + "</span>"
                + "</div>"
                + "<p style=\"font-size: 13px; line-height: 1.5; color: #6b7280; margin: 0 0 8px 0;\">This code expires in <strong>5 minutes</strong> and can only be used once.</p>"
                + "<p style=\"font-size: 12px; line-height: 1.4; color: #9ca3af; margin: 16px 0 0 0; border-top: 1px solid #f3f4f6; padding-top: 16px;\">If you did not request this verification, please safely ignore this email.</p>"
                + "</div></body></html>";

        sendEmail(recipientEmail, subject, textContent, htmlContent);
    }

    /**
     * Sends password reset email containing secure reset link.
     */
    public void sendPasswordResetEmail(String recipientEmail, String resetLink) {
        String subject = "Reset your VYNK password";
        String textContent = "Hello from VYNK,\n\n"
                + "We received a request to reset the password for your VYNK account.\n\n"
                + "Click the link below to set a new password:\n\n"
                + resetLink
                + "\n\nThis link expires in 1 hour and can only be used once.\n\n"
                + "If you did not request a password reset, you can safely ignore this email. "
                + "Your account remains secure.";

        String htmlContent = "<!DOCTYPE html>"
                + "<html><head><meta charset=\"utf-8\"></head><body style=\"font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #fafafa; padding: 24px; color: #111827;\">"
                + "<div style=\"max-width: 480px; margin: 0 auto; background: #ffffff; border: 1px solid #e5e7eb; border-radius: 8px; padding: 32px;\">"
                + "<h1 style=\"font-size: 20px; font-weight: 700; margin: 0 0 16px 0; color: #111827;\">Reset Your Password</h1>"
                + "<p style=\"font-size: 14px; line-height: 1.5; color: #4b5563; margin: 0 0 24px 0;\">We received a request to reset your password. Click the button below to choose a new password:</p>"
                + "<div style=\"text-align: center; margin: 0 0 24px 0;\">"
                + "<a href=\"" + resetLink + "\" style=\"display: inline-block; background: #000000; color: #ffffff; padding: 12px 28px; text-decoration: none; border-radius: 6px; font-size: 14px; font-weight: 600;\">Reset Password &rarr;</a>"
                + "</div>"
                + "<p style=\"font-size: 13px; line-height: 1.5; color: #6b7280; margin: 0;\">This link expires in <strong>1 hour</strong> and can only be used once.</p>"
                + "<p style=\"font-size: 12px; line-height: 1.4; color: #9ca3af; margin: 16px 0 0 0; border-top: 1px solid #f3f4f6; padding-top: 16px;\">If you did not request a password reset, you can safely ignore this email.</p>"
                + "</div></body></html>";

        sendEmail(recipientEmail, subject, textContent, htmlContent);
    }

    /**
     * Legacy link verification email method (maintained for backwards compatibility).
     */
    public void sendVerificationEmail(String recipientEmail, String verificationLink) {
        String subject = "Verify your VYNK account";
        String textContent = "Welcome to VYNK!\n\n"
                + "Click the link below to verify your email address:\n\n"
                + verificationLink
                + "\n\nThis link expires in 24 hours.\n\n"
                + "If you did not create a VYNK account, you can safely ignore this email.";

        String htmlContent = "<!DOCTYPE html>"
                + "<html><body style=\"font-family: sans-serif; padding: 20px;\">"
                + "<h2>Welcome to VYNK</h2>"
                + "<p>Click the link below to verify your email:</p>"
                + "<p><a href=\"" + verificationLink + "\">Verify Email Address</a></p>"
                + "<p>This link expires in 24 hours.</p>"
                + "</body></html>";

        sendEmail(recipientEmail, subject, textContent, htmlContent);
    }

    /**
     * Sends email payload via Resend's HTTPS REST API.
     */
    protected void sendEmail(String recipientEmail, String subject, String textContent, String htmlContent) {
        if (apiKey == null || apiKey.isBlank()) {
            if (simulationMode) {
                log.warn("SIMULATION MODE: RESEND_API_KEY is not configured. Email to [{}] simulated successfully (subject: '{}')",
                        maskEmail(recipientEmail), subject);
                return;
            }
            log.error("RESEND_API_KEY is not configured and simulation mode is disabled.");
            throw new EmailDeliveryException("Email service is not configured. RESEND_API_KEY is required.");
        }

        try {
            Map<String, Object> payload = new HashMap<>();
            String formattedFrom = fromEmail.contains("<") ? fromEmail : "VYNK <" + fromEmail + ">";
            payload.put("from", formattedFrom);
            payload.put("to", List.of(recipientEmail.trim()));
            payload.put("subject", subject);
            payload.put("text", textContent);
            payload.put("html", htmlContent);

            String jsonPayload = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "VYNK-Backend/1.0")
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            int status = response.statusCode();
            if (status >= 200 && status < 300) {
                log.info("Email successfully accepted by Resend API for recipient [{}] (HTTP {})", maskEmail(recipientEmail), status);
            } else {
                String sanitizedBody = sanitizeResponseBody(response.body());
                log.error("Resend API rejected email to [{}] with HTTP {}: {}", maskEmail(recipientEmail), status, sanitizedBody);
                throw new EmailDeliveryException("Resend API rejected email delivery with status " + status + ": " + sanitizedBody);
            }
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("Network failure communicating with Resend API for recipient [{}]: {}", maskEmail(recipientEmail), ex.getMessage());
            throw new EmailDeliveryException("Resend API delivery failed: " + ex.getMessage(), ex);
        }
    }

    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "***";
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return "***" + (atIndex >= 0 ? email.substring(atIndex) : "");
        }
        String local = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        if (local.length() <= 2) {
            return local.charAt(0) + "***" + domain;
        }
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + domain;
    }

    private String sanitizeResponseBody(String body) {
        if (body == null || body.isBlank()) {
            return "No response body";
        }
        // Limit length and trim whitespace to prevent log pollution
        return body.length() > 500 ? body.substring(0, 500) + "..." : body.trim();
    }
}
