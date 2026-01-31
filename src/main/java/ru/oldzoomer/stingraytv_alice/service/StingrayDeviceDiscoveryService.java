package ru.oldzoomer.stingraytv_alice.service;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.oldzoomer.stingraytv_alice.config.StingrayConfigurationProperties;

/**
 * Service for discovering StingrayTV devices on the local network using mDNS.
 * This service handles automatic discovery of StingrayTV receivers on the local network
 * and validates their connectivity.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StingrayDeviceDiscoveryService {

    private final StingrayConfigurationProperties stingrayProperties;

    private static final String STINGRAY_SERVICE_TYPE = "_stingray-remote._tcp.local.";
    private final Map<String, Device> discoveredDevices = new ConcurrentHashMap<>();
    private final RestClient restClient = RestClient.create();

    /**
     * Discover StingrayTV devices on the local network using mDNS.
     * First attempts to use a configured IP address if provided, otherwise performs
     * mDNS discovery to find devices on the network.
     *
     * @return Device object if found, null otherwise
     */
    public Device discoverStingrayDevice() {
        String receiverIp = stingrayProperties.getReceiverIp();
        int receiverPort = stingrayProperties.getReceiverPort();

        if (StringUtils.hasText(receiverIp) && receiverPort >= 0) {
            log.debug("Using configured receiver IP: {} and port: {}", receiverIp, receiverPort);
            Optional<Device> device = getDevice(receiverIp, receiverPort);

            if (device.isPresent()) {
                log.info("Found device using configured IP: {}", device.get());
                return device.get();
            }
        }

        log.info("Starting mDNS discovery for StingrayTV devices...");
        try {
            JmDNS jmdns = JmDNS.create(InetAddress.getLocalHost());
            CountDownLatch latch = new CountDownLatch(1);

            jmdns.addServiceListener(STINGRAY_SERVICE_TYPE, new ServiceListener() {
                @Override
                public void serviceAdded(ServiceEvent event) {
                    log.debug("Service added: {}", event.getName());
                }

                @Override
                public void serviceRemoved(ServiceEvent event) {
                    log.debug("Service removed: {}", event.getName());
                    discoveredDevices.remove(event.getName());
                }

                @Override
                public void serviceResolved(ServiceEvent event) {
                    log.debug("Service resolved: {} at {}:{}",
                            event.getName(),
                            event.getInfo().getHostAddresses()[0],
                            event.getInfo().getPort());

                    Optional<Device> device = getDevice(
                            event.getInfo().getHostAddresses()[0],
                            event.getInfo().getPort()
                    );

                    device.ifPresent(value -> {
                        discoveredDevices.put(event.getName(), value);
                        log.info("Discovered device via mDNS: {}", value);
                    });

                    latch.countDown();
                }
            });

            // Wait for discovery with timeout
            boolean discovered = latch.await(5, TimeUnit.SECONDS);
            jmdns.close();

            if (discovered && !discoveredDevices.isEmpty()) {
                Device firstDevice = discoveredDevices.values().iterator().next();
                log.info("Successfully discovered StingrayTV device via mDNS: {}", firstDevice);
                return firstDevice;
            } else {
                log.warn("No devices discovered via mDNS after timeout");
            }

        } catch (IOException e) {
            log.error("IO error during mDNS discovery", e);
        } catch (InterruptedException e) {
            log.error("Discovery interrupted", e);
            Thread.currentThread().interrupt();
        }

        log.warn("No StingrayTV devices discovered via mDNS or configured IP");
        return null;
    }

    /**
     * Check if device is reachable at the given URL using Spring WebClient.
     * Validates that the device is responding correctly to API requests.
     *
     * @param receiverIp IP address of the device
     * @param receiverPort Port of the device
     * @return Optional Device object if reachable, empty otherwise
     */
    private Optional<Device> getDevice(String receiverIp, int receiverPort) {
        try {
            String baseUrl = String.format("http://%s:%d/v1.6",
                    receiverIp, receiverPort);
            log.debug("Checking device connectivity at URL: {}", baseUrl + "/receiver-info");

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.get()
                    .uri(baseUrl + "/receiver-info")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("userFriendlyModelName") &&
                    response.containsKey("serialNumber")) {
                Device device = new Device(baseUrl, response.get("userFriendlyModelName").toString(),
                        response.get("serialNumber").toString(), response.get("hardwareId").toString(),
                        response.get("softwareVersion").toString());
                log.debug("Successfully validated device at URL: {}", baseUrl);
                return Optional.of(device);
            } else {
                log.warn("Device at {} returned invalid response for receiver-info", baseUrl);
                return Optional.empty();
            }
        } catch (Exception e) {
            log.error("Error getting device info from StingrayTV at URL: {}",
                     String.format("http://%s:%d/v1.6", receiverIp, receiverPort), e);
            return Optional.empty();
        }
    }

    /**
     * Record class representing a discovered StingrayTV device.
     *
     * @param baseUrl base URL of the device API
     * @param model device model name
     * @param serialNumber device serial number
     */
    public record Device(String baseUrl, String model, String serialNumber,
                         String hardwareId, String softwareVersion) {
    }
}