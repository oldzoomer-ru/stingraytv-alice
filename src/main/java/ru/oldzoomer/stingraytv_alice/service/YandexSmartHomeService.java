package ru.oldzoomer.stingraytv_alice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import ru.oldzoomer.stingraytv_alice.dto.yandex.UserUnlinkResponse;
import ru.oldzoomer.stingraytv_alice.dto.yandex.YandexSmartHomeRequest;
import ru.oldzoomer.stingraytv_alice.dto.yandex.YandexSmartHomeResponse;
import ru.oldzoomer.stingraytv_alice.enums.QueryTypes;
import ru.oldzoomer.stingraytv_alice.gateway.YandexSmartHomeGateway;

import java.util.Optional;

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
     * Requires authentication via Spring Security
     */
    @PreAuthorize("isAuthenticated()")
    public YandexSmartHomeResponse processUserDevicesRequest() {
        String userId = getCurrentUserId().orElse("unknown");
        log.info("Processing user devices discovery request from user: {}", userId);

        // Create a minimal request for discovery
        YandexSmartHomeRequest request = new YandexSmartHomeRequest();
        request.setRequestId("discovery-" + System.currentTimeMillis());

        // Pass user ID to gateway for inclusion in response
        return smartHomeGateway.processRequest(request, userId, QueryTypes.DEVICES_DiSCOVERY);
    }

    /**
     * Process device state query request
     * Requires authentication via Spring Security
     */
    @PreAuthorize("isAuthenticated()")
    public YandexSmartHomeResponse processDeviceQueryRequest(YandexSmartHomeRequest request) {
        return processAuthenticatedRequest(request, "device query", QueryTypes.DEVICES_QUERY);
    }

    /**
     * Process device action request
     * Requires authentication via Spring Security
     */
    @PreAuthorize("isAuthenticated()")
    public YandexSmartHomeResponse processDeviceActionRequest(YandexSmartHomeRequest request) {
        return processAuthenticatedRequest(request, "device action", QueryTypes.DEVICES_ACTION);
    }

    /**
     * Common method for processing authenticated requests
     * Assumes authentication is already handled by Spring Security
     */
    private YandexSmartHomeResponse processAuthenticatedRequest(YandexSmartHomeRequest request, String requestType, QueryTypes queryTypes) {
        String userId = getCurrentUserId().orElse("unknown");
        log.info("Processing {} request from user: {}, request_id: {}", requestType, userId, request.getRequestId());

        return smartHomeGateway.processRequest(request, userId, queryTypes);
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
                .errorCode("VALIDATION_ERROR")
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
     * Process user unlink request
     * This method handles account disconnection initiated by user
     * The user token should be revoked regardless of response correctness
     */
    @PreAuthorize("isAuthenticated()")
    public UserUnlinkResponse processUserUnlinkRequest() {
        String userId = getCurrentUserId().orElse("unknown");

        log.info("Processing user unlink request from user: {}", userId);

        // Log the unlink event for investigation
        log.warn("User {} initiated account unlink. Token should be revoked.", userId);

        // According to Yandex documentation, user token should be revoked
        // regardless of response correctness
        // In a real implementation, we would revoke the token here

        return UserUnlinkResponse.builder()
                .build();
    }

    public YandexSmartHomeResponse createMissingParameterErrorResponse() {
        return YandexSmartHomeResponse.builder()
                .status("error")
                .errorCode("MISSING_PARAMETER_ERROR")
                .errorMessage("missing parameter")
                .build();
    }
}
