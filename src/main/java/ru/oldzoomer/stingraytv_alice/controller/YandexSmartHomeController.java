package ru.oldzoomer.stingraytv_alice.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.oldzoomer.stingraytv_alice.dto.yandex.UserUnlinkResponse;
import ru.oldzoomer.stingraytv_alice.dto.yandex.YandexSmartHomeRequest;
import ru.oldzoomer.stingraytv_alice.dto.yandex.YandexSmartHomeResponse;
import ru.oldzoomer.stingraytv_alice.service.YandexSmartHomeService;

/**
 * Controller for handling Yandex Smart Home API requests.
 * This controller manages all endpoints for device discovery, state queries,
 * and device actions for integration with Yandex Smart Home.
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1.0")
public class YandexSmartHomeController {

    private final YandexSmartHomeService smartHomeService;

    /**
     * Handles user device discovery request (GET).
     * This endpoint is called by Yandex Smart Home to discover available devices.
     *
     * @param requestId unique identifier for the request
     * @return ResponseEntity with device discovery response
     */
    @GetMapping("/user/devices")
    public ResponseEntity<YandexSmartHomeResponse> getUserDevices(@RequestHeader("X-Request-Id") String requestId) {
        log.debug("Processing device discovery request with ID: {}", requestId);
        YandexSmartHomeResponse response = smartHomeService.processUserDevicesRequest(requestId);
        return ResponseEntity.ok(response);
    }

    /**
     * Handles device state query request.
     * This endpoint is called by Yandex Smart Home to query current device states.
     *
     * @param request the device query request payload
     * @param requestId unique identifier for the request
     * @return ResponseEntity with device state query response
     */
    @PostMapping("/user/devices/query")
    public ResponseEntity<YandexSmartHomeResponse> queryDeviceStates(
            @Valid @RequestBody YandexSmartHomeRequest request,
            @RequestHeader("X-Request-Id") String requestId) {
        log.debug("Processing device query request with ID: {}", requestId);
        YandexSmartHomeResponse response = smartHomeService.processDeviceQueryRequest(request, requestId);
        return ResponseEntity.ok(response);
    }

    /**
     * Handles device action request.
     * This endpoint is called by Yandex Smart Home to execute device actions.
     *
     * @param request the device action request payload
     * @param requestId unique identifier for the request
     * @return ResponseEntity with device action response
     */
    @PostMapping("/user/devices/action")
    public ResponseEntity<YandexSmartHomeResponse> executeDeviceAction(
            @Valid @RequestBody YandexSmartHomeRequest request,
            @RequestHeader("X-Request-Id") String requestId) {
        log.debug("Processing device action request with ID: {}", requestId);
        YandexSmartHomeResponse response = smartHomeService.processDeviceActionRequest(request, requestId);
        return ResponseEntity.ok(response);
    }

    /**
     * Handles user unlink request.
     * Notifies about account disconnection between provider and Yandex.
     * User token should be revoked regardless of response correctness.
     *
     * @param requestId unique identifier for the request
     * @return ResponseEntity with user unlink response
     */
    @PostMapping("/user/unlink")
    public ResponseEntity<UserUnlinkResponse> unlinkUser(
            @RequestHeader("X-Request-Id") String requestId) {
        log.debug("Processing user unlink request with ID: {}", requestId);
        UserUnlinkResponse response = smartHomeService.processUserUnlinkRequest(requestId);
        return ResponseEntity.ok(response);
    }

    /**
     * Handles NoResourceFoundException.
     * Returns a 200 OK response with a NOT_FOUND error status for unknown endpoints.
     *
     * @param ex the exception thrown
     * @param request the web request
     * @return ResponseEntity with error response
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<YandexSmartHomeResponse> handleResourceNotFoundException(NoResourceFoundException ex, WebRequest request) {
        log.warn("Page is not found: {}", ex.getResourcePath());

        YandexSmartHomeResponse response = smartHomeService.createNotFoundResponse();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Handles MissingServletRequestParameterException.
     * Returns a 200 OK response with validation error status for missing parameters.
     *
     * @param ex the exception thrown
     * @return ResponseEntity with error response
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<YandexSmartHomeResponse> handleMissingParameters(
            MissingServletRequestParameterException ex) {
        log.warn("Missing parameter: {}", ex.getMessage());

        YandexSmartHomeResponse response = smartHomeService.createValidationErrorResponse("Missing parameters");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Global exception handler for validation errors.
     * Handles validation errors from method arguments and request body validation.
     *
     * @param ex the validation exception thrown
     * @return ResponseEntity with validation error response
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<YandexSmartHomeResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        log.warn("Validation error in request: {}", ex.getMessage());
        log.debug("Validation errors: {}", ex.getBindingResult().getFieldErrors());

        YandexSmartHomeResponse response = smartHomeService.createValidationErrorResponse("Validation error");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Global exception handler for JSON parsing errors.
     * Handles cases where the request body is not valid JSON.
     *
     * @param ex the JSON parsing exception thrown
     * @return ResponseEntity with validation error response
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<YandexSmartHomeResponse> handleJsonParseException(HttpMessageNotReadableException ex) {
        log.warn("JSON parse error in request: {}", ex.getMessage());

        YandexSmartHomeResponse response = smartHomeService.createValidationErrorResponse("Invalid JSON");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Global exception handler for other exceptions.
     * Handles unexpected errors that are not caught by more specific handlers.
     *
     * @param ex the unexpected exception thrown
     * @return ResponseEntity with internal error response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<YandexSmartHomeResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error processing request", ex);

        YandexSmartHomeResponse response = smartHomeService.createInternalErrorResponse("Internal error");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
