package ru.oldzoomer.stingraytv_alice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.RequiredArgsConstructor;
import ru.oldzoomer.stingraytv_alice.service.StingrayDeviceDiscoveryService;

@Configuration
@RequiredArgsConstructor
public class ReceiverConfig {
    private final StingrayDeviceDiscoveryService stingrayDeviceDiscoveryService;

    @Bean
    StingrayDeviceDiscoveryService.Device detectedStingrayDevice() {
        return stingrayDeviceDiscoveryService.discoverStingrayDevice();
    }
}
