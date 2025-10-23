package ru.oldzoomer.stingraytv_alice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.oldzoomer.stingraytv_alice.dto.ClientTokenDto;
import ru.oldzoomer.stingraytv_alice.utils.InMemoryPreferences;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.prefs.Preferences;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for PreferencesStorageService
 */
@ExtendWith(MockitoExtension.class)
class PreferencesStorageServiceTest {

    private PreferencesStorageService preferencesStorageService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        preferencesStorageService = new PreferencesStorageService(objectMapper);
        Preferences preferences = new InMemoryPreferences();
        preferencesStorageService.setPreferencesStore(preferences);
    }

    @Test
    void shouldSaveAndRetrieveClientToken() {
        // Given
        String clientId = "test-client";
        ClientTokenDto tokenDto = ClientTokenDto.builder()
                .clientId(clientId)
                .clientSecret("test-secret")
                .createdAt(LocalDateTime.now())
                .lastUsedAt(LocalDateTime.now())
                .active(true)
                .build();

        // When
        preferencesStorageService.saveClientToken(clientId, tokenDto);
        Optional<ClientTokenDto> retrievedToken = preferencesStorageService.getClientToken(clientId);

        // Then
        assertThat(retrievedToken).isPresent();
        assertThat(retrievedToken.get().getClientId()).isEqualTo(clientId);
        assertThat(retrievedToken.get().getClientSecret()).isEqualTo("test-secret");
        assertThat(retrievedToken.get().isActive()).isTrue();
    }

    @Test
    void shouldReturnEmptyWhenTokenNotFound() {
        // When
        Optional<ClientTokenDto> retrievedToken = preferencesStorageService.getClientToken("non-existent");

        // Then
        assertThat(retrievedToken).isEmpty();
    }

    @Test
    void shouldDeleteClientToken() {
        // Given
        String clientId = "test-client";
        ClientTokenDto tokenDto = ClientTokenDto.builder()
                .clientId(clientId)
                .clientSecret("test-secret")
                .createdAt(LocalDateTime.now())
                .lastUsedAt(LocalDateTime.now())
                .active(true)
                .build();

        preferencesStorageService.saveClientToken(clientId, tokenDto);

        // When
        preferencesStorageService.deleteClientToken(clientId);
        Optional<ClientTokenDto> retrievedToken = preferencesStorageService.getClientToken(clientId);

        // Then
        assertThat(retrievedToken).isEmpty();
    }

    @Test
    void shouldGetAllClientTokens() {
        // Given
        ClientTokenDto token1 = ClientTokenDto.builder()
                .clientId("client-1")
                .clientSecret("secret-1")
                .createdAt(LocalDateTime.now())
                .lastUsedAt(LocalDateTime.now())
                .active(true)
                .build();

        ClientTokenDto token2 = ClientTokenDto.builder()
                .clientId("client-2")
                .clientSecret("secret-2")
                .createdAt(LocalDateTime.now())
                .lastUsedAt(LocalDateTime.now())
                .active(true)
                .build();

        preferencesStorageService.saveClientToken("client-1", token1);
        preferencesStorageService.saveClientToken("client-2", token2);

        // When
        List<ClientTokenDto> allTokens = preferencesStorageService.getAllClientTokens();

        // Then
        assertThat(allTokens).hasSize(2);
        assertThat(allTokens).extracting(ClientTokenDto::getClientId)
                .containsExactlyInAnyOrder("client-1", "client-2");
    }

    @Test
    void shouldUpdateTokenUsage() {
        // Given
        String clientId = "test-client";
        LocalDateTime originalTime = LocalDateTime.now().minusHours(1);
        ClientTokenDto tokenDto = ClientTokenDto.builder()
                .clientId(clientId)
                .clientSecret("test-secret")
                .createdAt(originalTime)
                .lastUsedAt(originalTime)
                .active(true)
                .build();

        preferencesStorageService.saveClientToken(clientId, tokenDto);

        // When
        preferencesStorageService.updateTokenUsage(clientId);
        Optional<ClientTokenDto> updatedToken = preferencesStorageService.getClientToken(clientId);

        // Then
        assertThat(updatedToken).isPresent();
        assertThat(updatedToken.get().getLastUsedAt()).isAfter(originalTime);
    }

    @Test
    void shouldCheckIfTokenExists() {
        // Given
        String clientId = "test-client";
        ClientTokenDto tokenDto = ClientTokenDto.builder()
                .clientId(clientId)
                .clientSecret("test-secret")
                .createdAt(LocalDateTime.now())
                .lastUsedAt(LocalDateTime.now())
                .active(true)
                .build();

        // When & Then
        assertThat(preferencesStorageService.hasClientToken(clientId)).isFalse();

        preferencesStorageService.saveClientToken(clientId, tokenDto);
        assertThat(preferencesStorageService.hasClientToken(clientId)).isTrue();
    }

    @Test
    void shouldGetClientSecret() {
        // Given
        String clientId = "test-client";
        String secret = "test-secret";
        ClientTokenDto tokenDto = ClientTokenDto.builder()
                .clientId(clientId)
                .clientSecret(secret)
                .createdAt(LocalDateTime.now())
                .lastUsedAt(LocalDateTime.now())
                .active(true)
                .build();

        preferencesStorageService.saveClientToken(clientId, tokenDto);

        // When
        Optional<String> retrievedSecret = preferencesStorageService.getClientSecret(clientId);

        // Then
        assertThat(retrievedSecret).isPresent();
        assertThat(retrievedSecret.get()).isEqualTo(secret);
    }
}