package ru.oldzoomer.stingraytv_alice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class StingrayTVService {

    private final RestClient restClient;
    private final WebClient webClient;
    private final StingrayDeviceDiscoveryService.Device device;

    /**
     * Gets the current power state of the StingrayTV device.
     *
     * @return PowerState object with the current power state
     */
    public PowerState getPowerState() {
        try {
            String baseUrl = device.baseUrl();
            if (baseUrl == null) {
                log.warn("Device base URL is null, returning offline state");
                return new PowerState("offline");
            }

            log.debug("Getting power state from device at URL: {}", baseUrl + "/power");
            PowerState response = restClient.get()
                    .uri(baseUrl + "/power")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(PowerState.class);

            if (response != null && response.state != null) {
                log.debug("Successfully retrieved power state: {}", response.state);
                return response;
            } else {
                log.warn("Received null or empty power state response, defaulting to offline");
                return new PowerState("offline");
            }
        } catch (Exception e) {
            log.error("Error getting power state from StingrayTV device at URL: {}", device.baseUrl(), e);
            return new PowerState("offline");
        }
    }

    /**
     * Sets the power state of the StingrayTV device.
     *
     * @param powerOn true to turn on, false to turn off
     * @return true if successful, false otherwise
     */
    public boolean setPowerState(boolean powerOn) {
        try {
            String baseUrl = device.baseUrl();
            if (baseUrl == null) {
                log.warn("Device base URL is null, cannot set power state");
                return false;
            }

            String powerState = powerOn ? "on" : "off";
            Map<String, String> requestBody = Map.of("state", powerState);
            
            log.debug("Setting power state to '{}' on device at URL: {}", powerState, baseUrl + "/power");

            restClient.put()
                    .uri(baseUrl + "/power")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Successfully set power state to '{}' on device at URL: {}", powerState, baseUrl);
            return true;
        } catch (Exception e) {
            log.error("Error setting power state '{}' on StingrayTV device at URL: {}", powerOn ? "on" : "off", device.baseUrl(), e);
            return false;
        }
    }

    /**
     * Gets the current volume state of the StingrayTV device.
     *
     * @return VolumeState object with the current volume state
     */
    public VolumeState getVolumeState() {
        try {
            String baseUrl = device.baseUrl();
            if (baseUrl == null) {
                log.warn("Device base URL is null, returning default volume state");
                return new VolumeState(20, 0);
            }

            log.debug("Getting volume state from device at URL: {}", baseUrl + "/volume");
            VolumeState response = restClient.get()
                    .uri(baseUrl + "/volume")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(VolumeState.class);

            if (response != null) {
                log.debug("Successfully retrieved volume state: {}", response.state);
                return response;
            } else {
                log.warn("Received null volume state response, defaulting to 0");
                return new VolumeState(20, 0);
            }
        } catch (Exception e) {
            log.error("Error getting volume state from StingrayTV device at URL: {}", device.baseUrl(), e);
            return new VolumeState(20, 0);
        }
    }

    /**
     * Sets the volume of the StingrayTV device.
     *
     * @param volume the volume level to set
     * @return true if successful, false otherwise
     */
    public boolean setVolume(int volume) {
        try {
            String baseUrl = device.baseUrl();
            if (baseUrl == null) {
                log.warn("Device base URL is null, cannot set volume");
                return false;
            }

            Map<String, Integer> requestBody = Map.of("state", volume);
            log.debug("Setting volume to '{}' on device at URL: {}", volume, baseUrl + "/volume");

            restClient.put()
                    .uri(baseUrl + "/volume")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Successfully set volume to '{}' on device at URL: {}", volume, baseUrl);
            return true;
        } catch (Exception e) {
            log.error("Error setting volume to '{}' on StingrayTV device at URL: {}", volume, device.baseUrl(), e);
            return false;
        }
    }

    /**
     * Gets the current channel information from the StingrayTV device.
     *
     * @return ChannelState object with current channel information
     */
    public ChannelState getCurrentChannel() {
        try {
            String baseUrl = device.baseUrl();
            if (baseUrl == null) {
                log.warn("Device base URL is null, returning default channel state");
                return new ChannelState(0, "Unknown");
            }

            log.debug("Getting current channel from device at URL: {}", baseUrl + "/channels/current");
            ChannelState response = webClient.get()
                    .uri(baseUrl + "/channels/current")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToFlux(ChannelState.class)
                    .blockFirst();

            if (response != null) {
                log.debug("Successfully retrieved current channel: {} (channel list ID: {})",
                         response.channelNumber, response.channelListId);
                return response;
            } else {
                log.warn("Received null channel state response, defaulting to channel 0");
                return new ChannelState(0, "Unknown");
            }
        } catch (Exception e) {
            log.error("Error getting current channel from StingrayTV device at URL: {}", device.baseUrl(), e);
            return new ChannelState(0, "Unknown");
        }
    }

    /**
     * Changes the channel on the StingrayTV device.
     *
     * @param channelNumber the channel number to change to
     * @return true if successful, false otherwise
     */
    public boolean changeChannel(int channelNumber) {
        try {
            String baseUrl = device.baseUrl();
            if (baseUrl == null) {
                log.warn("Device base URL is null, cannot change channel");
                return false;
            }

            if (channelNumber < 0) {
                log.warn("Invalid channel number: {}, must be >= 0", channelNumber);
                return false;
            }

            log.debug("Changing channel to '{}' on device at URL: {}", channelNumber, baseUrl + "/channels/current");
            ChannelState channelState = getCurrentChannel();

            Map<String, Object> requestBody = Map.of(
                    "channelNumber", channelNumber,
                    "channelListId", channelState.channelListId()
            );

            restClient.put()
                    .uri(baseUrl + "/channels/current")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Successfully changed channel to '{}' on device at URL: {}", channelNumber, baseUrl);
            return true;
        } catch (Exception e) {
            log.error("Error changing channel to '{}' on StingrayTV device at URL: {}", channelNumber, device.baseUrl(), e);
            return false;
        }
    }

    /**
     * Sends a mute command to the StingrayTV device.
     *
     * @return true if successful, false otherwise
     */
    public boolean mute() {
        try {
            String baseUrl = device.baseUrl();
            if (baseUrl == null) {
                log.warn("Device base URL is null, cannot send mute command");
                return false;
            }

            Map<String, String> requestBody = Map.of("key", "Volume Mute");
            log.debug("Sending mute command to device at URL: {}", baseUrl + "/input/events");

            restClient.post()
                    .uri(baseUrl + "/input/events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Successfully sent mute command to device at URL: {}", baseUrl);
            return true;
        } catch (Exception e) {
            log.error("Error sending mute command to StingrayTV device at URL: {}", device.baseUrl(), e);
            return false;
        }
    }

    /**
     * Sends a play/pause command to the StingrayTV device.
     *
     * @return true if successful, false otherwise
     */
    public boolean pause() {
        try {
            String baseUrl = device.baseUrl();
            if (baseUrl == null) {
                log.warn("Device base URL is null, cannot send pause command");
                return false;
            }

            Map<String, String> requestBody = Map.of("key", "Pause");
            log.debug("Sending pause command to device at URL: {}", baseUrl + "/input/events");

            restClient.post()
                    .uri(baseUrl + "/input/events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Successfully sent pause command to device at URL: {}", baseUrl);
            return true;
        } catch (Exception e) {
            log.error("Error sending pause command to StingrayTV device at URL: {}", device.baseUrl(), e);
            return false;
        }
    }

    public record PowerState(String state) {
    }

    public record VolumeState(int max, int state) {
    }

    public record ChannelState(int channelNumber, String channelListId) {
    }
}
