package ru.oldzoomer.stingraytv_alice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
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
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class YandexSmartHomeController {

    private final YandexSmartHomeService smartHomeService;

    /**
     * Handle user device discovery request (GET)
     */
    @GetMapping("/user/devices")
    public ResponseEntity<YandexSmartHomeResponse> getUserDevices() {
        YandexSmartHomeResponse response = smartHomeService.processUserDevicesRequest();
        return ResponseEntity.ok(response);
    }

    /**
     * Handle device state query request
     */
    @PostMapping("/user/devices/query")
    public ResponseEntity<YandexSmartHomeResponse> queryDeviceStates(
            @Valid @RequestBody YandexSmartHomeRequest request) {

        YandexSmartHomeResponse response = smartHomeService.processDeviceQueryRequest(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Handle device action request
     */
    @PostMapping("/user/devices/action")
    public ResponseEntity<YandexSmartHomeResponse> executeDeviceAction(
            @Valid @RequestBody YandexSmartHomeRequest request) {

        YandexSmartHomeResponse response = smartHomeService.processDeviceActionRequest(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Handle user unlink request
     * Notifies about account disconnection between provider and Yandex
     * User token should be revoked regardless of response correctness
     */
    @PostMapping("/user/unlink")
    public ResponseEntity<UserUnlinkResponse> unlinkUser(
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {

        if (requestId == null || requestId.trim().isEmpty()) {
            log.warn("User unlink request received without X-Request-Id header");
            return ResponseEntity.badRequest().build();
        }

        log.info("Processing user unlink request with X-Request-Id: {}", requestId);

        UserUnlinkResponse response = smartHomeService.processUserUnlinkRequest();
        return ResponseEntity.ok(response);
    }

    /**
     * Health check endpoint
     */
    @RequestMapping(value = "", method = RequestMethod.HEAD)
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("OK");
    }
}
