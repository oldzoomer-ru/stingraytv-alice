package ru.oldzoomer.stingraytv_alice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import ru.oldzoomer.stingraytv_alice.dto.yandex.UserUnlinkResponse;
import ru.oldzoomer.stingraytv_alice.dto.yandex.YandexSmartHomeRequest;
import ru.oldzoomer.stingraytv_alice.dto.yandex.YandexSmartHomeResponse;
import ru.oldzoomer.stingraytv_alice.enums.QueryTypes;
import ru.oldzoomer.stingraytv_alice.gateway.YandexSmartHomeGateway;

/**
 * Unit tests for YandexSmartHomeService
 */
@ExtendWith(MockitoExtension.class)
class YandexSmartHomeServiceTest {

    @Mock
    private YandexSmartHomeGateway smartHomeGateway;

    @InjectMocks
    private YandexSmartHomeService smartHomeService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void processDeviceQueryRequest_WhenAuthenticated_ShouldReturnResponse() {
        // Given
        YandexSmartHomeRequest request = new YandexSmartHomeRequest();
        YandexSmartHomeResponse expectedResponse = createTestResponse();
        String requestId = "test-request-id";

        setupAuthenticatedUser();
        when(smartHomeGateway.processRequest(request, requestId, "test-user", QueryTypes.DEVICES_QUERY)).thenReturn(expectedResponse);

        // When
        YandexSmartHomeResponse result = smartHomeService.processDeviceQueryRequest(request, requestId);

        // Then
        assertThat(result).isEqualTo(expectedResponse);
        verify(smartHomeGateway).processRequest(request, requestId, "test-user", QueryTypes.DEVICES_QUERY);
    }

    @Test
    void processDeviceActionRequest_WhenAuthenticated_ShouldReturnResponse() {
        // Given
        YandexSmartHomeRequest request = new YandexSmartHomeRequest();
        YandexSmartHomeResponse expectedResponse = createTestResponse();
        String requestId = "test-request-id";

        setupAuthenticatedUser();
        when(smartHomeGateway.processRequest(request, requestId, "test-user", QueryTypes.DEVICES_ACTION)).thenReturn(expectedResponse);

        // When
        YandexSmartHomeResponse result = smartHomeService.processDeviceActionRequest(request, requestId);

        // Then
        assertThat(result).isEqualTo(expectedResponse);
        verify(smartHomeGateway).processRequest(request, requestId, "test-user", QueryTypes.DEVICES_ACTION);
    }

    @Test
    void processUserDevicesRequest_WhenAuthenticated_ShouldReturnResponse() {
        // Given
        YandexSmartHomeResponse expectedResponse = createTestResponse();
        String requestId = "test-request-id";

        setupAuthenticatedUser();
        when(smartHomeGateway.processRequest(any(YandexSmartHomeRequest.class), eq(requestId), eq("test-user"), any(QueryTypes.class)))
                .thenReturn(expectedResponse);

        // When
        YandexSmartHomeResponse result = smartHomeService.processUserDevicesRequest(requestId);

        // Then
        assertThat(result).isEqualTo(expectedResponse);
        verify(smartHomeGateway).processRequest(any(YandexSmartHomeRequest.class), eq(requestId), eq("test-user"), any(QueryTypes.class));
    }

    @Test
    void createValidationErrorResponse_ShouldReturnCorrectResponse() {
        // When
        YandexSmartHomeResponse response = smartHomeService.createValidationErrorResponse("Invalid request parameters");

        // Then
        assertThat(response.getStatus()).isEqualTo("error");
        assertThat(response.getErrorCode()).isEqualTo("INVALID_VALUE");
        assertThat(response.getErrorMessage()).isEqualTo("Invalid request parameters");
    }

    @Test
    void createInternalErrorResponse_ShouldReturnCorrectResponse() {
        // When
        YandexSmartHomeResponse response = smartHomeService.createInternalErrorResponse("Internal server error");

        // Then
        assertThat(response.getStatus()).isEqualTo("error");
        assertThat(response.getErrorCode()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getErrorMessage()).isEqualTo("Internal server error");
    }

    @Test
    void processUserUnlinkRequest_WhenAuthenticated_ShouldReturnResponse() {
        // Given
        setupAuthenticatedUser();
        String requestId = "test-request-id";

        // When
        UserUnlinkResponse response = smartHomeService.processUserUnlinkRequest(requestId);

        // Then
        assertThat(response).isNotNull();
    }

    @Test
    void processUserUnlinkRequest_WhenNotAuthenticated_ShouldReturnResponse() {
        // Given
        SecurityContextHolder.clearContext();
        String requestId = "test-request-id";

        // When
        UserUnlinkResponse response = smartHomeService.processUserUnlinkRequest(requestId);

        // Then
        assertThat(response).isNotNull();
    }

    private void setupAuthenticatedUser() {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .claim("sub", "test-user")
                .claim("scope", "read write")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private YandexSmartHomeResponse createTestResponse() {
        return YandexSmartHomeResponse.builder()
                .requestId("test-request-id")
                .status("success")
                .build();
    }
}