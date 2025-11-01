package ru.oldzoomer.stingraytv_alice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Security configuration for Yandex Smart Home integration.
 * Provides OAuth2 resource server with JWT authentication and CORS configuration.
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final CorsConfigurationSource corsConfigurationSource;

    /**
     * Main security filter chain configuration
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        log.info("Configuring security filter chain");

        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
                new OAuth2AuthorizationServerConfigurer();
        http
                // CORS configuration
                .cors(cors -> cors.configurationSource(corsConfigurationSource))

                // Disable CSRF for stateless API
                .csrf(AbstractHttpConfigurer::disable)

                // Stateless session management
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Authorization rules
                .authorizeHttpRequests(authz -> authz
                        // Public endpoints
                        .requestMatchers(
                                "/oauth/authorize",
                                "/oauth/login",
                                "/oauth/token",
                                "/login",
                                "/v1.0"
                        ).permitAll()

                        // Protected API endpoints
                        .requestMatchers("/v1.0/user/devices/**").authenticated()
                        .requestMatchers("/v1.0/user/unlink").authenticated()
                        .requestMatchers("/v1.0/admin/**").hasRole("ADMIN")

                        // Default to permit all for other endpoints
                        .anyRequest().permitAll()
                )

                // OAuth2 authorization server configuration
                .with(authorizationServerConfigurer, Customizer.withDefaults())

                // OAuth2 resource server configuration
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))

                // Exception handling
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            log.warn("Authentication failed for request: {} {}",
                                    request.getMethod(), request.getRequestURI());
                            response.setStatus(401);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Authentication required\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            log.warn("Access denied for request: {} {}",
                                    request.getMethod(), request.getRequestURI());
                            response.setStatus(403);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\":\"Forbidden\",\"message\":\"Access denied\"}");
                        })
                );

        return http.build();
    }
}
