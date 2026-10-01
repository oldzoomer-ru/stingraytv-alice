package ru.oldzoomer.stingraytv_alice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Service for communicating with the StingrayTV satellite receiver.
 * Provides methods for querying device state and sending control commands.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StingrayTVService {

    private final RestClient restClient;
    private final StingrayDeviceDiscoveryService.Device device;

    /**
     * Result of an action performed on the StingrayTV device.
     *
     * @param ok whether the action succeeded
     * @param errorMessage descriptive error message, null if successful
     */
    public record ActionResult(boolean ok, String errorMessage) {
        public static ActionResult success() {
            return new ActionResult(true, null);
        }

        public static ActionResult failure(String message) {
            return new ActionResult(false, message);
        }
    }

    /**
     * Gets the current power state of the StingrayTV device.
     *
     * @return PowerState object with the current power state
     */
    public PowerState getPowerState() {
        return executeWithBaseUrl(baseUrl ->
                restClient.get()
                        .uri(baseUrl + "/power")
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .body(PowerState.class),
                "power state",
                () -> new PowerState("offline")
        );
    }

    /**
     * Sets the power state of the StingrayTV device.
     *
     * @param powerOn true to turn on, false to turn off
     * @return ActionResult indicating success or failure
     */
    public ActionResult setPowerState(boolean powerOn) {
        return executeAction("power state", baseUrl -> {
            String powerState = powerOn ? "on" : "off";
            Map<String, String> requestBody = Map.of("state", powerState);
            restClient.put()
                    .uri(baseUrl + "/power")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Successfully set power state to '{}' on device at URL: {}", powerState, baseUrl);
        });
    }

    /**
     * Gets the current volume state of the StingrayTV device.
     *
     * @return VolumeState object with the current volume state
     */
    public VolumeState getVolumeState() {
        return executeWithBaseUrl(baseUrl ->
                restClient.get()
                        .uri(baseUrl + "/volume")
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .body(VolumeState.class),
                "volume state",
                () -> new VolumeState(20, 0)
        );
    }

    /**
     * Sets the volume of the StingrayTV device.
     *
     * @param volume the volume level to set (0-20)
     * @return ActionResult indicating success or failure
     */
    public ActionResult setVolume(int volume) {
        if (volume < 0 || volume > 20) {
            log.warn("Volume out of range: {}, must be 0-20", volume);
            return ActionResult.failure("Volume must be between 0 and 20");
        }

        return executeAction("volume", baseUrl -> {
            Map<String, Integer> requestBody = Map.of("state", volume);
            log.debug("Setting volume to '{}' on device at URL: {}", volume, baseUrl + "/volume");
            restClient.put()
                    .uri(baseUrl + "/volume")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Successfully set volume to '{}' on device at URL: {}", volume, baseUrl);
        });
    }

    /**
     * Gets the current channel information from the StingrayTV device.
     *
     * @return ChannelState object with current channel information
     */
    public ChannelState getCurrentChannel() {
        return executeWithBaseUrl(baseUrl -> {
            ResponseEntity<ChannelState[]> response = restClient.get()
                    .uri(baseUrl + "/channels/current")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .toEntity(ChannelState[].class);
            if (response.getBody() != null && response.getBody().length > 0) {
                return response.getBody()[0];
            }
            return null;
        }, "current channel", () -> new ChannelState(0, "Unknown"));
    }

    /**
     * Changes the channel on the StingrayTV device.
     *
     * @param channelNumber the channel number to change to (0-9999)
     * @return ActionResult indicating success or failure
     */
    public ActionResult changeChannel(int channelNumber) {
        if (channelNumber < 0 || channelNumber > 9999) {
            log.warn("Channel number out of range: {}, must be 0-9999", channelNumber);
            return ActionResult.failure("Channel number must be between 0 and 9999");
        }

        return executeAction("channel", baseUrl -> {
            ChannelState channelState = getCurrentChannel();
            Map<String, Object> requestBody = Map.of(
                    "channelNumber", channelNumber,
                    "channelListId", channelState.channelListId()
            );
            log.debug("Changing channel to '{}' on device at URL: {}", channelNumber, baseUrl + "/channels/current");
            restClient.put()
                    .uri(baseUrl + "/channels/current")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Successfully changed channel to '{}' on device at URL: {}", channelNumber, baseUrl);
        });
    }

    /**
     * Sends a mute command to the StingrayTV device.
     *
     * @return ActionResult indicating success or failure
     */
    public ActionResult mute() {
        return executeAction("mute", baseUrl -> {
            Map<String, String> requestBody = Map.of("key", "Volume Mute");
            log.debug("Sending mute command to device at URL: {}", baseUrl + "/input/events");
            restClient.post()
                    .uri(baseUrl + "/input/events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Successfully sent mute command to device at URL: {}", baseUrl);
        });
    }

    /**
     * Sends a play/pause command to the StingrayTV device.
     *
     * @return ActionResult indicating success or failure
     */
    public ActionResult pause() {
        return executeAction("pause", baseUrl -> {
            Map<String, String> requestBody = Map.of("key", "Pause");
            log.debug("Sending pause command to device at URL: {}", baseUrl + "/input/events");
            restClient.post()
                    .uri(baseUrl + "/input/events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Successfully sent pause command to device at URL: {}", baseUrl);
        });
    }

    /**
     * Executes an action that reads a state value from the device.
     *
     * @param <T> the response type
     * @param requestFn function that performs the REST call given the base URL
     * @param operationName human-readable name for logging
     * @param defaultFn supplier of the default value on failure
     * @return the response or a default value on failure
     */
    private <T> T executeWithBaseUrl(java.util.function.Function<String, T> requestFn,
                                     String operationName,
                                     java.util.function.Supplier<T> defaultFn) {
        try {
            String baseUrl = device.baseUrl();
            if (baseUrl == null) {
                log.warn("Device base URL is null, returning default {} state", operationName);
                return defaultFn.get();
            }

            log.debug("Getting {} from device at URL: {}", operationName, baseUrl);
            T response = requestFn.apply(baseUrl);

            if (response != null) {
                log.debug("Successfully retrieved {}", operationName);
                return response;
            } else {
                log.warn("Received null {} response, defaulting", operationName);
                return defaultFn.get();
            }
        } catch (Exception e) {
            log.error("Error getting {} from StingrayTV device at URL: {}",
                    operationName, device != null ? device.baseUrl() : "unknown", e);
            return defaultFn.get();
        }
    }

    /**
     * Executes an action that sends a command to the device.
     *
     * @param actionName human-readable name of the action for logging
     * @param actionFn function that performs the REST call given the base URL
     * @return ActionResult indicating success or failure
     */
    private ActionResult executeAction(String actionName, java.util.function.Consumer<String> actionFn) {
        try {
            String baseUrl = device.baseUrl();
            if (baseUrl == null) {
                log.warn("Device base URL is null, cannot {}", actionName);
                return ActionResult.failure("Device not available");
            }

            actionFn.accept(baseUrl);
            return ActionResult.success();
        } catch (Exception e) {
            log.error("Error {} on StingrayTV device at URL: {}",
                    actionName, device != null ? device.baseUrl() : "unknown", e);
            return ActionResult.failure("Error " + actionName);
        }
    }

    public record PowerState(String state) {
    }

    public record VolumeState(int max, int state) {
    }

    public record ChannelState(int channelNumber, String channelListId) {
    }
}
