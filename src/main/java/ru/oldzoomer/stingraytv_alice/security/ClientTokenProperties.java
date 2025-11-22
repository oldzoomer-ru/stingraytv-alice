package ru.oldzoomer.stingraytv_alice.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for Client Token management
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.client-token")
public class ClientTokenProperties {

    /**
     * Whether to generate client token automatically on first startup
     */
    private boolean autoGenerate = true;

    /**
     * Token length in characters
     */
    private int tokenLength = 32;

    /**
     * Token prefix for identification
     */
    private String tokenPrefix = "stingray-";
}