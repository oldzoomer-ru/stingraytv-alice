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
 * Controller for handling Yandex Smart Home API requests
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1.0")
public class YandexSmartHomeController {

    private final YandexSmartHomeService smartHomeService;

    /**
     * Handle user device discovery request (GET)
     */
    @GetMapping("/user/devices")
    public ResponseEntity<YandexSmartHomeResponse> getUserDevices(@RequestHeader("X-Request-Id") String requestId) {
        YandexSmartHomeResponse response = smartHomeService.processUserDevicesRequest(requestId);
        return ResponseEntity.ok(response);
    }

    /**
     * Handle device state query request
     */
    @PostMapping("/user/devices/query")
    public ResponseEntity<YandexSmartHomeResponse> queryDeviceStates(
            @Valid @RequestBody YandexSmartHomeRequest request,
            @RequestHeader("X-Request-Id") String requestId) {

        YandexSmartHomeResponse response = smartHomeService.processDeviceQueryRequest(request, requestId);
        return ResponseEntity.ok(response);
    }

    /**
     * Handle device action request
     */
    @PostMapping("/user/devices/action")
    public ResponseEntity<YandexSmartHomeResponse> executeDeviceAction(
            @Valid @RequestBody YandexSmartHomeRequest request,
            @RequestHeader("X-Request-Id") String requestId) {

        YandexSmartHomeResponse response = smartHomeService.processDeviceActionRequest(request, requestId);
        return ResponseEntity.ok(response);
    }

    /**
     * Handle user unlink request
     * Notifies about account disconnection between provider and Yandex
     * User token should be revoked regardless of response correctness
     */
    @PostMapping("/user/unlink")
    public ResponseEntity<UserUnlinkResponse> unlinkUser(
            @RequestHeader("X-Request-Id") String requestId) {
        UserUnlinkResponse response = smartHomeService.processUserUnlinkRequest(requestId);
        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<YandexSmartHomeResponse> handleResourceNotFoundException(NoResourceFoundException ex, WebRequest request) {
        log.warn("Page is not found: {}", ex.getResourcePath());

        YandexSmartHomeResponse response = smartHomeService.createNotFoundResponse();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<YandexSmartHomeResponse> handleMissingParameters(
            MissingServletRequestParameterException ex) {
        log.warn("Missing parameter: {}", ex.getMessage());

        YandexSmartHomeResponse response = smartHomeService.createValidationErrorResponse("Missing parameters");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Global exception handler for validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<YandexSmartHomeResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        log.warn("Validation error: {}", ex.getMessage());

        YandexSmartHomeResponse response = smartHomeService.createValidationErrorResponse("Validation error");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Global exception handler for JSON parsing errors
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<YandexSmartHomeResponse> handleJsonParseException(HttpMessageNotReadableException ex) {
        log.warn("JSON parse error: {}", ex.getMessage());

        YandexSmartHomeResponse response = smartHomeService.createValidationErrorResponse("Invalid JSON");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Global exception handler for other exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<YandexSmartHomeResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);

        YandexSmartHomeResponse response = smartHomeService.createInternalErrorResponse("Internal error");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
