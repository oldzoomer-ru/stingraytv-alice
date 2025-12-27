package ru.oldzoomer.stingraytv_alice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StingrayTVServiceTest {

    private static final String BASE_URL = "http://192.168.1.100:50000";

    @Mock
    private RetryableRestClient restClient;

    @Mock
    private StingrayDeviceDiscoveryService.Device device;

    @InjectMocks
    private StingrayTVService stingrayTVService;

    @Test
    void getPowerState_WhenDeviceFound_ReturnsPowerState() {
        // Arrange
        when(device.baseUrl()).thenReturn(BASE_URL);
        when(restClient.getWithRetry(BASE_URL + "/power")).thenReturn(Map.of("state", "on"));

        // Act
        StingrayTVService.PowerState result = stingrayTVService.getPowerState();

        // Assert
        assertNotNull(result);
        assertEquals("on", result.getState());
    }

    @Test
    void getPowerState_WhenDeviceNotFound_ReturnsOffline() {
        // Act
        StingrayTVService.PowerState result = stingrayTVService.getPowerState();

        // Assert
        assertNotNull(result);
        assertEquals("offline", result.getState());
    }

    @Test
    void setPowerState_WhenDeviceFound_ReturnsTrue() {
        // Arrange
        when(device.baseUrl()).thenReturn(BASE_URL);

        // Act
        boolean result = stingrayTVService.setPowerState(true);

        // Assert
        assertTrue(result);
    }

    @Test
    void setPowerState_WhenDeviceNotFound_ReturnsFalse() {
        // Act
        boolean result = stingrayTVService.setPowerState(true);

        // Assert
        assertFalse(result);
    }

    @Test
    void getVolumeState_WhenDeviceFound_ReturnsVolumeState() {
        // Arrange
        when(device.baseUrl()).thenReturn(BASE_URL);
        when(restClient.getWithRetry(BASE_URL + "/volume")).thenReturn(Map.of("state", 75));

        // Act
        StingrayTVService.VolumeState result = stingrayTVService.getVolumeState();

        // Assert
        assertNotNull(result);
        assertEquals(75, result.getState());
    }

    @Test
    void setVolume_WithValidVolume_ReturnsTrue() {
        // Arrange
        when(device.baseUrl()).thenReturn(BASE_URL);

        // Act
        boolean result = stingrayTVService.setVolume(50);

        // Assert
        assertTrue(result);
    }

    @Test
    void setVolume_WithInvalidVolume_ThrowsException() {
        // Act & Assert
        assertFalse(stingrayTVService.setVolume(-1));
        assertFalse(stingrayTVService.setVolume(101));
    }

    @Test
    void getCurrentChannel_WhenDeviceFound_ReturnsChannelState() {
        // Arrange
        when(device.baseUrl()).thenReturn(BASE_URL);
        //noinspection unchecked
        when(restClient.getWithRetry(BASE_URL + "/channels/current"))
            .thenReturn(Map.of("channelNumber", 5, "channelListId", "Unknown"));

        // Act
        StingrayTVService.ChannelState result = stingrayTVService.getCurrentChannel();

        // Assert
        assertNotNull(result);
        assertEquals(5, result.getChannelNumber());
        assertEquals("Unknown", result.getChannelListId());
    }

    @Test
    void changeChannel_WithValidChannel_ReturnsTrue() {
        // Arrange
        when(device.baseUrl()).thenReturn(BASE_URL);

        // Act
        boolean result = stingrayTVService.changeChannel(10);

        // Assert
        assertTrue(result);
    }

    @Test
    void changeChannel_WithNegativeChannel_ThrowsException() {
        // Act & Assert
        assertFalse(stingrayTVService.changeChannel(-1));
    }
}