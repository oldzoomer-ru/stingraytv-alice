package ru.oldzoomer.stingraytv_alice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.oldzoomer.stingraytv_alice.config.OAuthProperties;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for OAuth 2.0 functionality in TokenService
 */
@ExtendWith(MockitoExtension.class)
class TokenServiceOAuthTest {

    @Mock
    private OAuthProperties oauthProperties;

    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService(oauthProperties);
        tokenService.setJwtSecret("test-secret-key-for-oauth-test-which-is-long-enough-for-jwt");
        tokenService.setExpiresIn(3600);
        tokenService.init();
    }

    @Test
    void createCode_ShouldGenerateValidJWT() {
        // When
        String code = tokenService.createCode("test-user", "smart-home");

        // Then
        assertThat(code).isNotNull().isNotEmpty();
        assertThat(code).contains("."); // JWT format
    }

    @Test
    void consumeCode_WithValidCode_ShouldReturnTokens() {
        // Given
        String code = tokenService.createCode("test-user", "smart-home");

        // When
        Optional<Map<String, Object>> result = tokenService.consumeCode(code);

        // Then
        assertThat(result).isPresent();
        Map<String, Object> tokens = result.get();
        assertThat(tokens).containsKeys("access_token", "refresh_token", "expires_in", "token_type", "scope", "user_id");
        assertThat(tokens.get("token_type")).isEqualTo("Bearer");
        assertThat(tokens.get("user_id")).isEqualTo("test-user");
        assertThat(tokens.get("scope")).isEqualTo("smart-home");
    }

    @Test
    void consumeCode_WithInvalidCode_ShouldReturnEmpty() {
        // When
        Optional<Map<String, Object>> result = tokenService.consumeCode("invalid-code");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void refresh_WithValidRefreshToken_ShouldReturnNewAccessToken() {
        // Given
        String code = tokenService.createCode("test-user", "smart-home");
        Optional<Map<String, Object>> tokens = tokenService.consumeCode(code);
        String refreshToken = (String) tokens.get().get("refresh_token");

        // When
        Optional<Map<String, Object>> result = tokenService.refresh(refreshToken);

        // Then
        assertThat(result).isPresent();
        Map<String, Object> newTokens = result.get();
        assertThat(newTokens).containsKeys("access_token", "expires_in", "token_type", "scope", "user_id");
        assertThat(newTokens.get("token_type")).isEqualTo("Bearer");
        assertThat(newTokens.get("user_id")).isEqualTo("test-user");
        assertThat(newTokens.get("scope")).isEqualTo("smart-home");
    }

    @Test
    void refresh_WithInvalidRefreshToken_ShouldReturnEmpty() {
        // When
        Optional<Map<String, Object>> result = tokenService.refresh("invalid-refresh-token");

        // Then
        assertThat(result).isEmpty();
    }
}