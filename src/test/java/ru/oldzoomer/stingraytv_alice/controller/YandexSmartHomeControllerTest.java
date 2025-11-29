package ru.oldzoomer.stingraytv_alice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import ru.oldzoomer.stingraytv_alice.config.SecurityConfig;
import ru.oldzoomer.stingraytv_alice.dto.yandex.UserUnlinkResponse;
import ru.oldzoomer.stingraytv_alice.dto.yandex.YandexSmartHomeRequest;
import ru.oldzoomer.stingraytv_alice.dto.yandex.YandexSmartHomeResponse;
import ru.oldzoomer.stingraytv_alice.service.YandexSmartHomeService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@Import({SecurityConfig.class})
class YandexSmartHomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private YandexSmartHomeService smartHomeService;

    @Test
    void getUserDevices_ValidRequest_ReturnsOk() throws Exception {
        // Arrange
        YandexSmartHomeResponse response = YandexSmartHomeResponse.builder()
                .requestId("discovery-test-id")
                .status("ok")
                .build();

        when(smartHomeService.processUserDevicesRequest()).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/v1.0/user/devices")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject("test-user"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void queryDeviceStates_ValidRequest_ReturnsOk() throws Exception {
        // Arrange
        YandexSmartHomeRequest request = new YandexSmartHomeRequest();
        request.setRequestId("query-request-id");

        YandexSmartHomeResponse response = YandexSmartHomeResponse.builder()
                .requestId("query-request-id")
                .status("ok")
                .build();

        when(smartHomeService.processDeviceQueryRequest(any(YandexSmartHomeRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/v1.0/user/devices/query")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject("test-user")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.request_id").value("query-request-id"))
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void executeDeviceAction_ValidRequest_ReturnsOk() throws Exception {
        // Arrange
        YandexSmartHomeRequest request = new YandexSmartHomeRequest();
        request.setRequestId("action-request-id");

        YandexSmartHomeResponse response = YandexSmartHomeResponse.builder()
                .requestId("action-request-id")
                .status("ok")
                .build();

        when(smartHomeService.processDeviceActionRequest(any(YandexSmartHomeRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/v1.0/user/devices/action")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject("test-user")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.request_id").value("action-request-id"))
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void healthCheck_ReturnsOk() throws Exception {
        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.head("/v1.0"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void unlinkUser_ValidRequest_ReturnsOk() throws Exception {
        // Arrange
        String requestId = "unlink-request-id-123";
        UserUnlinkResponse response = UserUnlinkResponse.builder()
                .requestId(requestId)
                .build();

        when(smartHomeService.processUserUnlinkRequest()).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/v1.0/user/unlink")
                        .with(SecurityMockMvcRequestPostProcessors.jwt())
                        .header("X-Request-Id", requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.request_id").value(requestId));
    }

    @Test
    void unlinkUser_WithoutAuth_ReturnsUnauthorized() throws Exception {
        // Arrange
        String requestId = "unlink-request-id-123";

        // Act & Assert
        mockMvc.perform(post("/v1.0/user/unlink")
                        .header("X-Request-Id", requestId))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void unlinkUser_WithoutXRequestId_ReturnsBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/v1.0/user/unlink")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void queryDeviceStates_InvalidJson_ReturnsBadRequest() throws Exception {
        // Arrange
        String invalidJson = "{\"request_id\": \"test-id\", \"payload\": {\"devices\": [{\"id\": \"device1\", \"capabilities\": [invalid]}]}}";

        // Act & Assert
        mockMvc.perform(post("/v1.0/user/devices/query")
                        .with(SecurityMockMvcRequestPostProcessors.jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

}
