package ru.oldzoomer.stingraytv_alice.gateway;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.oldzoomer.stingraytv_alice.config.StingrayConfigurationProperties;
import ru.oldzoomer.stingraytv_alice.dto.yandex.YandexSmartHomeRequest;
import ru.oldzoomer.stingraytv_alice.dto.yandex.YandexSmartHomeResponse;
import ru.oldzoomer.stingraytv_alice.enums.QueryTypes;
import ru.oldzoomer.stingraytv_alice.service.StingrayDeviceDiscoveryService;
import ru.oldzoomer.stingraytv_alice.service.StingrayTVService;

/**
 * Main gateway for Yandex Smart Home integration with StingrayTV API.
 * This component handles all communication between Yandex Smart Home and the StingrayTV receiver.
 * It processes requests, manages device capabilities, and coordinates with the service layer.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class YandexSmartHomeGateway {
    private final StingrayConfigurationProperties stingrayConfigurationProperties;
    private final StingrayTVService stingrayTVService;
    private final StingrayDeviceDiscoveryService.Device stingrayDevice;

    /**
     * Processes Yandex Smart Home request with user ID and returns response.
     * This is the main entry point for handling all Yandex Smart Home API requests.
     *
     * @param request the incoming request payload
     * @param requestId unique identifier for the request
     * @param userId identifier of the authenticated user
     * @param type type of request being processed
     * @return YandexSmartHomeResponse with the processed result
     */
    public YandexSmartHomeResponse processRequest(YandexSmartHomeRequest request, String requestId, String userId, QueryTypes type) {
        log.debug("Processing Yandex Smart Home request: {}, user: {}", requestId, userId);

        try {
            // Handle query and action requests (with devices in payload)
            return handleDevicesRequest(request, requestId, userId, type);
        } catch (Exception e) {
            log.error("Error processing Yandex Smart Home request: {}", requestId, e);
            return createErrorResponse(requestId, "Internal server error");
        }
    }

    /**
     * Handles different types of device requests based on the request type.
     * Routes requests to appropriate handlers for discovery, query, or action operations.
     *
     * @param request the incoming request payload
     * @param requestId unique identifier for the request
     * @param userId identifier of the authenticated user
     * @param type type of request being processed
     * @return YandexSmartHomeResponse with the processed result
     */
    private YandexSmartHomeResponse handleDevicesRequest(YandexSmartHomeRequest request, String requestId, String userId, QueryTypes type) {
        log.debug("Handling devices request of type: {}", type);
        // Determine request type based on payload structure
        return switch (type) {
            case DEVICES_QUERY -> handleQueryRequest(requestId, userId);
            case DEVICES_ACTION -> handleActionRequest(request, requestId, userId);
            case DEVICES_DISCOVERY -> handleDiscoveryRequest(requestId, userId);
            case null -> createErrorResponse(requestId, "Unrecognized request type");
        };
    }

    /**
     * Handles device discovery requests.
     * Returns information about available devices to Yandex Smart Home.
     *
     * @param requestId unique identifier for the request
     * @param userId identifier of the authenticated user
     * @return YandexSmartHomeResponse with device discovery information
     */
    private YandexSmartHomeResponse handleDiscoveryRequest(String requestId, String userId) {
        log.info("Handling device discovery request for user: {}", userId);

        YandexSmartHomeResponse.Payload.Device device = YandexSmartHomeResponse.Payload.Device.builder()
                .id(stingrayDevice.serialNumber())
                .name(stingrayDevice.model())
                .description(stingrayConfigurationProperties.getDeviceDescription())
                .room(stingrayConfigurationProperties.getRoom())
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

    /**
     * Handles device state query requests.
     * Returns current state information for devices to Yandex Smart Home.
     *
     * @param requestId unique identifier for the request
     * @param userId identifier of the authenticated user
     * @return YandexSmartHomeResponse with device state information
     */
    private YandexSmartHomeResponse handleQueryRequest(String requestId, String userId) {
        log.info("Handling device query request for user: {}", userId);

        try {
            YandexSmartHomeResponse.Payload.Device device = YandexSmartHomeResponse.Payload.Device.builder()
                    .id(stingrayDevice.serialNumber())
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

    /**
     * Handles device action requests.
     * Processes commands to control devices from Yandex Smart Home.
     *
     * @param request the incoming request payload
     * @param requestId unique identifier for the request
     * @param userId identifier of the authenticated user
     * @return YandexSmartHomeResponse with action execution results
     */
    private YandexSmartHomeResponse handleActionRequest(YandexSmartHomeRequest request, String requestId, String userId) {
        log.info("Handling device action request for user: {}", userId);

        try {
            if (request.getPayload().getDevices() == null || request.getPayload().getDevices().isEmpty()) {
                return createErrorResponse(requestId, "No devices specified in action request");
            }

            // Process actions for each device
            for (YandexSmartHomeRequest.Payload.Device device : request.getPayload().getDevices()) {
                if (stingrayDevice.serialNumber().equals(device.getId())) {
                    return processDeviceActions(device, requestId, userId);
                }
            }

            return createErrorResponse(requestId, "Device not found");

        } catch (Exception e) {
            log.error("Error handling action request", e);
            return createErrorResponse(requestId, "Failed to execute device action");
        }
    }

    /**
     * Processes actions for a specific device.
     * Executes individual capability actions for the device.
     *
     * @param device the device to process actions for
     * @param requestId unique identifier for the request
     * @param userId identifier of the authenticated user
     * @return YandexSmartHomeResponse with action execution results
     */
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

    /**
     * Executes a specific device action based on capability type.
     * Routes actions to appropriate handlers based on capability type.
     *
     * @param capabilityType type of capability being executed
     * @param actionValue value for the action
     * @return true if action was successful, false otherwise
     */
    private boolean executeDeviceAction(String capabilityType, Object actionValue) {
        try {
            if (actionValue instanceof Map) {
                @SuppressWarnings("unchecked")
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

    /**
     * Handles power state actions (on/off).
     *
     * @param actionValue value for the power action
     * @return true if action was successful, false otherwise
     */
    private boolean handlePowerAction(Object actionValue) {
        if (actionValue instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> actionMap = (Map<String, Object>) actionValue;
            if (actionMap.containsKey("value")) {
                boolean powerOn = Boolean.TRUE.equals(actionMap.get("value"));
                return stingrayTVService.setPowerState(powerOn);
            }
        }
        return false;
    }

    /**
     * Handles range actions (volume, channel).
     *
     * @param instance type of range action (volume, channel)
     * @param actionValue value for the action
     * @return true if action was successful, false otherwise
     */
    private boolean handleRangeAction(String instance, Object actionValue) {
        if (actionValue instanceof Map) {
            @SuppressWarnings("unchecked")
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

    /**
     * Creates the list of device capabilities.
     * Defines what actions and properties this device supports.
     *
     * @return List of device capabilities
     */
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

    /**
     * Creates the current capability states for device query requests.
     * Returns the current state of device capabilities.
     *
     * @return List of current capability states
     */
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

    /**
     * Creates the updated device state for action responses.
     * Returns the updated state after executing actions.
     *
     * @return Device state with action results
     */
    private YandexSmartHomeResponse.Payload.Device createUpdatedDeviceState() {
        return YandexSmartHomeResponse.Payload.Device.builder()
                .id(stingrayDevice.serialNumber())
                .capabilities(List.of(
                        YandexSmartHomeResponse.Payload.Device.Capability.builder()
                                .type("devices.capabilities.on_off")
                                .state(Map.of(
                                        "instance", "on",
                                        "action_result", Map.of("status", "DONE")
                                ))
                                .build(),
                        YandexSmartHomeResponse.Payload.Device.Capability.builder()
                                .type("devices.capabilities.range")
                                .state(Map.of(
                                        "instance", "channel",
                                        "action_result", Map.of("status", "DONE")
                                )).build(),
                        YandexSmartHomeResponse.Payload.Device.Capability.builder()
                                .type("devices.capabilities.range")
                                .state(Map.of(
                                        "instance", "volume",
                                        "action_result", Map.of("status", "DONE")
                                ))
                                .build()
                ))
                .build();
    }

    /**
     * Creates an error response for failed requests.
     *
     * @param requestId unique identifier for the request
     * @param errorMessage error message to include in response
     * @return YandexSmartHomeResponse with error status
     */
    private YandexSmartHomeResponse createErrorResponse(String requestId, String errorMessage) {
        return YandexSmartHomeResponse.builder()
                .requestId(requestId)
                .status("error")
                .errorCode("INTERNAL_ERROR")
                .errorMessage(errorMessage)
                .build();
    }
}