package ru.oldzoomer.stingraytv_alice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing temporary authorization codes for physical presence authentication
 * This service generates and stores temporary codes in memory for 5 minutes
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TemporaryCodeService {

    /**
     * In-memory storage for temporary codes with expiration timestamps
     */
    private final Map<String, CodeEntry> temporaryCodes = new ConcurrentHashMap<>();

    /**
     * Secure random generator for code generation
     */
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generate a temporary authorization code for physical presence authentication
     *
     * @param clientId the client identifier
     */
    public void generateTemporaryCode(String clientId) {
        // Generate a secure random 16-byte code (24 characters when base64 encoded)
        byte[] bytes = new byte[16];
        secureRandom.nextBytes(bytes);
        String code = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        // Store the code with expiration timestamp (5 minutes from now)
        long expiryTime = System.currentTimeMillis() + 5 * 60 * 1000; // 5 minutes
        temporaryCodes.put(code, new CodeEntry(code, clientId, expiryTime));

        log.info("Generated temporary authorization code for client {}: {}", clientId, code);
    }

    /**
     * Verify and consume a temporary authorization code
     *
     * @param code     the code to verify
     * @param clientId the client identifier to match against
     * @return true if valid and not expired, false otherwise
     */
    public boolean validateAndConsumeCode(String code, String clientId) {
        CodeEntry entry = temporaryCodes.get(code);

        // Check if code exists and is not expired
        if (entry == null || System.currentTimeMillis() > entry.expiryTime ||
                getClientIdForCode(code).equals(clientId)) {
            log.warn("Invalid or expired temporary authorization code: {}", code);
            return false;
        }

        // Remove the code after successful validation
        temporaryCodes.remove(code);
        log.info("Consumed temporary authorization code for client: {}", entry.clientId);
        return true;
    }

    /**
     * Get the client ID associated with a temporary code
     *
     * @param code the code to lookup
     * @return client ID if found, null otherwise
     */
    public String getClientIdForCode(String code) {
        CodeEntry entry = temporaryCodes.get(code);
        return entry != null ? entry.clientId : null;
    }

    /**
     * Clean up expired codes (called periodically)
     */
    public void cleanupExpiredCodes() {
        long currentTime = System.currentTimeMillis();
        int initialSize = temporaryCodes.size();

        temporaryCodes.entrySet().removeIf(entry -> currentTime > entry.getValue().expiryTime);

        int removedCount = initialSize - temporaryCodes.size();
        if (removedCount > 0) {
            log.debug("Removed {} expired temporary codes", removedCount);
        }
    }

    /**
     * Internal class to hold code information
     */
    private record CodeEntry(String code, String clientId, long expiryTime) {
    }
}
