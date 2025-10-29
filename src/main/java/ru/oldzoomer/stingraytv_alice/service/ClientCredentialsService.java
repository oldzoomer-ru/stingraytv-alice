package ru.oldzoomer.stingraytv_alice.service;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Service;
import ru.oldzoomer.stingraytv_alice.config.ClientTokenProperties;
import ru.oldzoomer.stingraytv_alice.config.OAuthProperties;
import ru.oldzoomer.stingraytv_alice.dto.ClientTokenDto;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for managing OAuth2 Client Credentials according to RFC 6749
 * Implements Client Credentials Grant (Section 4.4)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClientCredentialsService {

    private final OAuthProperties authProperties;
    private final ClientTokenProperties clientTokenProperties;
    private final PreferencesStorageService preferencesStorageService;

    /**
     * -- GETTER --
     * Get client secret (confidential)
     */
    @Getter
    private final Map<OAuthProperties.Client, String> clientSecret = new HashMap<>();

    @PostConstruct
    public void init() {
        generateClientCredentials();
    }

    /**
     * Generate client credentials according to RFC 6749
     */
    private void generateClientCredentials() {
        for (OAuthProperties.Client client : authProperties.getClients()) {
            String clientId = client.getClientId();

            // Check if credentials already exist in Preferences API
            if (preferencesStorageService.hasClientToken(clientId)) {
                log.info("Using existing client credentials for client: {}", clientId);
                preferencesStorageService.getClientSecret(clientId).ifPresent(secret -> clientSecret.put(client, secret));
            } else {
                // Generate new secure client secret
                SecureRandom random = new SecureRandom();
                byte[] bytes = new byte[clientTokenProperties.getTokenLength()];
                random.nextBytes(bytes);

                String secret = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
                clientSecret.put(client, secret);

                // Store in Preferences API
                ClientTokenDto tokenDto = ClientTokenDto.builder()
                        .clientId(clientId)
                        .clientSecret(secret)
                        .createdAt(LocalDateTime.now())
                        .lastUsedAt(LocalDateTime.now())
                        .active(true)
                        .build();

                preferencesStorageService.saveClientToken(clientId, tokenDto);

                log.info("Generated and stored OAuth2 Client Credentials:");
                log.info("Client ID: {}", clientId);
                log.info("Client Secret: {} (stored in Preferences API)", secret);
                log.info("Use these credentials with Client Credentials grant type");
                log.info("Token endpoint: /oauth2/token");
                log.info("Grant type: client_credentials");
            }
        }
    }

    /**
     * Get client ID (public identifier)
     */
    public String getClientId(OAuthProperties.Client client) {
        return client.getClientId();
    }

    /**
     * Check if client credentials are available
     */
    public boolean hasCredentials(OAuthProperties.Client client) {
        return preferencesStorageService.hasClientToken(client.getClientId()) ||
                (clientSecret.containsKey(client) && !clientSecret.get(client).trim().isEmpty());
    }

    /**
     * Regenerate client credentials
     */
    public void regenerateCredentials() {
        // Clear existing credentials
        clientSecret.clear();

        // Delete existing tokens from Preferences API
        for (OAuthProperties.Client client : authProperties.getClients()) {
            preferencesStorageService.deleteClientToken(client.getClientId());
        }

        // Generate new credentials
        generateClientCredentials();
        log.info("Regenerated OAuth2 Client Credentials");
    }

    /**
     * Create registered client for OAuth2 Authorization Server
     */
    public RegisteredClient createRegisteredClient(OAuthProperties.Client client) {
        String clientId = client.getClientId();
        String secret = clientSecret.get(client);

        // If not in memory, try to get from Preferences API
        if (secret == null) {
            secret = preferencesStorageService.getClientSecret(clientId)
                    .orElseThrow(() -> new IllegalStateException("No client secret found for client: " + clientId));
        }

        // Update usage timestamp
        preferencesStorageService.updateTokenUsage(clientId);

        return RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(clientId)
                .clientSecret(new BCryptPasswordEncoder().encode(secret))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope("device:read")
                .scope("device:write")
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofHours(1))
                        .build())
                .clientSettings(ClientSettings.builder()
                        .requireAuthorizationConsent(false)
                        .build())
                .build();
    }
}