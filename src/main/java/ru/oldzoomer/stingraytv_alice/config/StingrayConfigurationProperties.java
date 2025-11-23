package ru.oldzoomer.stingraytv_alice.config;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for Stingray TV integration
 */
@Getter
@Setter(AccessLevel.PACKAGE)
@Validated
@Component
@ConfigurationProperties(prefix = "app.stingray")
public class StingrayConfigurationProperties {
    @NotBlank(message = "Device description is required")
    private String deviceDescription;

    @NotBlank(message = "Room name is required")
    private String room;

    private String receiverIp;

    private int receiverPort;
}
