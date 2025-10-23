package ru.oldzoomer.stingraytv_alice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.oldzoomer.stingraytv_alice.service.StingrayDeviceDiscoveryService;

@Configuration
@RequiredArgsConstructor
public class ReceiverConfig {
    private final StingrayDeviceDiscoveryService stingrayDeviceDiscoveryService;

    @Bean
    public StingrayDeviceDiscoveryService.Device detectedStingrayDevice() {
        return stingrayDeviceDiscoveryService.discoverStingrayDevice();
    }
}
