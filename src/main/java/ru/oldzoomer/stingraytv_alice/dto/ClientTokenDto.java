package ru.oldzoomer.stingraytv_alice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for client token storage in Preferences API
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientTokenDto {

    /**
     * Client ID
     */
    private String clientId;

    /**
     * Client secret (encrypted)
     */
    private String clientSecret;

    /**
     * Token creation timestamp
     */
    private LocalDateTime createdAt;

    /**
     * Token last usage timestamp
     */
    private LocalDateTime lastUsedAt;

    /**
     * Whether the token is active
     */
    private boolean active;
}