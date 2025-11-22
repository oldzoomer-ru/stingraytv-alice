package ru.oldzoomer.stingraytv_alice.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * CORS configuration for cross-origin requests
 */
@Configuration
@Slf4j
public class CorsConfig {
    @Value("${cors.allowed-origins:*}")
    private String corsAllowedOrigins;

    @Value("${cors.allowed-methods:GET,POST,HEAD}")
    private String corsAllowedMethods;

    /**
     * CORS configuration source bean
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        log.info("Configuring CORS with allowed origins: {}", corsAllowedOrigins);

        CorsConfiguration configuration = new CorsConfiguration();

        // Configure allowed origins
        if ("*".equals(corsAllowedOrigins)) {
            configuration.setAllowedOriginPatterns(List.of("*"));
        } else {
            configuration.setAllowedOrigins(Arrays.asList(corsAllowedOrigins.split(",")));
        }

        // Configure allowed methods
        configuration.setAllowedMethods(Arrays.asList(corsAllowedMethods.split(",")));

        // Configure allowed headers and credentials
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}