package ru.oldzoomer.stingraytv_alice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

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

    /**
     * Health check endpoint
     */
    @RequestMapping(value = "", method = RequestMethod.HEAD)
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("OK");
    }
}
