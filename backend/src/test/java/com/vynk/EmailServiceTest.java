package com.vynk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vynk.auth.email.EmailDeliveryException;
import com.vynk.auth.email.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EmailServiceTest {

    private HttpClient httpClient;
    private HttpResponse<String> httpResponse;
    private ObjectMapper objectMapper;
    private final String testApiKey = "re_test_api_key_12345";
    private final String fromEmail = "onboarding@resend.dev";
    private final String apiUrl = "https://api.resend.com/emails";

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        httpClient = Mockito.mock(HttpClient.class);
        httpResponse = Mockito.mock(HttpResponse.class);
        objectMapper = new ObjectMapper();
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendVerificationOtpDeliversSuccessfullyWhenResendReturns200() throws Exception {
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("{\"id\":\"msg_12345\"}");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        EmailService emailService = new EmailService(
                testApiKey,
                fromEmail,
                apiUrl,
                10,
                false,
                objectMapper,
                httpClient
        );

        assertDoesNotThrow(() -> emailService.sendVerificationOtp("student@college.edu", "654321"));

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(requestCaptor.capture(), any(HttpResponse.BodyHandler.class));

        HttpRequest sentRequest = requestCaptor.getValue();
        assertEquals("POST", sentRequest.method());
        assertEquals(apiUrl, sentRequest.uri().toString());
        assertEquals("Bearer " + testApiKey, sentRequest.headers().firstValue("Authorization").orElse(""));
        assertEquals("application/json", sentRequest.headers().firstValue("Content-Type").orElse(""));
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendPasswordResetEmailDeliversSuccessfullyWhenResendReturns200() throws Exception {
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("{\"id\":\"msg_reset_999\"}");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        EmailService emailService = new EmailService(
                testApiKey,
                fromEmail,
                apiUrl,
                10,
                false,
                objectMapper,
                httpClient
        );

        assertDoesNotThrow(() -> emailService.sendPasswordResetEmail("student@college.edu", "https://vynk.com/reset-password?token=abc"));

        verify(httpClient, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendVerificationOtpThrowsEmailDeliveryExceptionWhenResendReturns4xxError() throws Exception {
        when(httpResponse.statusCode()).thenReturn(422);
        when(httpResponse.body()).thenReturn("{\"statusCode\":422,\"message\":\"Domain not verified\"}");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        EmailService emailService = new EmailService(
                testApiKey,
                fromEmail,
                apiUrl,
                10,
                false,
                objectMapper,
                httpClient
        );

        EmailDeliveryException ex = assertThrows(EmailDeliveryException.class,
                () -> emailService.sendVerificationOtp("student@college.edu", "654321"));

        assertTrue(ex.getMessage().contains("Resend API rejected email delivery with status 422"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendVerificationOtpThrowsEmailDeliveryExceptionOnNetworkOrTimeoutFailure() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new IOException("Connection timed out after 10000ms"));

        EmailService emailService = new EmailService(
                testApiKey,
                fromEmail,
                apiUrl,
                10,
                false,
                objectMapper,
                httpClient
        );

        EmailDeliveryException ex = assertThrows(EmailDeliveryException.class,
                () -> emailService.sendVerificationOtp("student@college.edu", "654321"));

        assertTrue(ex.getMessage().contains("Resend API delivery failed"));
        assertTrue(ex.getMessage().contains("Connection timed out"));
    }

    @Test
    void simulationModeAllowsMissingApiKeyWithoutThrowing() {
        EmailService emailService = new EmailService(
                "", // Blank API key
                fromEmail,
                apiUrl,
                10,
                true, // simulationMode enabled
                objectMapper,
                httpClient
        );

        assertDoesNotThrow(() -> emailService.sendVerificationOtp("student@college.edu", "123456"));
        verifyNoInteractions(httpClient);
    }

    @Test
    void nonSimulationModeThrowsExceptionWhenApiKeyIsMissing() {
        EmailService emailService = new EmailService(
                "", // Blank API key
                fromEmail,
                apiUrl,
                10,
                false, // simulationMode disabled
                objectMapper,
                httpClient
        );

        EmailDeliveryException ex = assertThrows(EmailDeliveryException.class,
                () -> emailService.sendVerificationOtp("student@college.edu", "123456"));

        assertTrue(ex.getMessage().contains("RESEND_API_KEY is required"));
        verifyNoInteractions(httpClient);
    }

    @Test
    void maskEmailProtectsStudentPrivacyInLogs() {
        assertEquals("s***a@college.edu", EmailService.maskEmail("surya@college.edu"));
        assertEquals("j***e@test.edu", EmailService.maskEmail("jane@test.edu"));
        assertEquals("a***@test.edu", EmailService.maskEmail("ab@test.edu"));
        assertEquals("***@test.edu", EmailService.maskEmail("a@test.edu"));
        assertEquals("***", EmailService.maskEmail(null));
        assertEquals("***", EmailService.maskEmail(""));
    }
}
