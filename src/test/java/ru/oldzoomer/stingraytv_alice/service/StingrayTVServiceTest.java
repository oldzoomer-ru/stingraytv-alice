package ru.oldzoomer.stingraytv_alice.service;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import ru.oldzoomer.stingraytv_alice.service.StingrayTVService.ChannelState;
import ru.oldzoomer.stingraytv_alice.service.StingrayTVService.PowerState;
import ru.oldzoomer.stingraytv_alice.service.StingrayTVService.VolumeState;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StingrayTVServiceTest {

    private static final String BASE_URL = "http://192.168.1.100:50000";

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @SuppressWarnings("rawtypes")
    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    @Mock
    private StingrayDeviceDiscoveryService.Device device;

    @InjectMocks
    private StingrayTVService stingrayTVService;

    @Test
    void getPowerState_WhenDeviceFound_ReturnsPowerState() {
        // Arrange
        when(device.baseUrl()).thenReturn(BASE_URL);
        //noinspection unchecked
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(BASE_URL + "/power")).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.accept(any())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(PowerState.class)).thenReturn(PowerState.builder().state("on").build());

        // Act
        PowerState result = stingrayTVService.getPowerState();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getState()).isEqualTo("on");
    }

    @Test
    void getPowerState_WhenDeviceNotFound_ReturnsOffline() {
        // Arrange
        when(device.baseUrl()).thenReturn(null);

        // Act
        PowerState result = stingrayTVService.getPowerState();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getState()).isEqualTo("offline");
    }

    @Test
    void getPowerState_WhenExceptionOccurs_ReturnsOffline() {
        // Arrange
        when(device.baseUrl()).thenReturn(BASE_URL);
        when(restClient.get()).thenThrow(new RuntimeException("Network error"));

        // Act
        PowerState result = stingrayTVService.getPowerState();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getState()).isEqualTo("offline");
    }

    @Test
    void setPowerState_WhenDeviceFound_ReturnsTrue() {
        // Arrange
        when(device.baseUrl()).thenReturn(BASE_URL);
        when(restClient.put()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(BASE_URL + "/power")).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.contentType(any())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.body(anyMap())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(ResponseEntity.noContent().build());

        // Act
        boolean result = stingrayTVService.setPowerState(true);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void setPowerState_WhenDeviceNotFound_ReturnsFalse() {
        // Arrange
        when(device.baseUrl()).thenReturn(null);

        // Act
        boolean result = stingrayTVService.setPowerState(true);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void setPowerState_WhenExceptionOccurs_ReturnsFalse() {
        // Arrange
        when(device.baseUrl()).thenReturn(BASE_URL);
        when(restClient.put()).thenThrow(new RuntimeException("Network error"));

        // Act
        boolean result = stingrayTVService.setPowerState(true);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void getVolumeState_WhenDeviceFound_ReturnsVolumeState() {
        // Arrange
        when(device.baseUrl()).thenReturn(BASE_URL);
        //noinspection unchecked
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(BASE_URL + "/volume")).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.accept(any())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(VolumeState.class)).thenReturn(VolumeState.builder().state(75).build());

        // Act
        VolumeState result = stingrayTVService.getVolumeState();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getState()).isEqualTo(75);
    }

    @Test
    void getVolumeState_WhenDeviceNotFound_ReturnsZero() {
        // Arrange
        when(device.baseUrl()).thenReturn(null);

        // Act
        VolumeState result = stingrayTVService.getVolumeState();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getState()).isZero();
    }

    @Test
    void getVolumeState_WhenExceptionOccurs_ReturnsZero() {
        // Arrange
        when(device.baseUrl()).thenReturn(BASE_URL);
        when(restClient.get()).thenThrow(new RuntimeException("Network error"));

        // Act
        VolumeState result = stingrayTVService.getVolumeState();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getState()).isZero();
    }

    @Test
    void setVolume_WithValidVolume_ReturnsTrue() {
        // Arrange
        when(device.baseUrl()).thenReturn(BASE_URL);
        when(restClient.put()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(BASE_URL + "/volume")).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.contentType(any())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.body(anyMap())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(ResponseEntity.noContent().build());

        // Act
        boolean result = stingrayTVService.setVolume(50);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void setVolume_WithInvalidVolume_ReturnsFalse() {
        // Act & Assert
        assertThat(stingrayTVService.setVolume(-1)).isFalse();
        assertThat(stingrayTVService.setVolume(101)).isFalse();
    }

    @Test
    void setVolume_WhenDeviceNotFound_ReturnsFalse() {
        // Arrange
        when(device.baseUrl()).thenReturn(null);

        // Act
        boolean result = stingrayTVService.setVolume(50);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void setVolume_WhenExceptionOccurs_ReturnsFalse() {
        // Arrange
        when(device.baseUrl()).thenReturn(BASE_URL);
        when(restClient.put()).thenThrow(new RuntimeException("Network error"));

        // Act
        boolean result = stingrayTVService.setVolume(50);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @Disabled("Disabled for now, because it's not possible to mock the WebClient")
    void getCurrentChannel_WhenDeviceFound_ReturnsChannelState() {
        // Arrange
        when(device.baseUrl()).thenReturn(BASE_URL);
        //noinspection unchecked
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(BASE_URL + "/channels/current")).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.accept(any())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(ChannelState.class)).thenReturn(ChannelState.builder()
                .channelNumber(5).channelListId("Unknown").build());

        // Act
        ChannelState result = stingrayTVService.getCurrentChannel();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getChannelNumber()).isEqualTo(5);
        assertThat(result.getChannelListId()).isEqualTo("Unknown");
    }

    @Test
    void getCurrentChannel_WhenDeviceNotFound_ReturnsDefaultValues() {
        // Arrange
        when(device.baseUrl()).thenReturn(null);

        // Act
        ChannelState result = stingrayTVService.getCurrentChannel();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getChannelNumber()).isZero();
        assertThat(result.getChannelListId()).isEqualTo("Unknown");
    }

    @Test
    void getCurrentChannel_WhenExceptionOccurs_ReturnsDefaultValues() {
        // Arrange
        when(device.baseUrl()).thenReturn(BASE_URL);

        // Act
        ChannelState result = stingrayTVService.getCurrentChannel();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getChannelNumber()).isZero();
        assertThat(result.getChannelListId()).isEqualTo("Unknown");
    }

    @Test
    void changeChannel_WithValidChannel_ReturnsTrue() {
        // Arrange
        when(device.baseUrl()).thenReturn(BASE_URL);
        when(restClient.put()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(BASE_URL + "/channels/current")).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.contentType(any())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.body(anyMap())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(ResponseEntity.noContent().build());

        // Act
        boolean result = stingrayTVService.changeChannel(10);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void changeChannel_WithNegativeChannel_ReturnsFalse() {
        // Act & Assert
        assertThat(stingrayTVService.changeChannel(-1)).isFalse();
    }

    @Test
    void changeChannel_WhenDeviceNotFound_ReturnsFalse() {
        // Arrange
        when(device.baseUrl()).thenReturn(null);

        // Act
        boolean result = stingrayTVService.changeChannel(10);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void changeChannel_WhenExceptionOccurs_ReturnsFalse() {
        // Arrange
        when(device.baseUrl()).thenReturn(BASE_URL);
        when(restClient.put()).thenThrow(new RuntimeException("Network error"));

        // Act
        boolean result = stingrayTVService.changeChannel(10);

        // Assert
        assertThat(result).isFalse();
    }
}