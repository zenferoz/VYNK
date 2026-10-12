
package com.vynk.common.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${vynk.frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(frontendBaseUrl.replaceAll("/+$", ""), "http://localhost:5173"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin",
                "X-CSRF-TOKEN",
                "X-XSRF-TOKEN"
        ));
        configuration.setExposedHeaders(List.of("X-CSRF-TOKEN", "Set-Cookie"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public org.springframework.security.core.session.SessionRegistry sessionRegistry() {
        return new org.springframework.security.core.session.SessionRegistryImpl();
    }

    @Bean
    public org.springframework.security.web.authentication.session.SessionAuthenticationStrategy sessionAuthenticationStrategy(
            org.springframework.security.core.session.SessionRegistry sessionRegistry) {
        return new org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy(sessionRegistry);
    }

    @Bean
    public org.springframework.boot.web.servlet.ServletListenerRegistrationBean<org.springframework.security.web.session.HttpSessionEventPublisher> httpSessionEventPublisher() {
        return new org.springframework.boot.web.servlet.ServletListenerRegistrationBean<>(new org.springframework.security.web.session.HttpSessionEventPublisher());
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            com.vynk.security.session.UserSessionInvalidationService userSessionInvalidationService)
            throws Exception {

        AuthenticationEntryPoint apiAuthenticationEntryPoint =
                (request, response, exception) ->
                        response.sendError(
                                HttpStatus.UNAUTHORIZED.value(),
                                "Authentication required."
                        );

        AccessDeniedHandler apiAccessDeniedHandler =
                (request, response, exception) -> {
                    response.setStatus(HttpStatus.FORBIDDEN.value());
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write(
                            "{\"status\":403,\"error\":\"Forbidden\",\"message\":\"Access denied.\"}"
                    );
                };

        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf
                .ignoringRequestMatchers(
                    "/api/auth/register",
                    "/api/auth/login",
                    "/api/auth/verify-email",
                    "/api/auth/resend-verification",
                    "/api/auth/forgot-password",
                    "/api/auth/reset-password"
                )
            )
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(apiAuthenticationEntryPoint)
                .accessDeniedHandler(apiAccessDeniedHandler)
            )
            .addFilterAfter(
                new com.vynk.security.session.SessionValidationFilter(userSessionInvalidationService),
                org.springframework.security.web.context.SecurityContextHolderFilter.class
            )
            .sessionManagement(session -> session
                .sessionFixation().migrateSession()
                .maximumSessions(-1)
                .sessionRegistry(sessionRegistry())
                .expiredSessionStrategy(event -> {
                    HttpServletResponse response = event.getResponse();
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write(
                        "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Session has expired.\"}"
                    );
                })
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/health").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/colleges").permitAll()
                .requestMatchers("/api/csrf").permitAll()
                .anyRequest().authenticated()
            )
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .logoutSuccessHandler((request, response, authentication) -> {
                    response.setStatus(HttpStatus.OK.value());
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write(
                        "{\"message\":\"Logout successful.\"}"
                    );
                })
            );

        return http.build();
    }
}

