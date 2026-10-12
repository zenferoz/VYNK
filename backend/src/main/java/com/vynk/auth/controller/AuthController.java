package com.vynk.auth.controller;

import com.vynk.auth.dto.LoginRequest;
import com.vynk.auth.dto.LoginResponse;
import com.vynk.auth.dto.RegisterRequest;
import com.vynk.auth.service.RegistrationService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;
import java.util.Map;

import com.vynk.auth.dto.ForgotPasswordRequest;
import com.vynk.auth.dto.ResetPasswordRequest;
import com.vynk.auth.service.PasswordResetService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final RegistrationService registrationService;
    private final AuthenticationManager authenticationManager;
    private final PasswordResetService passwordResetService;

    private final HttpSessionSecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    private final com.vynk.security.ratelimit.RateLimitingService rateLimitingService;
    private final org.springframework.security.web.authentication.session.SessionAuthenticationStrategy sessionAuthenticationStrategy;

    public AuthController(
            RegistrationService registrationService,
            AuthenticationManager authenticationManager) {
        this(registrationService, authenticationManager, null, null, null);
    }

    public AuthController(
            RegistrationService registrationService,
            AuthenticationManager authenticationManager,
            com.vynk.security.ratelimit.RateLimitingService rateLimitingService) {
        this(registrationService, authenticationManager, null, rateLimitingService, null);
    }

    public AuthController(
            RegistrationService registrationService,
            AuthenticationManager authenticationManager,
            PasswordResetService passwordResetService,
            com.vynk.security.ratelimit.RateLimitingService rateLimitingService) {
        this(registrationService, authenticationManager, passwordResetService, rateLimitingService, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public AuthController(
            RegistrationService registrationService,
            AuthenticationManager authenticationManager,
            @org.springframework.beans.factory.annotation.Autowired(required = false) PasswordResetService passwordResetService,
            @org.springframework.beans.factory.annotation.Autowired(required = false) com.vynk.security.ratelimit.RateLimitingService rateLimitingService,
            @org.springframework.beans.factory.annotation.Autowired(required = false) org.springframework.security.web.authentication.session.SessionAuthenticationStrategy sessionAuthenticationStrategy) {
        this.registrationService = registrationService;
        this.authenticationManager = authenticationManager;
        this.passwordResetService = passwordResetService;
        this.rateLimitingService = rateLimitingService;
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {

        if (rateLimitingService != null) {
            String clientIp = resolveClientIp(httpRequest);
            rateLimitingService.checkLimit("register", clientIp, 5, 60);
        }

        registrationService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of(
                        "message",
                        "Registration successful. Email verification is pending."
                ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        if (rateLimitingService != null) {
            String clientIp = resolveClientIp(httpRequest);
            rateLimitingService.checkLimit("login", clientIp, 10, 60);
        }

        try {
            String email = request.getEmail()
                    .trim()
                    .toLowerCase(Locale.ROOT);

            Authentication authentication =
                    authenticationManager.authenticate(
                            UsernamePasswordAuthenticationToken
                                    .unauthenticated(
                                            email,
                                            request.getPassword()
                                    )
                    );

            SecurityContext context =
                    SecurityContextHolder.createEmptyContext();

            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            jakarta.servlet.http.HttpSession session = httpRequest.getSession(true);
            httpRequest.changeSessionId();

            if (sessionAuthenticationStrategy != null) {
                sessionAuthenticationStrategy.onAuthentication(authentication, httpRequest, httpResponse);
            }
            if (session != null) {
                session.setAttribute("VYNK_AUTH_TIME", java.time.Instant.now());
            }

            securityContextRepository.saveContext(
                    context,
                    httpRequest,
                    httpResponse
            );

            return ResponseEntity.ok(
                    new LoginResponse(
                            "Login successful.",
                            authentication.getName()
                    )
            );

        } catch (BadCredentialsException ex) {
            SecurityContextHolder.clearContext();

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "message",
                            "Invalid email or password."
                    ));

        } catch (DisabledException | LockedException ex) {
            SecurityContextHolder.clearContext();

            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "message",
                            "This account is not currently allowed to log in."
                    ));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest) {

        if (rateLimitingService != null) {
            String clientIp = resolveClientIp(httpRequest);
            rateLimitingService.checkLimit("forgot-password", clientIp, 3, 60);
        }

        if (passwordResetService != null) {
            passwordResetService.requestPasswordReset(request.getEmail());
        }

        return ResponseEntity.ok(Map.of(
                "message",
                "If an eligible account exists for this email, password reset instructions have been sent."
        ));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request,
            HttpServletRequest httpRequest) {

        if (rateLimitingService != null) {
            String clientIp = resolveClientIp(httpRequest);
            rateLimitingService.checkLimit("reset-password", clientIp, 5, 60);
        }

        if (passwordResetService == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("message", "Password reset service is temporarily unavailable."));
        }

        passwordResetService.resetPassword(request.getToken(), request.getNewPassword());

        return ResponseEntity.ok(Map.of(
                "message",
                "Your password has been successfully reset. You can now log in."
        ));
    }

    private String resolveClientIp(HttpServletRequest httpRequest) {
        if (httpRequest == null) {
            return "anonymous";
        }
        String remoteAddr = httpRequest.getRemoteAddr();
        return (remoteAddr != null && !remoteAddr.isBlank()) ? remoteAddr : "anonymous";
    }
}