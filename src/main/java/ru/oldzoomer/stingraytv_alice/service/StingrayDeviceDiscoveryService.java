package ru.oldzoomer.stingraytv_alice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import ru.oldzoomer.stingraytv_alice.config.StingrayProperties;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Service for discovering StingrayTV devices on the local network using mDNS
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StingrayDeviceDiscoveryService {

    private final StingrayProperties stingrayProperties;

    private static final String STINGRAY_SERVICE_TYPE = "_stingray-remote._tcp.local.";
    private final Map<String, Device> discoveredDevices = new ConcurrentHashMap<>();
    private final RestClient restClient = RestClient.create();

    /**
     * Discover StingrayTV devices on the local network using mDNS
     */
    public Device discoverStingrayDevice() {
        String receiverIp = stingrayProperties.getReceiverIp();
        int receiverPort = stingrayProperties.getReceiverPort();

        if (StringUtils.hasText(receiverIp) && receiverPort >= 0) {
            Optional<Device> device = getDevice(receiverIp, receiverPort);

            if (device.isPresent()) {
                return device.get();
            }
        }

        try {
            JmDNS jmdns = JmDNS.create(InetAddress.getLocalHost());
            CountDownLatch latch = new CountDownLatch(1);

            jmdns.addServiceListener(STINGRAY_SERVICE_TYPE, new ServiceListener() {
                @Override
                public void serviceAdded(ServiceEvent event) {
                    log.info("Service added: {}", event.getName());
                }

                @Override
                public void serviceRemoved(ServiceEvent event) {
                    log.info("Service removed: {}", event.getName());
                    discoveredDevices.remove(event.getName());
                }

                @Override
                public void serviceResolved(ServiceEvent event) {
                    log.info("Service resolved: {} at {}:{}",
                            event.getName(),
                            event.getInfo().getHostAddresses()[0],
                            event.getInfo().getPort());

                    Optional<Device> device = getDevice(
                            event.getInfo().getHostAddresses()[0],
                            event.getInfo().getPort()
                    );

                    device.ifPresent(value -> discoveredDevices.put(event.getName(), value));

                    latch.countDown();
                }
            });

            // Wait for discovery with timeout
            boolean discovered = latch.await(5, TimeUnit.SECONDS);
            jmdns.close();

            if (discovered && !discoveredDevices.isEmpty()) {
                Device firstDevice = discoveredDevices.values().iterator().next();
                log.info("Discovered StingrayTV device: {}", firstDevice);
                return firstDevice;
            }

        } catch (IOException e) {
            log.error("Error during mDNS discovery", e);
        } catch (InterruptedException e) {
            log.error("Discovery interrupted", e);
            Thread.currentThread().interrupt();
        }

        log.warn("No StingrayTV devices discovered via mDNS");
        return null;
    }

    public record Device(String baseUrl, String model) {
    }

    /**
     * Check if device is reachable at the given URL using Spring WebClient
     */
    private Optional<Device> getDevice(String receiverIp, int receiverPort) {
        try {
            String baseUrl = String.format("http://%s:%d/v1.6",
                    receiverIp, receiverPort);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.get()
                    .uri(baseUrl + "/receiver-info")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("userFriendlyModelName")) {
                return Optional.of(
                        new Device(baseUrl, response.get("userFriendlyModelName").toString())
                );
            } else {
                return Optional.empty();
            }
        } catch (Exception e) {
            log.error("Error getting device info from StingrayTV", e);
            return Optional.empty();
        }
    }
}