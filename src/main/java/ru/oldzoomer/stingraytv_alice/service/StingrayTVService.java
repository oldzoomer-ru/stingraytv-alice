package ru.oldzoomer.stingraytv_alice.service;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class StingrayTVService {

    private final RestClient restClient;
    private final WebClient webClient;
    private final StingrayDeviceDiscoveryService.Device device;

    public PowerState getPowerState() {
        try {
            String baseUrl = device.baseUrl();
            if (baseUrl == null) {
                return PowerState.builder().state("offline").build();
            }

            PowerState response = restClient.get()
                    .uri(baseUrl + "/power")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(PowerState.class);

            if (response != null && response.state != null) {
                return response;
            } else {
                return PowerState.builder()
                        .state("offline")
                        .build();
            }
        } catch (Exception e) {
            log.error("Error getting power state from StingrayTV", e);
            return PowerState.builder()
                    .state("offline")
                    .build();
        }
    }

    public boolean setPowerState(boolean powerOn) {
        try {
            String baseUrl = device.baseUrl();
            if (baseUrl == null) {
                return false;
            }

            String powerState = powerOn ? "on" : "off";
            Map<String, String> requestBody = Map.of("state", powerState);

            restClient.put()
                    .uri(baseUrl + "/power")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .toBodilessEntity();

            return true;
        } catch (Exception e) {
            log.error("Error setting power state on StingrayTV", e);
            return false;
        }
    }

    public VolumeState getVolumeState() {
        try {
            String baseUrl = device.baseUrl();
            if (baseUrl == null) {
                return VolumeState.builder().state(0).build();
            }

            VolumeState response = restClient.get()
                    .uri(baseUrl + "/volume")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(VolumeState.class);

            if (response != null) {
                return response;
            } else {
                return VolumeState.builder()
                        .state(0)
                        .build();
            }
        } catch (Exception e) {
            log.error("Error getting volume state from StingrayTV", e);
            return VolumeState.builder()
                    .state(0)
                    .build();
        }
    }

    public boolean setVolume(int volume) {
        try {
            String baseUrl = device.baseUrl();
            if (baseUrl == null) {
                return false;
            }

            Map<String, Integer> requestBody = Map.of("state", volume);

            restClient.put()
                    .uri(baseUrl + "/volume")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .toBodilessEntity();

            return true;
        } catch (Exception e) {
            log.error("Error setting volume on StingrayTV", e);
            return false;
        }
    }

    public ChannelState getCurrentChannel() {
        try {
            String baseUrl = device.baseUrl();
            if (baseUrl == null) {
                return ChannelState.builder().channelNumber(0).channelListId("Unknown").build();
            }

            ChannelState response = webClient.get()
                    .uri(baseUrl + "/channels/current")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToFlux(ChannelState.class)
                    .blockFirst();

            if (response != null) {
                return response;
            } else {
                return ChannelState.builder()
                        .channelNumber(0)
                        .channelListId("Unknown")
                        .build();
            }
        } catch (Exception e) {
            log.error("Error getting current channel from StingrayTV", e);
            return ChannelState.builder()
                    .channelNumber(0)
                    .channelListId("Unknown")
                    .build();
        }
    }

    public boolean changeChannel(int channelNumber) {
        try {
            String baseUrl = device.baseUrl();
            if (baseUrl == null) {
                return false;
            }

            if (channelNumber < 0) {
                return false;
            }

            ChannelState channelState = getCurrentChannel();

            Map<String, Object> requestBody = Map.of(
                    "channelNumber", channelNumber,
                    "channelListId", channelState.getChannelListId()
            );

            restClient.put()
                    .uri(baseUrl + "/channels/current")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .toBodilessEntity();

            return true;
        } catch (Exception e) {
            log.error("Error changing channel on StingrayTV", e);
            return false;
        }
    }

    @Builder
    @AllArgsConstructor
    @Data
    public static class PowerState {
        private String state;
    }

    @Builder
    @AllArgsConstructor
    @Data
    public static class VolumeState {
        private int max;
        private int state;
    }

    @Builder
    @AllArgsConstructor
    @Data
    public static class ChannelState {
        private int channelNumber;
        private String channelListId;
    }
}
