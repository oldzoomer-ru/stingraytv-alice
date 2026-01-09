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
 * Service for handling Yandex Smart Home business logic.
 * This service manages authentication, request processing, and error handling
 * for integration with Yandex Smart Home API.
 * Uses Spring Security for authentication and authorization.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class YandexSmartHomeService {

    private final YandexSmartHomeGateway smartHomeGateway;

    /**
     * Processes user devices discovery request (GET without payload).
     * This method handles the initial device discovery request from Yandex Smart Home.
     *
     * @param requestId unique identifier for the request
     * @return YandexSmartHomeResponse with device discovery information
     */
    public YandexSmartHomeResponse processUserDevicesRequest(String requestId) {
        log.debug("Processing user devices discovery request with ID: {}", requestId);
        // Create a minimal request for discovery
        YandexSmartHomeRequest request = new YandexSmartHomeRequest();

        // Pass user ID to gateway for inclusion in response
        return processAuthenticatedRequest(request, requestId, "discovery request", QueryTypes.DEVICES_DISCOVERY);
    }

    /**
     * Processes device state query request.
     * This method handles requests to query current device states from Yandex Smart Home.
     *
     * @param request the device query request payload
     * @param requestId unique identifier for the request
     * @return YandexSmartHomeResponse with device state information
     */
    public YandexSmartHomeResponse processDeviceQueryRequest(YandexSmartHomeRequest request, String requestId) {
        log.debug("Processing device query request with ID: {}", requestId);
        return processAuthenticatedRequest(request, requestId, "device query", QueryTypes.DEVICES_QUERY);
    }

    /**
     * Processes device action request.
     * This method handles requests to execute device actions from Yandex Smart Home.
     *
     * @param request the device action request payload
     * @param requestId unique identifier for the request
     * @return YandexSmartHomeResponse with action execution results
     */
    public YandexSmartHomeResponse processDeviceActionRequest(YandexSmartHomeRequest request, String requestId) {
        log.debug("Processing device action request with ID: {}", requestId);
        return processAuthenticatedRequest(request, requestId, "device action", QueryTypes.DEVICES_ACTION);
    }

    /**
     * Processes user unlink request.
     * This method handles account disconnection initiated by user.
     * The user token should be revoked regardless of response correctness.
     *
     * @param requestId unique identifier for the request
     * @return UserUnlinkResponse indicating successful unlinking
     */
    public UserUnlinkResponse processUserUnlinkRequest(String requestId) {
        String userId = getCurrentUserId().orElse("unknown");
        log.info("Processing user unlink request from user: {}, request_id: {}", userId, requestId);

        return UserUnlinkResponse.builder()
                .build();
    }

    /**
     * Common method for processing authenticated requests.
     * This method handles authentication and delegates to the gateway for processing.
     *
     * @param request the request payload
     * @param requestId unique identifier for the request
     * @param requestType type of request being processed
     * @param queryTypes enum indicating the type of operation
     * @return YandexSmartHomeResponse with the processed result
     */
    private YandexSmartHomeResponse processAuthenticatedRequest(YandexSmartHomeRequest request, String requestId,
                                                                    String requestType, QueryTypes queryTypes) {
        String userId = getCurrentUserId().orElse("unknown");
        log.info("Processing {} request from user: {}, request_id: {}", requestType, userId, requestId);

        return smartHomeGateway.processRequest(request, requestId, userId, queryTypes);
    }

    /**
     * Gets current user ID from Spring Security context.
     * Extracts the user identifier from the JWT token in the security context.
     *
     * @return Optional containing user ID if authenticated, empty otherwise
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
     * Creates error response for validation errors.
     * This method generates a standardized error response for validation failures.
     *
     * @param message error message to include in response
     * @return YandexSmartHomeResponse with validation error status
     */
    public YandexSmartHomeResponse createValidationErrorResponse(String message) {
        return YandexSmartHomeResponse.builder()
                .status("error")
                .errorCode("INVALID_VALUE")
                .errorMessage(message)
                .build();
    }

    /**
     * Creates error response for internal errors.
     * This method generates a standardized error response for internal server errors.
     *
     * @param message error message to include in response
     * @return YandexSmartHomeResponse with internal error status
     */
    public YandexSmartHomeResponse createInternalErrorResponse(String message) {
        return YandexSmartHomeResponse.builder()
                .status("error")
                .errorCode("INTERNAL_ERROR")
                .errorMessage(message)
                .build();
    }

    /**
     * Creates error response for not found errors.
     * This method generates a standardized error response for resource not found errors.
     *
     * @return YandexSmartHomeResponse with not found error status
     */
    public YandexSmartHomeResponse createNotFoundResponse() {
        return YandexSmartHomeResponse.builder()
                .status("error")
                .errorCode("NOT_FOUND")
                .errorMessage("Not found")
                .build();
    }
}
