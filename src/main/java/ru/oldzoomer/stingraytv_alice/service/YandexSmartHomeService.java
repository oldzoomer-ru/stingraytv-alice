package ru.oldzoomer.stingraytv_alice.service;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.oldzoomer.stingraytv_alice.dto.yandex.UserUnlinkResponse;
import ru.oldzoomer.stingraytv_alice.dto.yandex.YandexSmartHomeRequest;
import ru.oldzoomer.stingraytv_alice.dto.yandex.YandexSmartHomeResponse;
import ru.oldzoomer.stingraytv_alice.enums.QueryTypes;
import ru.oldzoomer.stingraytv_alice.gateway.YandexSmartHomeGateway;

/**
 * Service for handling Yandex Smart Home business logic
 * Uses Spring Security for authentication and authorization
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class YandexSmartHomeService {

    private final YandexSmartHomeGateway smartHomeGateway;

    /**
     * Process user devices discovery request (GET without payload)
     */
    public YandexSmartHomeResponse processUserDevicesRequest(String requestId) {
        // Create a minimal request for discovery
        YandexSmartHomeRequest request = new YandexSmartHomeRequest();

        // Pass user ID to gateway for inclusion in response
        return processAuthenticatedRequest(request, requestId, "discovery request", QueryTypes.DEVICES_DISCOVERY);
    }

    /**
     * Process device state query request
     */
    public YandexSmartHomeResponse processDeviceQueryRequest(YandexSmartHomeRequest request, String requestId) {
        return processAuthenticatedRequest(request, requestId, "device query", QueryTypes.DEVICES_QUERY);
    }

    /**
     * Process device action request
     */
    public YandexSmartHomeResponse processDeviceActionRequest(YandexSmartHomeRequest request, String requestId) {
        return processAuthenticatedRequest(request, requestId, "device action", QueryTypes.DEVICES_ACTION);
    }

    /**
     * Process user unlink request
     * This method handles account disconnection initiated by user
     * The user token should be revoked regardless of response correctness
     */
    public UserUnlinkResponse processUserUnlinkRequest(String requestId) {
        String userId = getCurrentUserId().orElse("unknown");

        log.info("Processing user unlink request from user: {}, request_id: {}", userId);

        return UserUnlinkResponse.builder()
                .build();
    }

    /**
     * Common method for processing authenticated requests
     */
    private YandexSmartHomeResponse processAuthenticatedRequest(YandexSmartHomeRequest request, String requestId,
                                                                    String requestType, QueryTypes queryTypes) {
        String userId = getCurrentUserId().orElse("unknown");
        log.info("Processing {} request from user: {}, request_id: {}", requestType, userId, requestId);

        return smartHomeGateway.processRequest(request, requestId, userId, queryTypes);
    }

    /**
     * Get current user ID from Spring Security context
     */
    private Optional<String> getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            return Optional.ofNullable(jwt.getSubject());
        }
        return Optional.empty();
    }

    /**
     * Create error response for validation errors
     */
    public YandexSmartHomeResponse createValidationErrorResponse(String message) {
        return YandexSmartHomeResponse.builder()
                .status("error")
                .errorCode("INVALID_VALUE")
                .errorMessage(message)
                .build();
    }

    /**
     * Create error response for internal errors
     */
    public YandexSmartHomeResponse createInternalErrorResponse(String message) {
        return YandexSmartHomeResponse.builder()
                .status("error")
                .errorCode("INTERNAL_ERROR")
                .errorMessage(message)
                .build();
    }

    /**
     * Create error response for internal errors
     */
    public YandexSmartHomeResponse createNotFoundResponse() {
        return YandexSmartHomeResponse.builder()
                .status("error")
                .errorCode("NOT_FOUND")
                .errorMessage("Not found")
                .build();
    }
}
