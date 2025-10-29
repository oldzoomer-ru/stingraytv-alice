package ru.oldzoomer.stingraytv_alice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.oldzoomer.stingraytv_alice.dto.ClientTokenDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.prefs.Preferences;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PreferencesStorageService
 */
@ExtendWith(MockitoExtension.class)
class PreferencesStorageServiceTest {

    private PreferencesStorageService preferencesStorageService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private Preferences preferences;

    @BeforeEach
    void setUp() {
        preferencesStorageService = new PreferencesStorageService(objectMapper);
        preferencesStorageService.setPreferencesStore(preferences);
    }

    @Test
    void shouldSaveAndRetrieveClientToken() throws JsonProcessingException {
        // Given
        String clientId = "test-client";
        String tokenJson = "{\"clientId\":\"test-client\",\"clientSecret\":\"test-secret\",\"active\":true}";
        ClientTokenDto tokenDto = ClientTokenDto.builder()
                .clientId(clientId)
                .clientSecret("test-secret")
                .createdAt(LocalDateTime.now())
                .lastUsedAt(LocalDateTime.now())
                .active(true)
                .build();

        when(objectMapper.writeValueAsString(tokenDto)).thenReturn(tokenJson);
        when(objectMapper.readValue(tokenJson, ClientTokenDto.class)).thenReturn(tokenDto);
        when(preferences.get(eq("client_token_test-client"), eq(null))).thenReturn(tokenJson);

        // When
        preferencesStorageService.saveClientToken(clientId, tokenDto);
        Optional<ClientTokenDto> retrievedToken = preferencesStorageService.getClientToken(clientId);

        // Then
        assertThat(retrievedToken).isPresent();
        assertThat(retrievedToken.get().getClientId()).isEqualTo(clientId);
        assertThat(retrievedToken.get().getClientSecret()).isEqualTo("test-secret");
        assertThat(retrievedToken.get().isActive()).isTrue();

        verify(preferences).put("client_token_test-client", tokenJson);
        verify(preferences).get("client_token_test-client", null);
    }

    @Test
    void shouldReturnEmptyWhenTokenNotFound() {
        // Given
        when(preferences.get("client_token_non-existent", null)).thenReturn(null);

        // When
        Optional<ClientTokenDto> retrievedToken = preferencesStorageService.getClientToken("non-existent");

        // Then
        assertThat(retrievedToken).isEmpty();
        verify(preferences).get("client_token_non-existent", null);
    }

    @Test
    void shouldDeleteClientToken() {
        // Given
        String clientId = "test-client";

        // When
        preferencesStorageService.deleteClientToken(clientId);

        // Then
        verify(preferences).remove("client_token_test-client");
    }

    @Test
    void shouldGetAllClientTokens() throws Exception {
        // Given
        String[] keys = {"client_token_client-1", "client_token_client-2", "other.key"};
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

        String token1Json = "{\"clientId\":\"client-1\",\"clientSecret\":\"secret-1\",\"active\":true}";
        String token2Json = "{\"clientId\":\"client-2\",\"clientSecret\":\"secret-2\",\"active\":true}";

        when(preferences.keys()).thenReturn(keys);
        when(preferences.get("client_token_client-1", null)).thenReturn(token1Json);
        when(preferences.get("client_token_client-2", null)).thenReturn(token2Json);
        when(objectMapper.readValue(token1Json, ClientTokenDto.class)).thenReturn(token1);
        when(objectMapper.readValue(token2Json, ClientTokenDto.class)).thenReturn(token2);

        // When
        List<ClientTokenDto> allTokens = preferencesStorageService.getAllClientTokens();

        // Then
        assertThat(allTokens).hasSize(2);
        assertThat(allTokens).extracting(ClientTokenDto::getClientId)
                .containsExactlyInAnyOrder("client-1", "client-2");
        verify(preferences).keys();
    }

    @Test
    void shouldUpdateTokenUsage() throws Exception {
        // Given
        String clientId = "test-client";
        String tokenJson = "{\"clientId\":\"test-client\",\"clientSecret\":\"test-secret\",\"active\":true}";
        LocalDateTime originalTime = LocalDateTime.now().minusHours(1);
        ClientTokenDto originalToken = ClientTokenDto.builder()
                .clientId(clientId)
                .clientSecret("test-secret")
                .createdAt(originalTime)
                .lastUsedAt(originalTime)
                .active(true)
                .build();

        when(preferences.get("client_token_test-client", null)).thenReturn(tokenJson);
        when(objectMapper.readValue(tokenJson, ClientTokenDto.class)).thenReturn(originalToken);
        when(objectMapper.writeValueAsString(any(ClientTokenDto.class))).thenReturn(tokenJson);

        // When
        preferencesStorageService.updateTokenUsage(clientId);

        // Then
        verify(preferences).put("client_token_test-client", tokenJson);
        verify(objectMapper).writeValueAsString(argThat(token ->
                ((ClientTokenDto) token).getClientId().equals(clientId) &&
                        ((ClientTokenDto) token).getLastUsedAt().isAfter(originalTime)
        ));
    }

    @Test
    void shouldCheckIfTokenExists() throws JsonProcessingException {
        // Given
        String clientId = "test-client";
        String existingClientId = "existing-client";

        when(preferences.get("client_token_test-client", null)).thenReturn(null);
        String existingTokenJson = "{\"clientId\":\"existing-client\",\"clientSecret\":\"secret\",\"active\":true}";
        ClientTokenDto existingToken = ClientTokenDto.builder()
                .clientId("existing-client")
                .clientSecret("secret")
                .createdAt(LocalDateTime.now())
                .lastUsedAt(LocalDateTime.now())
                .active(true)
                .build();
        when(preferences.get("client_token_existing-client", null)).thenReturn(existingTokenJson);
        when(objectMapper.readValue(existingTokenJson, ClientTokenDto.class)).thenReturn(existingToken);

        // When & Then
        assertThat(preferencesStorageService.hasClientToken(clientId)).isFalse();
        assertThat(preferencesStorageService.hasClientToken(existingClientId)).isTrue();

        verify(preferences).get("client_token_test-client", null);
        verify(preferences).get("client_token_existing-client", null);
    }

    @Test
    void shouldGetClientSecret() throws JsonProcessingException {
        // Given
        String clientId = "test-client";
        String secret = "test-secret";
        String tokenJson = "{\"clientId\":\"test-client\",\"clientSecret\":\"test-secret\",\"active\":true}";
        ClientTokenDto tokenDto = ClientTokenDto.builder()
                .clientId(clientId)
                .clientSecret(secret)
                .createdAt(LocalDateTime.now())
                .lastUsedAt(LocalDateTime.now())
                .active(true)
                .build();

        when(preferences.get("client_token_test-client", null)).thenReturn(tokenJson);
        when(objectMapper.readValue(tokenJson, ClientTokenDto.class)).thenReturn(tokenDto);

        // When
        Optional<String> retrievedSecret = preferencesStorageService.getClientSecret(clientId);

        // Then
        assertThat(retrievedSecret).isPresent();
        assertThat(retrievedSecret.get()).isEqualTo(secret);
        verify(preferences).get("client_token_test-client", null);
        verify(objectMapper).readValue(tokenJson, ClientTokenDto.class);
    }
}
