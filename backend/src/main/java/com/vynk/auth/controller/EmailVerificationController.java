
package com.vynk.auth.controller;

import com.vynk.auth.dto.ResendVerificationRequest;
import com.vynk.auth.dto.VerifyEmailRequest;
import com.vynk.auth.service.EmailVerificationService;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;
    private final com.vynk.security.ratelimit.RateLimitingService rateLimitingService;

    public EmailVerificationController(
            EmailVerificationService emailVerificationService) {
        this(emailVerificationService, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public EmailVerificationController(
            EmailVerificationService emailVerificationService,
            com.vynk.security.ratelimit.RateLimitingService rateLimitingService) {
        this.emailVerificationService = emailVerificationService;
        this.rateLimitingService = rateLimitingService;
    }

    @GetMapping("/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmailGet() {
        return ResponseEntity.status(org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED).body(
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
            jakarta.servlet.http.HttpServletRequest httpRequest) {

        if (rateLimitingService != null) {
            String clientIp = httpRequest != null && httpRequest.getRemoteAddr() != null ? httpRequest.getRemoteAddr() : "anonymous";
            rateLimitingService.checkLimit("resend-verification", clientIp, 3, 60);
        }

        emailVerificationService.resendVerificationToken(request.getEmail());

        return ResponseEntity.ok(
                Map.of("message", "If an eligible unverified account exists for this email, a verification link has been sent.")
        );
    }
}
