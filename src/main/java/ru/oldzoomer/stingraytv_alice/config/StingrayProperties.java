package ru.oldzoomer.stingraytv_alice.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for Stingray TV integration
 */
@Data
@Validated
@Component
@ConfigurationProperties(prefix = "app.stingray")
public class StingrayProperties {

    @NotBlank(message = "Device ID is required")
    private String deviceId;

    @NotBlank(message = "Device name is required")
    private String deviceName;

    @NotBlank(message = "Device description is required")
    private String deviceDescription;

    @NotBlank(message = "Room name is required")
    private String room;

    private String receiverIp = "";

    private int receiverPort = -1;
}