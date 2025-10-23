package ru.oldzoomer.stingraytv_alice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.oldzoomer.stingraytv_alice.dto.ClientTokenDto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Service for storing client tokens in Preferences API
 * Provides persistent storage for OAuth2 client credentials
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PreferencesStorageService {

    /**
     * Key prefix for client tokens in Preferences API
     */
    private static final String CLIENT_TOKEN_PREFIX = "client_token_";
    private final ObjectMapper objectMapper;
    /**
     * In-memory storage simulating Preferences API
     * In production, this would be replaced with actual Preferences API calls
     */
    @Setter(AccessLevel.PACKAGE)
    private Preferences preferencesStore = Preferences.userRoot()
            .node("stingraytv_alice");

    /**
     * Save client token to Preferences API
     *
     * @param clientId       the client identifier
     * @param clientTokenDto the client token data
     */
    public void saveClientToken(String clientId, ClientTokenDto clientTokenDto) {
        try {
            String key = CLIENT_TOKEN_PREFIX + clientId;
            String value = objectMapper.writeValueAsString(clientTokenDto);

            preferencesStore.put(key, value);
            log.debug("Saved client token for client: {}", clientId);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize client token for client: {}", clientId, e);
            throw new RuntimeException("Failed to save client token", e);
        }
    }

    /**
     * Get client token from Preferences API
     *
     * @param clientId the client identifier
     * @return Optional containing the client token if found
     */
    public Optional<ClientTokenDto> getClientToken(String clientId) {
        String key = CLIENT_TOKEN_PREFIX + clientId;
        String value = preferencesStore.get(key, null);

        if (value == null) {
            return Optional.empty();
        }

        try {
            ClientTokenDto tokenDto = objectMapper.readValue(value, ClientTokenDto.class);
            return Optional.of(tokenDto);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize client token for client: {}", clientId, e);
            return Optional.empty();
        }
    }

    /**
     * Delete client token from Preferences API
     *
     * @param clientId the client identifier
     */
    public void deleteClientToken(String clientId) {
        String key = CLIENT_TOKEN_PREFIX + clientId;
        preferencesStore.remove(key);
        log.debug("Deleted client token for client: {}", clientId);
    }

    /**
     * Get all client tokens from Preferences API
     *
     * @return list of all client tokens
     */
    public List<ClientTokenDto> getAllClientTokens() {
        List<ClientTokenDto> tokens = new ArrayList<>();

        try {
            Arrays.stream(preferencesStore.keys())
                    .filter(entry -> entry.startsWith(CLIENT_TOKEN_PREFIX))
                    .forEach(entry -> {
                        try {
                            ClientTokenDto tokenDto = objectMapper.readValue(
                                    preferencesStore.get(entry, null), ClientTokenDto.class);
                            tokens.add(tokenDto);
                        } catch (JsonProcessingException e) {
                            log.error("Failed to deserialize client token for key: {}", entry, e);
                        }
                    });

            return tokens;
        } catch (BackingStoreException e) {
            log.error("Preferences error: ", e);
            return tokens;
        }
    }

    /**
     * Update token usage timestamp
     *
     * @param clientId the client identifier
     */
    public void updateTokenUsage(String clientId) {
        getClientToken(clientId).ifPresent(tokenDto -> {
            tokenDto.setLastUsedAt(LocalDateTime.now());
            saveClientToken(clientId, tokenDto);
            log.debug("Updated usage timestamp for client: {}", clientId);
        });
    }

    /**
     * Check if client token exists
     *
     * @param clientId the client identifier
     * @return true if token exists and is active
     */
    public boolean hasClientToken(String clientId) {
        return getClientToken(clientId)
                .map(ClientTokenDto::isActive)
                .orElse(false);
    }

    /**
     * Get client secret
     *
     * @param clientId the client identifier
     * @return Optional containing the client secret if found
     */
    public Optional<String> getClientSecret(String clientId) {
        return getClientToken(clientId)
                .map(ClientTokenDto::getClientSecret);
    }
}