package ru.oldzoomer.stingraytv_alice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.RequiredArgsConstructor;
import ru.oldzoomer.stingraytv_alice.service.StingrayDeviceDiscoveryService;

/**
 * Configuration for the discovered StingrayTV device.
 * Device discovery runs during context startup and returns null if no device is found.
 * Downstream components handle a null device gracefully.
 */
@Configuration
@RequiredArgsConstructor
public class ReceiverConfig {
    private final StingrayDeviceDiscoveryService stingrayDeviceDiscoveryService;

    @Bean
    StingrayDeviceDiscoveryService.Device detectedStingrayDevice() {
        return stingrayDeviceDiscoveryService.discoverStingrayDevice();
    }
}
