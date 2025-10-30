package ru.oldzoomer.stingraytv_alice.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import ru.oldzoomer.stingraytv_alice.controller.YandexOAuthController;
import ru.oldzoomer.stingraytv_alice.service.TokenService;
import ru.oldzoomer.stingraytv_alice.service.YandexSmartHomeService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for SecurityConfig
 */
@WebMvcTest
@Import({
        SecurityConfig.class,
        CorsConfig.class,
        YandexOAuthController.class,
        TokenService.class,
        OAuthProperties.class
})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private YandexSmartHomeService yandexSmartHomeService;

    @Test
    void publicEndpoints_ShouldBeAccessibleWithoutAuthentication() throws Exception {
        // OAuth authorize requires parameters, so it returns 400 (Bad Request) instead of 401 (Unauthorized)
        mockMvc.perform(get("/oauth/authorize"))
                .andExpect(status().is4xxClientError());

        // /login endpoint is now handled by YandexOAuthController
        mockMvc.perform(get("/oauth/authorize")
                        .param("response_type", "code")
                        .param("client_id", "yandex-alice")
                        .param("redirect_uri", "https://social.yandex.net/broker/redirect")
                        .param("state", "test-state"))
                .andExpect(status().isOk());

        mockMvc.perform(MockMvcRequestBuilders.head("/v1.0"))
                .andExpect(status().isOk());
    }

    @Test
    void protectedApiEndpoints_ShouldRequireAuthentication() throws Exception {
        // Without authentication, endpoint returns 401 (Unauthorized)
        mockMvc.perform(get("/v1.0/user/devices"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void protectedApiEndpoints_ShouldBeAccessibleWithAuthentication() throws Exception {
        // With authentication, endpoint is accessible
        mockMvc.perform(get("/v1.0/user/devices"))
                .andExpect(status().isOk());
    }

    @Test
    void corsConfiguration_ShouldAllowConfiguredOrigins() throws Exception {
        // Test CORS headers on public endpoint /oauth/authorize with required parameters
        mockMvc.perform(get("/oauth/authorize")
                        .header("Origin", "http://localhost:3000")
                        .param("response_type", "code")
                        .param("client_id", "yandex-alice")
                        .param("redirect_uri", "https://social.yandex.net/broker/redirect")
                        .param("state", "test-state"))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.header().string("Access-Control-Allow-Origin", "http://localhost:3000"));

        mockMvc.perform(get("/oauth/authorize")
                        .header("Origin", "https://example.com")
                        .param("response_type", "code")
                        .param("client_id", "yandex-alice")
                        .param("redirect_uri", "https://social.yandex.net/broker/redirect")
                        .param("state", "test-state"))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.header().string("Access-Control-Allow-Origin", "https://example.com"));
    }
}