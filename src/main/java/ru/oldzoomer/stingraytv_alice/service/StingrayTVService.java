package ru.oldzoomer.stingraytv_alice.service;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Service;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class StingrayTVService {

    private final RetryableRestClient retryableRestClient;
    private final StingrayDeviceDiscoveryService.Device device;
    
    // Кэширование состояний устройств для уменьшения нагрузки на устройства
    private final AtomicReference<PowerState> cachedPowerState = new AtomicReference<>();
    private final AtomicReference<VolumeState> cachedVolumeState = new AtomicReference<>();
    private final AtomicReference<ChannelState> cachedChannelState = new AtomicReference<>();
    
    // Время кэширования в миллисекундах (5 секунд)
    private static final long CACHE_TIMEOUT = 5000;
    private volatile long lastPowerStateUpdate = 0;
    private volatile long lastVolumeStateUpdate = 0;
    private volatile long lastChannelStateUpdate = 0;
    
    public PowerState getPowerState() {
        long now = System.currentTimeMillis();
        
        // Проверяем, не истек ли срок действия кэша
        if (cachedPowerState.get() != null && (now - lastPowerStateUpdate) < CACHE_TIMEOUT) {
            log.debug("Returning cached power state");
            return cachedPowerState.get();
        }
        
        try {
            String baseUrl = device.baseUrl();
            if (baseUrl == null) {
                PowerState offlineState = PowerState.builder().state("offline").build();
                cachedPowerState.set(offlineState);
                lastPowerStateUpdate = now;
                return offlineState;
            }

            Map<String, Object> response = retryableRestClient.getWithRetry(
                    baseUrl + "/power");

            if (response != null && response.containsKey("state")) {
                PowerState powerState = PowerState.builder()
                        .state(response.get("state").toString())
                        .build();
                cachedPowerState.set(powerState);
                lastPowerStateUpdate = now;
                return powerState;
            } else {
                PowerState offlineState = PowerState.builder()
                        .state("offline")
                        .build();
                cachedPowerState.set(offlineState);
                lastPowerStateUpdate = now;
                return offlineState;
            }
        } catch (Exception e) {
            log.error("Error getting power state from StingrayTV", e);
            PowerState offlineState = PowerState.builder()
                    .state("offline")
                    .build();
            cachedPowerState.set(offlineState);
            lastPowerStateUpdate = now;
            return offlineState;
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

            retryableRestClient.putWithRetry(
                    baseUrl + "/power", requestBody);
            return true;
        } catch (Exception e) {
            log.error("Error setting power state on StingrayTV", e);
            return false;
        }
    }

    public VolumeState getVolumeState() {
        long now = System.currentTimeMillis();
        
        // Проверяем, не истек ли срок действия кэша
        if (cachedVolumeState.get() != null && (now - lastVolumeStateUpdate) < CACHE_TIMEOUT) {
            log.debug("Returning cached volume state");
            return cachedVolumeState.get();
        }
        
        try {
            String baseUrl = device.baseUrl();
            if (baseUrl == null) {
                VolumeState defaultState = VolumeState.builder().state(0).build();
                cachedVolumeState.set(defaultState);
                lastVolumeStateUpdate = now;
                return defaultState;
            }

            Map<String, Object> response = retryableRestClient.getWithRetry(
                    baseUrl + "/volume");

            if (response != null) {
                int volume = response.containsKey("state") ?
                        Integer.parseInt(response.get("state").toString()) : 0;

                VolumeState volumeState = VolumeState.builder()
                        .state(volume)
                        .build();
                cachedVolumeState.set(volumeState);
                lastVolumeStateUpdate = now;
                return volumeState;
            } else {
                VolumeState defaultState = VolumeState.builder()
                        .state(0)
                        .build();
                cachedVolumeState.set(defaultState);
                lastVolumeStateUpdate = now;
                return defaultState;
            }
        } catch (Exception e) {
            log.error("Error getting volume state from StingrayTV", e);
            VolumeState defaultState = VolumeState.builder()
                    .state(0)
                    .build();
            cachedVolumeState.set(defaultState);
            lastVolumeStateUpdate = now;
            return defaultState;
        }
    }

    public boolean setVolume(int volume) {
        try {
            String baseUrl = device.baseUrl();
            if (baseUrl == null) {
                return false;
            }

            Map<String, Integer> requestBody = Map.of("state", volume);

            retryableRestClient.putWithRetry(
                    baseUrl + "/volume", requestBody);
            return true;
        } catch (Exception e) {
            log.error("Error setting volume on StingrayTV", e);
            return false;
        }
    }

    public ChannelState getCurrentChannel() {
        long now = System.currentTimeMillis();
        
        // Проверяем, не истек ли срок действия кэша
        if (cachedChannelState.get() != null && (now - lastChannelStateUpdate) < CACHE_TIMEOUT) {
            log.debug("Returning cached channel state");
            return cachedChannelState.get();
        }
        
        try {
            String baseUrl = device.baseUrl();
            if (baseUrl == null) {
                ChannelState defaultState = ChannelState.builder().channelNumber(0).channelListId("Unknown").build();
                cachedChannelState.set(defaultState);
                lastChannelStateUpdate = now;
                return defaultState;
            }

            Map<String, Object> response = retryableRestClient.getWithRetry(
                    baseUrl + "/channels/current");
            if (response != null) {
                int channelNumber = response.containsKey("channelNumber") ?
                        Integer.parseInt(response.get("channelNumber").toString()) : 0;
                String channelListId = response.containsKey("channelListId") ?
                        response.get("channelListId").toString() : "Unknown";

                ChannelState channelState = ChannelState.builder()
                        .channelNumber(channelNumber)
                        .channelListId(channelListId)
                        .build();
                cachedChannelState.set(channelState);
                lastChannelStateUpdate = now;
                return channelState;
            } else {
                ChannelState defaultState = ChannelState.builder()
                        .channelNumber(0)
                        .channelListId("Unknown")
                        .build();
                cachedChannelState.set(defaultState);
                lastChannelStateUpdate = now;
                return defaultState;
            }
        } catch (Exception e) {
            log.error("Error getting current channel from StingrayTV", e);
            ChannelState defaultState = ChannelState.builder()
                    .channelNumber(0)
                    .channelListId("Unknown")
                    .build();
            cachedChannelState.set(defaultState);
            lastChannelStateUpdate = now;
            return defaultState;
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

            retryableRestClient.putWithRetry(
                    baseUrl + "/channels/current", requestBody);
            return true;
        } catch (Exception e) {
            log.error("Error changing channel on StingrayTV", e);
            return false;
        }
    }

    @Builder
    @Data
    public static class PowerState {
        private String state;
    }

    @Builder
    @Data
    public static class VolumeState {
        private int max;
        private int state;
    }

    @Builder
    @Data
    public static class ChannelState {
        private int channelNumber;
        private String channelListId;
    }
}
