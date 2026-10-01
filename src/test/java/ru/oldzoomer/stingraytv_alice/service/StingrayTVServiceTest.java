package ru.oldzoomer.stingraytv_alice.service;

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

    @SuppressWarnings("unchecked")
    @Test
    void getPowerState_WhenDeviceFound_ReturnsPowerState() {
        // Arrange
        when(device.baseUrl()).thenReturn(BASE_URL);
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(BASE_URL + "/power")).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.accept(any())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(PowerState.class)).thenReturn(new PowerState("on"));

        // Act
        PowerState result = stingrayTVService.getPowerState();

        // Assert
        assertThat(result.state()).isEqualTo("on");
    }

    @Test
    void getPowerState_WhenDeviceNotFound_ReturnsOffline() {
        // Arrange
        when(device.baseUrl()).thenReturn(null);

        // Act
        PowerState result = stingrayTVService.getPowerState();

        // Assert
        assertThat(result.state()).isEqualTo("offline");
    }

    @Test
    void getPowerState_WhenExceptionOccurs_ReturnsOffline() {
        // Arrange
        when(device.baseUrl()).thenReturn(BASE_URL);
        when(restClient.get()).thenThrow(new RuntimeException("Network error"));

        // Act
        PowerState result = stingrayTVService.getPowerState();

        // Assert
        assertThat(result.state()).isEqualTo("offline");
    }

    @Test
    void setPowerState_WhenDeviceFound_ReturnsSuccess() {
        // Arrange
        when(device.baseUrl()).thenReturn(BASE_URL);
        when(restClient.put()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(BASE_URL + "/power")).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.contentType(any())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.body(anyMap())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(ResponseEntity.noContent().build());

        // Act
        StingrayTVService.ActionResult result = stingrayTVService.setPowerState(true);

        // Assert
        assertThat(result.ok()).isTrue();
        assertThat(result.errorMessage()).isNull();
    }

    @Test
    void setPowerState_WhenDeviceNotFound_ReturnsFailure() {
        // Arrange
        when(device.baseUrl()).thenReturn(null);

        // Act
        StingrayTVService.ActionResult result = stingrayTVService.setPowerState(true);

        // Assert
        assertThat(result.ok()).isFalse();
        assertThat(result.errorMessage()).isEqualTo("Device not available");
    }

    @Test
    void setPowerState_WhenExceptionOccurs_ReturnsFailure() {
        // Arrange
        when(device.baseUrl()).thenReturn(BASE_URL);
        when(restClient.put()).thenThrow(new RuntimeException("Network error"));

        // Act
        StingrayTVService.ActionResult result = stingrayTVService.setPowerState(true);

        // Assert
        assertThat(result.ok()).isFalse();
        assertThat(result.errorMessage()).isEqualTo("Error power state");
    }

    @SuppressWarnings("unchecked")
    @Test
    void getVolumeState_WhenDeviceFound_ReturnsVolumeState() {
        // Arrange
        when(device.baseUrl()).thenReturn(BASE_URL);
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(BASE_URL + "/volume")).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.accept(any())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(VolumeState.class)).thenReturn(new VolumeState(20, 75));

        // Act
        VolumeState result = stingrayTVService.getVolumeState();

        // Assert
        assertThat(result.state()).isEqualTo(75);
    }

    @Test
    void getVolumeState_WhenDeviceNotFound_ReturnsZero() {
        // Arrange
        when(device.baseUrl()).thenReturn(null);

        // Act
        VolumeState result = stingrayTVService.getVolumeState();

        // Assert
        assertThat(result.state()).isZero();
    }

    @Test
    void getVolumeState_WhenExceptionOccurs_ReturnsZero() {
        // Arrange
        when(device.baseUrl()).thenReturn(BASE_URL);
        when(restClient.get()).thenThrow(new RuntimeException("Network error"));

        // Act
        VolumeState result = stingrayTVService.getVolumeState();

        // Assert
        assertThat(result.state()).isZero();
    }

    @Test
    void setVolume_WithValidVolume_ReturnsSuccess() {
        // Arrange
        when(device.baseUrl()).thenReturn(BASE_URL);
        when(restClient.put()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(BASE_URL + "/volume")).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.contentType(any())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.body(anyMap())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(ResponseEntity.noContent().build());

        // Act
        StingrayTVService.ActionResult result = stingrayTVService.setVolume(10);

        // Assert
        assertThat(result.ok()).isTrue();
    }

    @Test
    void setVolume_WithNegativeVolume_ReturnsFailure() {
        // Act
        StingrayTVService.ActionResult result = stingrayTVService.setVolume(-1);

        // Assert
        assertThat(result.ok()).isFalse();
        assertThat(result.errorMessage()).isEqualTo("Volume must be between 0 and 20");
    }

    @Test
    void setVolume_WithOverMaxVolume_ReturnsFailure() {
        // Act
        StingrayTVService.ActionResult result = stingrayTVService.setVolume(21);

        // Assert
        assertThat(result.ok()).isFalse();
        assertThat(result.errorMessage()).isEqualTo("Volume must be between 0 and 20");
    }

    @Test
    void setVolume_WhenDeviceNotFound_ReturnsFailure() {
        // Arrange
        when(device.baseUrl()).thenReturn(null);

        // Act
        StingrayTVService.ActionResult result = stingrayTVService.setVolume(10);

        // Assert
        assertThat(result.ok()).isFalse();
        assertThat(result.errorMessage()).isEqualTo("Device not available");
    }

    @Test
    void setVolume_WhenExceptionOccurs_ReturnsFailure() {
        // Arrange
        when(device.baseUrl()).thenReturn(BASE_URL);
        when(restClient.put()).thenThrow(new RuntimeException("Network error"));

        // Act
        StingrayTVService.ActionResult result = stingrayTVService.setVolume(10);

        // Assert
        assertThat(result.ok()).isFalse();
        assertThat(result.errorMessage()).isEqualTo("Error volume");
    }

    @SuppressWarnings("unchecked")
	@Test
    void getCurrentChannel_WhenDeviceFound_ReturnsChannelState() {
        // Arrange
        when(device.baseUrl()).thenReturn(BASE_URL);
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(BASE_URL + "/channels/current")).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.accept(any())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        ChannelState[] channels = new ChannelState[]{new ChannelState(5, "list-1")};
        when(responseSpec.toEntity(ChannelState[].class)).thenReturn(ResponseEntity.ok(channels));

        // Act
        ChannelState result = stingrayTVService.getCurrentChannel();

        // Assert
        assertThat(result.channelNumber()).isEqualTo(5);
        assertThat(result.channelListId()).isEqualTo("list-1");
    }

    @Test
    void getCurrentChannel_WhenDeviceNotFound_ReturnsDefaultValues() {
        // Arrange
        when(device.baseUrl()).thenReturn(null);

        // Act
        ChannelState result = stingrayTVService.getCurrentChannel();

        // Assert
        assertThat(result.channelNumber()).isZero();
        assertThat(result.channelListId()).isEqualTo("Unknown");
    }

    @Test
    void getCurrentChannel_WhenExceptionOccurs_ReturnsDefaultValues() {
        // Arrange
        when(device.baseUrl()).thenReturn(BASE_URL);
        when(restClient.get()).thenThrow(new RuntimeException("Network error"));

        // Act
        ChannelState result = stingrayTVService.getCurrentChannel();

        // Assert
        assertThat(result.channelNumber()).isZero();
        assertThat(result.channelListId()).isEqualTo("Unknown");
    }

    @SuppressWarnings("unchecked")
	@Test
    void changeChannel_WithValidChannel_ReturnsSuccess() {
        // Arrange
        when(device.baseUrl()).thenReturn(BASE_URL);
        // Mock getCurrentChannel call (used internally by changeChannel)
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(BASE_URL + "/channels/current")).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.accept(any())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        ChannelState[] channels = new ChannelState[]{new ChannelState(1, "list-1")};
        when(responseSpec.toEntity(ChannelState[].class)).thenReturn(ResponseEntity.ok(channels));
        // Mock the PUT call
        when(restClient.put()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(BASE_URL + "/channels/current")).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.contentType(any())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.body(anyMap())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(ResponseEntity.noContent().build());

        // Act
        StingrayTVService.ActionResult result = stingrayTVService.changeChannel(10);

        // Assert
        assertThat(result.ok()).isTrue();
    }

    @Test
    void changeChannel_WithNegativeChannel_ReturnsFailure() {
        // Act
        StingrayTVService.ActionResult result = stingrayTVService.changeChannel(-1);

        // Assert
        assertThat(result.ok()).isFalse();
        assertThat(result.errorMessage()).isEqualTo("Channel number must be between 0 and 9999");
    }

    @Test
    void changeChannel_WithOverMaxChannel_ReturnsFailure() {
        // Act
        StingrayTVService.ActionResult result = stingrayTVService.changeChannel(10000);

        // Assert
        assertThat(result.ok()).isFalse();
        assertThat(result.errorMessage()).isEqualTo("Channel number must be between 0 and 9999");
    }

    @Test
    void changeChannel_WhenDeviceNotFound_ReturnsFailure() {
        // Arrange
        when(device.baseUrl()).thenReturn(null);

        // Act
        StingrayTVService.ActionResult result = stingrayTVService.changeChannel(10);

        // Assert
        assertThat(result.ok()).isFalse();
        assertThat(result.errorMessage()).isEqualTo("Device not available");
    }

    @Test
    void changeChannel_WhenExceptionOccurs_ReturnsFailure() {
        // Arrange
        when(device.baseUrl()).thenReturn(BASE_URL);
        when(restClient.put()).thenThrow(new RuntimeException("Network error"));

        // Act
        StingrayTVService.ActionResult result = stingrayTVService.changeChannel(10);

        // Assert
        assertThat(result.ok()).isFalse();
        assertThat(result.errorMessage()).isEqualTo("Error channel");
    }

    @Test
    void mute_WhenDeviceFound_ReturnsSuccess() {
        // Arrange
        when(device.baseUrl()).thenReturn(BASE_URL);
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(BASE_URL + "/input/events")).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.contentType(any())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.body(anyMap())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(ResponseEntity.noContent().build());

        // Act
        StingrayTVService.ActionResult result = stingrayTVService.mute();

        // Assert
        assertThat(result.ok()).isTrue();
    }

    @Test
    void mute_WhenDeviceNotFound_ReturnsFailure() {
        // Arrange
        when(device.baseUrl()).thenReturn(null);

        // Act
        StingrayTVService.ActionResult result = stingrayTVService.mute();

        // Assert
        assertThat(result.ok()).isFalse();
        assertThat(result.errorMessage()).isEqualTo("Device not available");
    }

    @Test
    void pause_WhenDeviceFound_ReturnsSuccess() {
        // Arrange
        when(device.baseUrl()).thenReturn(BASE_URL);
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(BASE_URL + "/input/events")).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.contentType(any())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.body(anyMap())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(ResponseEntity.noContent().build());

        // Act
        StingrayTVService.ActionResult result = stingrayTVService.pause();

        // Assert
        assertThat(result.ok()).isTrue();
    }

    @Test
    void pause_WhenDeviceNotFound_ReturnsFailure() {
        // Arrange
        when(device.baseUrl()).thenReturn(null);

        // Act
        StingrayTVService.ActionResult result = stingrayTVService.pause();

        // Assert
        assertThat(result.ok()).isFalse();
        assertThat(result.errorMessage()).isEqualTo("Device not available");
    }
}
