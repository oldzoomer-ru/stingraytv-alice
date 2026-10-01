package ru.oldzoomer.stingraytv_alice.gateway;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.oldzoomer.stingraytv_alice.config.StingrayConfigurationProperties;
import ru.oldzoomer.stingraytv_alice.dto.yandex.YandexSmartHomeRequest;
import ru.oldzoomer.stingraytv_alice.dto.yandex.YandexSmartHomeResponse;
import ru.oldzoomer.stingraytv_alice.enums.QueryTypes;
import ru.oldzoomer.stingraytv_alice.service.StingrayDeviceDiscoveryService;
import ru.oldzoomer.stingraytv_alice.service.StingrayTVService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    public YandexSmartHomeResponse processRequest(YandexSmartHomeRequest request, String requestId,
                                                  String userId, QueryTypes type) {
        log.debug("Processing Yandex Smart Home request: {}, user: {}", requestId, userId);

        try {
            // Handle query and action requests (with devices in payload)
            return handleDevicesRequest(request, requestId, userId, type);
        } catch (Exception e) {
            log.error("Error processing Yandex Smart Home request: {}", requestId, e);
            return createErrorResponse(requestId, "INTERNAL_ERROR", "Internal server error");
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
    private YandexSmartHomeResponse handleDevicesRequest(YandexSmartHomeRequest request, String requestId,
                                                         String userId, QueryTypes type) {
        log.debug("Handling devices request of type: {}", type);
        // Determine request type based on payload structure
        return switch (type) {
            case DEVICES_QUERY -> handleQueryRequest(requestId, userId);
            case DEVICES_ACTION -> handleActionRequest(request, requestId, userId);
            case DEVICES_DISCOVERY -> handleDiscoveryRequest(requestId, userId);
            case null -> createErrorResponse(requestId, "INTERNAL_ERROR", "Unrecognized request type");
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

        String serialNumber = stingrayDevice != null ? stingrayDevice.serialNumber() : "unknown";
        String model = stingrayDevice != null ? stingrayDevice.model() : "Unknown";
        String hardwareId = stingrayDevice != null ? stingrayDevice.hardwareId() : null;
        String softwareVersion = stingrayDevice != null ? stingrayDevice.softwareVersion() : null;

        YandexSmartHomeResponse.Payload.Device device = new YandexSmartHomeResponse.Payload.Device(
                serialNumber,
                model,
                stingrayConfigurationProperties.getDeviceDescription(),
                stingrayConfigurationProperties.getRoom(),
                "devices.types.media_device.receiver",
                createDeviceCapabilities(),
                null,
                createStatusInfo(),
                createDeviceInfo(model, hardwareId, softwareVersion)
        );

        YandexSmartHomeResponse.Payload payload = new YandexSmartHomeResponse.Payload(
                userId,
                List.of(device)
        );

        return new YandexSmartHomeResponse(requestId, "ok", null, null, payload);
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
            String serialNumber = stingrayDevice != null ? stingrayDevice.serialNumber() : "unknown";
            YandexSmartHomeResponse.Payload.Device device = new YandexSmartHomeResponse.Payload.Device(
                    serialNumber,
                    null,
                    null,
                    null,
                    null,
                    createCurrentCapabilityStates(),
                    null,
                    null,
                    null
            );

            YandexSmartHomeResponse.Payload payload = new YandexSmartHomeResponse.Payload(
                    userId,
                    List.of(device)
            );

            return new YandexSmartHomeResponse(requestId, "ok", null, null, payload);

        } catch (Exception e) {
            log.error("Error handling query request", e);
            return createErrorResponse(requestId, "INTERNAL_ERROR", "Failed to query device state");
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
    private YandexSmartHomeResponse handleActionRequest(YandexSmartHomeRequest request, String requestId,
                                                        String userId) {
        log.info("Handling device action request for user: {}", userId);

        try {
            if (request.payload().devices() == null || request.payload().devices().isEmpty()) {
                return createErrorResponse(requestId, "INVALID_REQUEST", "No devices specified in action request");
            }

            // Process actions for each device
            List<String> errors = new ArrayList<>();
            for (YandexSmartHomeRequest.Payload.Device device : request.payload().devices()) {
                if (stingrayDevice != null && stingrayDevice.serialNumber().equals(device.id())) {
                    List<String> actionErrors = processDeviceActions(device);
                    if (!actionErrors.isEmpty()) {
                        errors.addAll(actionErrors);
                    }
                }
            }

            if (errors.isEmpty()) {
                return new YandexSmartHomeResponse(
                        requestId,
                        "ok",
                        null,
                        null,
                        new YandexSmartHomeResponse.Payload(
                                userId,
                                List.of(createUpdatedDeviceState())
                        )
                );
            } else {
                String errorMessage = String.join("; ", errors);
                return createErrorResponse(requestId, "DEVICE_FAILED", errorMessage);
            }

        } catch (Exception e) {
            log.error("Error handling action request", e);
            return createErrorResponse(requestId, "INTERNAL_ERROR", "Failed to execute device action");
        }
    }

    /**
     * Processes actions for a specific device.
     * Executes individual capability actions for the device.
     *
     * @param device the device to process actions for
     * @return list of error messages for failed actions, empty if all succeeded
     */
    private List<String> processDeviceActions(YandexSmartHomeRequest.Payload.Device device) {
        List<String> errors = new ArrayList<>();

        if (device.capabilities() != null) {
            for (Map<String, Object> capability : device.capabilities()) {
                if (capability.containsKey("type") && capability.containsKey("state")) {
                    String capabilityType = (String) capability.get("type");
                    Map<String, Object> actionMap = extractActionMap(capability.get("state"));

                    if (actionMap == null) {
                        errors.add("Invalid action format for capability: " + capabilityType);
                        continue;
                    }

                    String instance = (String) actionMap.get("instance");
                    String actionError = executeDeviceAction(capabilityType, actionMap, instance);
                    if (actionError != null) {
                        errors.add(actionError);
                    }
                }
            }
        }

        return errors;
    }

    /**
     * Safely extracts an action map from the action value.
     * Returns null if the value is not a Map.
     *
     * @param actionValue the raw action value from the Yandex request
     * @return the action map, or null if not a valid Map
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> extractActionMap(Object actionValue) {
        if (actionValue instanceof Map) {
            return (Map<String, Object>) actionValue;
        }
        return null;
    }

    /**
     * Executes a specific device action based on capability type.
     * Routes actions to appropriate handlers based on capability type.
     *
     * @param capabilityType type of capability being executed
     * @param actionMap parsed action parameters
     * @param instance the capability instance (e.g., "volume", "mute")
     * @return error message if failed, null if successful
     */
    private String executeDeviceAction(String capabilityType, Map<String, Object> actionMap, String instance) {
        try {
            return switch (capabilityType) {
                case "devices.capabilities.on_off" -> handlePowerAction(actionMap);
                case "devices.capabilities.range" -> handleRangeAction(instance, actionMap);
                case "devices.capabilities.toggle" -> handleToggleAction(instance);
                default -> {
                    log.warn("Unsupported capability type: {}", capabilityType);
                    yield "Unsupported capability: " + capabilityType;
                }
            };
        } catch (Exception e) {
            log.error("Error executing device action for capability: {}", capabilityType, e);
            return "Error executing " + capabilityType;
        }
    }

    /**
     * Handles power state actions (on/off).
     *
     * @param actionMap the action parameters map
     * @return error message if failed, null if successful
     */
    private String handlePowerAction(Map<String, Object> actionMap) {
        if (actionMap.containsKey("value")) {
            boolean powerOn = Boolean.TRUE.equals(actionMap.get("value"));
            StingrayTVService.ActionResult result = stingrayTVService.setPowerState(powerOn);
            return result.errorMessage();
        }
        return "Invalid power action value";
    }

    /**
     * Handles range actions (volume, channel).
     *
     * @param instance type of range action (volume, channel)
     * @param actionMap the action parameters map
     * @return error message if failed, null if successful
     */
    private String handleRangeAction(String instance, Map<String, Object> actionMap) {
        if (actionMap.containsKey("value")) {
            Number value = (Number) actionMap.get("value");
            int intValue = value.intValue();

            return switch (instance) {
                case "volume" -> {
                    StingrayTVService.ActionResult result = stingrayTVService.setVolume(intValue);
                    yield result.errorMessage();
                }
                case "channel" -> {
                    StingrayTVService.ActionResult result = stingrayTVService.changeChannel(intValue);
                    yield result.errorMessage();
                }
                default -> {
                    log.warn("Unsupported range instance: {}", instance);
                    yield "Unsupported range instance: " + instance;
                }
            };
        }
        return "Invalid range action value";
    }

    /**
     * Handles toggle actions (mute, pause).
     *
     * @param instance type of toggle action (mute, pause)
     * @return error message if failed, null if successful
     */
    private String handleToggleAction(String instance) {
        return switch (instance) {
            case "mute" -> {
                StingrayTVService.ActionResult result = stingrayTVService.mute();
                yield result.errorMessage();
            }
            case "pause" -> {
                StingrayTVService.ActionResult result = stingrayTVService.pause();
                yield result.errorMessage();
            }
            default -> {
                log.warn("Unsupported toggle instance: {}", instance);
                yield "Unsupported toggle instance: " + instance;
            }
        };
    }

    /**
     * Creates the list of device capabilities.
     * Defines what actions and properties this device supports.
     *
     * @return List of device capabilities
     */
    private List<YandexSmartHomeResponse.Payload.Device.Capability> createDeviceCapabilities() {
        return List.of(
                new YandexSmartHomeResponse.Payload.Device.Capability("devices.capabilities.on_off", true, null, null),
                new YandexSmartHomeResponse.Payload.Device.Capability("devices.capabilities.range", true,
                        Map.of("instance", "volume", "unit", "unit.percent",
                                "range", Map.of("min", 0, "max", 20, "precision", 1)), null),
                new YandexSmartHomeResponse.Payload.Device.Capability("devices.capabilities.range", true,
                        Map.of("instance", "channel", "random_access", true,
                                "range", Map.of("min", 0, "max", 9999, "precision", 1)), null),
                new YandexSmartHomeResponse.Payload.Device.Capability("devices.capabilities.toggle", false,
                        Map.of("instance", "mute"), null),
                new YandexSmartHomeResponse.Payload.Device.Capability("devices.capabilities.toggle", false,
                        Map.of("instance", "pause"), null)
        );
    }

    /**
     * Creates a single capability entry for the given type, instance, and state.
     *
     * @param type the capability type
     * @param instance the capability instance (e.g., "on", "volume", "channel")
     * @param state the state map
     * @return a Capability record
     */
    private YandexSmartHomeResponse.Payload.Device.Capability capability(
            String type, String instance, Map<String, Object> state) {
        return new YandexSmartHomeResponse.Payload.Device.Capability(
                type, false, null,
                instance != null ? mergeState(state, Map.of("instance", instance)) : state
        );
    }

    /**
     * Merges an instance key into a state map if not already present.
     */
    private Map<String, Object> mergeState(Map<String, Object> state, Map<String, Object> extra) {
        Map<String, Object> merged = new LinkedHashMap<>(state);
        merged.putAll(extra);
        return merged;
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
                capability("devices.capabilities.on_off", "on",
                        Map.of("value", "on".equals(powerState.state()))),
                capability("devices.capabilities.range", "channel",
                        Map.of("value", channelState.channelNumber())),
                capability("devices.capabilities.range", "volume",
                        Map.of("value", volumeState.state()))
        );
    }

    /**
     * Creates the updated device state for action responses.
     * Returns the updated state after executing actions.
     *
     * @return Device state with action results
     */
    private YandexSmartHomeResponse.Payload.Device createUpdatedDeviceState() {
        return new YandexSmartHomeResponse.Payload.Device(
                stingrayDevice.serialNumber(),
                null,
                null,
                null,
                null,
                List.of(
                        capability("devices.capabilities.on_off", "on",
                                Map.of("action_result", Map.of("status", "DONE"))),
                        capability("devices.capabilities.range", "channel",
                                Map.of("action_result", Map.of("status", "DONE"))),
                        capability("devices.capabilities.range", "volume",
                                Map.of("action_result", Map.of("status", "DONE"))),
                        capability("devices.capabilities.toggle", "mute",
                                Map.of("action_result", Map.of("status", "DONE"))),
                        capability("devices.capabilities.toggle", "pause",
                                Map.of("action_result", Map.of("status", "DONE")))
                ),
                null,
                null,
                null
        );
    }

    /**
     * Creates an error response for failed requests.
     *
     * @param requestId unique identifier for the request
     * @param errorCode structured error code per Yandex Smart Home protocol
     * @param errorMessage human-readable error message
     * @return YandexSmartHomeResponse with error status
     */
    private YandexSmartHomeResponse createErrorResponse(String requestId, String errorCode, String errorMessage) {
        return new YandexSmartHomeResponse(requestId, "error", errorCode, errorMessage, null);
    }

    /**
     * Creates a StatusInfo object with the given status.
     *
     * @return A StatusInfo object with the given status.
     */
    private YandexSmartHomeResponse.Payload.Device.StatusInfo createStatusInfo() {
        return new YandexSmartHomeResponse.Payload.Device.StatusInfo(true);
    }

    /**
     * Creates a DeviceInfo object with the given manufacturer, model, hwVersion, and swVersion.
     *
     * @param model     The model of the device.
     * @param hwVersion The hardware version of the device.
     * @param swVersion The software version of the device.
     * @return DeviceInfo with the given manufacturer, model, hwVersion, and swVersion.
     */
    private YandexSmartHomeResponse.Payload.Device.DeviceInfo createDeviceInfo(String model, String hwVersion,
                                                                               String swVersion) {
        return new YandexSmartHomeResponse.Payload.Device.DeviceInfo("General Satellite", model, hwVersion, swVersion);
    }
}
