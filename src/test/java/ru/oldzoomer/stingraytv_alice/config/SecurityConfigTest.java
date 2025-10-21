package ru.oldzoomer.stingraytv_alice.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for SecurityConfig
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "jwt.secret=test-secret-key-for-security-config-test-1234567890",
        "cors.allowed-origins=http://localhost:3000,https://example.com",
        "cors.allowed-methods=GET,POST,OPTIONS"
})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicEndpoints_ShouldBeAccessibleWithoutAuthentication() throws Exception {
        // OAuth authorize requires parameters, so it returns 400 (Bad Request) instead of 401 (Unauthorized)
        mockMvc.perform(get("/oauth/authorize"))
                .andExpect(status().is4xxClientError());

        // /login endpoint is now handled by YandexOAuthController
        mockMvc.perform(get("/oauth/authorize")
                        .param("response_type", "code")
                        .param("client_id", "test-client")
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
                        .param("client_id", "test-client")
                        .param("state", "test-state"))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.header().string("Access-Control-Allow-Origin", "http://localhost:3000"));

        mockMvc.perform(get("/oauth/authorize")
                        .header("Origin", "https://example.com")
                        .param("response_type", "code")
                        .param("client_id", "test-client")
                        .param("state", "test-state"))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.header().string("Access-Control-Allow-Origin", "https://example.com"));
    }
}