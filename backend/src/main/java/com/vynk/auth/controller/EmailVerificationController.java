package com.vynk.auth.controller;

import com.vynk.auth.dto.ResendOtpRequest;
import com.vynk.auth.dto.ResendVerificationRequest;
import com.vynk.auth.dto.VerifyEmailRequest;
import com.vynk.auth.dto.VerifyOtpRequest;
import com.vynk.auth.service.EmailVerificationService;
import com.vynk.security.ratelimit.RateLimitingService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;
    private final RateLimitingService rateLimitingService;

    public EmailVerificationController(
            EmailVerificationService emailVerificationService) {
        this(emailVerificationService, null);
    }

    @Autowired
    public EmailVerificationController(
            EmailVerificationService emailVerificationService,
            @Autowired(required = false) RateLimitingService rateLimitingService) {
        this.emailVerificationService = emailVerificationService;
        this.rateLimitingService = rateLimitingService;
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, String>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request,
            HttpServletRequest httpRequest) {

        if (rateLimitingService != null) {
            String clientIp = resolveClientIp(httpRequest);
            rateLimitingService.checkLimit("verify-otp", clientIp, 10, 60);
        }

        emailVerificationService.verifyOtp(request.getEmail(), request.getOtp());

        return ResponseEntity.ok(
                Map.of("message", "Email verified successfully! Your VYNK account is now active.")
        );
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<Map<String, String>> resendOtp(
            @Valid @RequestBody ResendOtpRequest request,
            HttpServletRequest httpRequest) {

        if (rateLimitingService != null) {
            String clientIp = resolveClientIp(httpRequest);
            rateLimitingService.checkLimit("resend-otp", clientIp, 3, 60);
        }

        emailVerificationService.resendOtp(request.getEmail());

        return ResponseEntity.ok(
                Map.of("message", "If an eligible unverified account exists for this email, a verification code has been sent.")
        );
    }

    @GetMapping("/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmailGet() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(
                Map.of("message", "Email verification requires explicit confirmation via POST.")
        );
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmailPost(
            @Valid @RequestBody VerifyEmailRequest request) {

        emailVerificationService.verifyEmail(request.getToken());

        return ResponseEntity.ok(
                Map.of("message", "Email verified successfully! Your VYNK account is now active.")
        );
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Map<String, String>> resendVerification(
            @Valid @RequestBody ResendVerificationRequest request,
            HttpServletRequest httpRequest) {

        if (rateLimitingService != null) {
            String clientIp = resolveClientIp(httpRequest);
            rateLimitingService.checkLimit("resend-verification", clientIp, 3, 60);
        }

        emailVerificationService.resendOtp(request.getEmail());

        return ResponseEntity.ok(
                Map.of("message", "If an eligible unverified account exists for this email, a verification code has been sent.")
        );
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return "anonymous";
        }
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String remoteAddr = request.getRemoteAddr();
        return remoteAddr != null && !remoteAddr.isBlank() ? remoteAddr : "anonymous";
    }
}
