package ru.oldzoomer.stingraytv_alice.gateway;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.oldzoomer.stingraytv_alice.config.StingrayProperties;
import ru.oldzoomer.stingraytv_alice.dto.yandex.YandexSmartHomeRequest;
import ru.oldzoomer.stingraytv_alice.dto.yandex.YandexSmartHomeResponse;
import ru.oldzoomer.stingraytv_alice.enums.QueryTypes;
import ru.oldzoomer.stingraytv_alice.service.StingrayTVService;

import java.util.List;
import java.util.Map;

/**
 * Main gateway for Yandex Smart Home integration with StingrayTV API
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class YandexSmartHomeGateway {

    private final StingrayProperties stingrayProperties;
    private final StingrayTVService stingrayTVService;

    /**
     * Process Yandex Smart Home request with user ID and return response
     */
    public YandexSmartHomeResponse processRequest(YandexSmartHomeRequest request, String userId, QueryTypes type) {
        log.info("Processing Yandex Smart Home request: {}, user: {}", request.getRequestId(), userId);

        try {
            // Handle query and action requests (with devices in payload)
            return handleDevicesRequest(request, userId, type);
        } catch (Exception e) {
            log.error("Error processing Yandex Smart Home request: {}", request.getRequestId(), e);
            return createErrorResponse(request.getRequestId(), "Internal server error");
        }
    }

    private YandexSmartHomeResponse handleDevicesRequest(YandexSmartHomeRequest request, String userId, QueryTypes type) {
        String requestId = request.getRequestId();

        // Determine request type based on payload structure
        if (type == QueryTypes.DEVICES_QUERY) {
            return handleQueryRequest(requestId, userId);
        } else if (type == QueryTypes.DEVICES_ACTION) {
            return handleActionRequest(request, userId);
        } else {
            // Default to discovery if no specific type detected
            return handleDiscoveryRequest(requestId, userId);
        }
    }

    private YandexSmartHomeResponse handleDiscoveryRequest(String requestId, String userId) {
        log.info("Handling device discovery request for user: {}", userId);

        YandexSmartHomeResponse.Payload.Device device = YandexSmartHomeResponse.Payload.Device.builder()
                .id(stingrayProperties.getDeviceId())
                .name(stingrayProperties.getDeviceName())
                .description(stingrayProperties.getDeviceDescription())
                .room(stingrayProperties.getRoom())
                .type("devices.types.media_device.receiver")
                .capabilities(createDeviceCapabilities())
                .build();

        YandexSmartHomeResponse.Payload payload = YandexSmartHomeResponse.Payload.builder()
                .userId(userId)
                .devices(List.of(device))
                .build();

        return YandexSmartHomeResponse.builder()
                .requestId(requestId)
                .status("ok")
                .payload(payload)
                .build();
    }

    private YandexSmartHomeResponse handleQueryRequest(String requestId, String userId) {
        log.info("Handling device query request for user: {}", userId);

        try {
            YandexSmartHomeResponse.Payload.Device device = YandexSmartHomeResponse.Payload.Device.builder()
                    .id(stingrayProperties.getDeviceId())
                    .capabilities(createCurrentCapabilityStates())
                    .build();

            YandexSmartHomeResponse.Payload payload = YandexSmartHomeResponse.Payload.builder()
                    .userId(userId)
                    .devices(List.of(device))
                    .build();

            return YandexSmartHomeResponse.builder()
                    .requestId(requestId)
                    .status("ok")
                    .payload(payload)
                    .build();

        } catch (Exception e) {
            log.error("Error handling query request", e);
            return createErrorResponse(requestId, "Failed to query device state");
        }
    }

    private YandexSmartHomeResponse handleActionRequest(YandexSmartHomeRequest request, String userId) {
        log.info("Handling device action request for user: {}", userId);
        String requestId = request.getRequestId();

        try {
            if (request.getPayload().getDevices() == null || request.getPayload().getDevices().isEmpty()) {
                return createErrorResponse(requestId, "No devices specified in action request");
            }

            // Process actions for each device
            for (YandexSmartHomeRequest.Payload.Device device : request.getPayload().getDevices()) {
                if (stingrayProperties.getDeviceId().equals(device.getId())) {
                    return processDeviceActions(device, requestId, userId);
                }
            }

            return createErrorResponse(requestId, "Device not found");

        } catch (Exception e) {
            log.error("Error handling action request", e);
            return createErrorResponse(requestId, "Failed to execute device action");
        }
    }

    private YandexSmartHomeResponse processDeviceActions(YandexSmartHomeRequest.Payload.Device device, String requestId, String userId) {
        boolean allActionsSuccessful = true;

        if (device.getCapabilities() != null) {
            for (Map<String, Object> capability : device.getCapabilities()) {
                if (capability.containsKey("type") && capability.containsKey("state")) {
                    String capabilityType = (String) capability.get("type");
                    Object actionValue = capability.get("state");

                    boolean actionResult = executeDeviceAction(capabilityType, actionValue);
                    if (!actionResult) {
                        allActionsSuccessful = false;
                    }
                }
            }
        }

        if (allActionsSuccessful) {
            return YandexSmartHomeResponse.builder()
                    .requestId(requestId)
                    .status("ok")
                    .payload(YandexSmartHomeResponse.Payload.builder()
                            .userId(userId)
                            .devices(List.of(createUpdatedDeviceState()))
                            .build())
                    .build();
        } else {
            return createErrorResponse(requestId, "Some actions failed to execute");
        }
    }

    private boolean executeDeviceAction(String capabilityType, Object actionValue) {
        try {
            if (actionValue instanceof Map) {
                Map<String, Object> actionMap = (Map<String, Object>) actionValue;
                String instance = (String) actionMap.get("instance");

                return switch (capabilityType) {
                    case "devices.capabilities.on_off" -> handlePowerAction(actionValue);
                    case "devices.capabilities.range" -> handleRangeAction(instance, actionValue);
                    default -> {
                        log.warn("Unsupported capability type: {}", capabilityType);
                        yield false;
                    }
                };
            }
            log.warn("Invalid action value format for capability: {}", capabilityType);
            return false;
        } catch (Exception e) {
            log.error("Error executing device action for capability: {}", capabilityType, e);
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private boolean handlePowerAction(Object actionValue) {
        if (actionValue instanceof Map) {
            Map<String, Object> actionMap = (Map<String, Object>) actionValue;
            if (actionMap.containsKey("value")) {
                boolean powerOn = Boolean.TRUE.equals(actionMap.get("value"));
                return stingrayTVService.setPowerState(powerOn);
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private boolean handleRangeAction(String instance, Object actionValue) {
        if (actionValue instanceof Map) {
            Map<String, Object> actionMap = (Map<String, Object>) actionValue;
            if (actionMap.containsKey("value")) {
                int value = ((Number) actionMap.get("value")).intValue();

                return switch (instance) {
                    case "volume" -> stingrayTVService.setVolume(value);
                    case "channel" -> stingrayTVService.changeChannel(value);
                    default -> {
                        log.warn("Unsupported range instance: {}", instance);
                        yield false;
                    }
                };
            }
        }
        return false;
    }

    private List<YandexSmartHomeResponse.Payload.Device.Capability> createDeviceCapabilities() {
        return List.of(
                YandexSmartHomeResponse.Payload.Device.Capability.builder()
                        .type("devices.capabilities.on_off")
                        .retrievable(true)
                        .build(),
                YandexSmartHomeResponse.Payload.Device.Capability.builder()
                        .type("devices.capabilities.range")
                        .retrievable(true)
                        .parameters(Map.of(
                                "instance", "volume",
                                "unit", "unit.percent",
                                "range", Map.of(
                                        "min", 0,
                                        "max", 20,
                                        "precision", 1
                                )
                        )).build(),
                YandexSmartHomeResponse.Payload.Device.Capability.builder()
                        .type("devices.capabilities.range")
                        .retrievable(true)
                        .parameters(Map.of(
                                "instance", "channel",
                                "random_access", true,
                                "range", Map.of(
                                        "min", 0,
                                        "max", 9999,
                                        "precision", 1
                                )
                        )).build()
        );
    }

    private List<YandexSmartHomeResponse.Payload.Device.Capability> createCurrentCapabilityStates() {
        StingrayTVService.PowerState powerState = stingrayTVService.getPowerState();
        StingrayTVService.VolumeState volumeState = stingrayTVService.getVolumeState();
        StingrayTVService.ChannelState channelState = stingrayTVService.getCurrentChannel();

        return List.of(
                YandexSmartHomeResponse.Payload.Device.Capability.builder()
                        .type("devices.capabilities.on_off")
                        .state(Map.of(
                                "instance", "on",
                                "value", "on".equals(powerState.getState())
                        )).build(),
                YandexSmartHomeResponse.Payload.Device.Capability.builder()
                        .type("devices.capabilities.range")
                        .state(Map.of(
                                "instance", "channel",
                                "value", channelState.getChannelNumber()
                        )).build(),
                YandexSmartHomeResponse.Payload.Device.Capability.builder()
                        .type("devices.capabilities.range")
                        .state(Map.of(
                                "instance", "volume",
                                "value", volumeState.getState()
                        )).build()
        );
    }

    private YandexSmartHomeResponse.Payload.Device createUpdatedDeviceState() {
        StingrayTVService.PowerState powerState = stingrayTVService.getPowerState();
        StingrayTVService.VolumeState volumeState = stingrayTVService.getVolumeState();
        StingrayTVService.ChannelState channelState = stingrayTVService.getCurrentChannel();

        return YandexSmartHomeResponse.Payload.Device.builder()
                .id(stingrayProperties.getDeviceId())
                .capabilities(List.of(
                        YandexSmartHomeResponse.Payload.Device.Capability.builder()
                                .type("devices.capabilities.on_off")
                                .state(Map.of(
                                        "instance", "on",
                                        "value", "on".equals(powerState.getState())
                                ))
                                .build(),
                        YandexSmartHomeResponse.Payload.Device.Capability.builder()
                                .type("devices.capabilities.range")
                                .state(Map.of(
                                        "instance", "channel",
                                        "value", channelState.getChannelNumber()
                                )).build(),
                        YandexSmartHomeResponse.Payload.Device.Capability.builder()
                                .type("devices.capabilities.range")
                                .state(Map.of(
                                        "instance", "volume",
                                        "value", volumeState.getState()
                                ))
                                .build()
                ))
                .build();
    }

    private YandexSmartHomeResponse createErrorResponse(String requestId, String errorMessage) {
        return YandexSmartHomeResponse.builder()
                .requestId(requestId)
                .status("error")
                .errorCode("INTERNAL_ERROR")
                .errorMessage(errorMessage)
                .build();
    }
}