package ru.oldzoomer.stingraytv_alice.service;

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

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        YandexSmartHomeRequest request = createTestRequest();
        YandexSmartHomeResponse expectedResponse = createTestResponse();

        setupAuthenticatedUser();
        when(smartHomeGateway.processRequest(request, "test-user", QueryTypes.DEVICES_QUERY)).thenReturn(expectedResponse);

        // When
        YandexSmartHomeResponse result = smartHomeService.processDeviceQueryRequest(request);

        // Then
        assertThat(result).isEqualTo(expectedResponse);
        verify(smartHomeGateway).processRequest(request, "test-user", QueryTypes.DEVICES_QUERY);
    }

    @Test
    void processDeviceActionRequest_WhenAuthenticated_ShouldReturnResponse() {
        // Given
        YandexSmartHomeRequest request = createTestRequest();
        YandexSmartHomeResponse expectedResponse = createTestResponse();

        setupAuthenticatedUser();
        when(smartHomeGateway.processRequest(request, "test-user", QueryTypes.DEVICES_ACTION)).thenReturn(expectedResponse);

        // When
        YandexSmartHomeResponse result = smartHomeService.processDeviceActionRequest(request);

        // Then
        assertThat(result).isEqualTo(expectedResponse);
        verify(smartHomeGateway).processRequest(request, "test-user", QueryTypes.DEVICES_ACTION);
    }

    @Test
    void processUserDevicesRequest_WhenAuthenticated_ShouldReturnResponse() {
        // Given
        YandexSmartHomeResponse expectedResponse = createTestResponse();

        setupAuthenticatedUser();
        when(smartHomeGateway.processRequest(org.mockito.ArgumentMatchers.any(YandexSmartHomeRequest.class), org.mockito.ArgumentMatchers.eq("test-user"), org.mockito.ArgumentMatchers.any(QueryTypes.class)))
                .thenReturn(expectedResponse);

        // When
        YandexSmartHomeResponse result = smartHomeService.processUserDevicesRequest();

        // Then
        assertThat(result).isEqualTo(expectedResponse);
        verify(smartHomeGateway).processRequest(org.mockito.ArgumentMatchers.any(YandexSmartHomeRequest.class), org.mockito.ArgumentMatchers.eq("test-user"), org.mockito.ArgumentMatchers.any(QueryTypes.class));
    }

    @Test
    void createValidationErrorResponse_ShouldReturnCorrectResponse() {
        // Given
        String requestId = "test-request-id";

        // When
        YandexSmartHomeResponse response = smartHomeService.createValidationErrorResponse(requestId);

        // Then
        assertThat(response.getRequestId()).isEqualTo(requestId);
        assertThat(response.getStatus()).isEqualTo("error");
        assertThat(response.getErrorCode()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getErrorMessage()).isEqualTo("Invalid request parameters");
    }

    @Test
    void createInternalErrorResponse_ShouldReturnCorrectResponse() {
        // Given
        String requestId = "test-request-id";

        // When
        YandexSmartHomeResponse response = smartHomeService.createInternalErrorResponse(requestId);

        // Then
        assertThat(response.getRequestId()).isEqualTo(requestId);
        assertThat(response.getStatus()).isEqualTo("error");
        assertThat(response.getErrorCode()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getErrorMessage()).isEqualTo("Internal server error");
    }

    @Test
    void processUserUnlinkRequest_WhenAuthenticated_ShouldReturnResponse() {
        // Given
        String requestId = "unlink-request-id";
        setupAuthenticatedUser();

        // When
        UserUnlinkResponse response = smartHomeService.processUserUnlinkRequest(requestId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getRequestId()).isEqualTo(requestId);
    }

    @Test
    void processUserUnlinkRequest_WhenNotAuthenticated_ShouldReturnResponse() {
        // Given
        String requestId = "unlink-request-id";
        SecurityContextHolder.clearContext();

        // When
        UserUnlinkResponse response = smartHomeService.processUserUnlinkRequest(requestId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getRequestId()).isEqualTo(requestId);
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

    private YandexSmartHomeRequest createTestRequest() {
        YandexSmartHomeRequest request = new YandexSmartHomeRequest();
        request.setRequestId("test-request-id");
        return request;
    }

    private YandexSmartHomeResponse createTestResponse() {
        return YandexSmartHomeResponse.builder()
                .requestId("test-request-id")
                .status("success")
                .build();
    }
}