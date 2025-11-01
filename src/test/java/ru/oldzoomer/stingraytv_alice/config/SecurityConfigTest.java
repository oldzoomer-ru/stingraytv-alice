package ru.oldzoomer.stingraytv_alice.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.oldzoomer.stingraytv_alice.service.ClientCredentialsService;
import ru.oldzoomer.stingraytv_alice.service.PreferencesStorageService;
import ru.oldzoomer.stingraytv_alice.service.YandexSmartHomeService;
import ru.oldzoomer.stingraytv_alice.controller.YandexOAuthController;
import ru.oldzoomer.stingraytv_alice.service.TokenService;
import ru.oldzoomer.stingraytv_alice.service.YandexSmartHomeService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for SecurityConfig
 */
@WebMvcTest
@Import({SecurityConfig.class,
        AuthorizationServerConfig.class,
        OAuthProperties.class,
        ClientCredentialsService.class,
        ClientTokenProperties.class})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PreferencesStorageService preferencesStorageService;

    @MockitoBean
    private YandexSmartHomeService yandexSmartHomeService;

    @Test
    void publicEndpoints_ShouldBeAccessibleWithoutAuthentication() throws Exception {
        // OAuth authorize endpoint - OAuth2 server handles this, returns 400 for missing parameters
        mockMvc.perform(get("/oauth/authorize"))
                .andExpect(status().is4xxClientError());

        // Health check endpoint
        mockMvc.perform(head("/v1.0"))
                .andExpect(status().isOk());
    }

    @Test
    void protectedApiEndpoints_ShouldRequireAuthentication() throws Exception {
        // Without authentication, endpoint should not be accessible
        // It may return 401 (Unauthorized) or 500 if there's an internal error
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
}