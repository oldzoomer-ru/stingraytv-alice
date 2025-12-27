package ru.oldzoomer.stingraytv_alice.service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for making REST calls with retry mechanism and exponential backoff
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RetryableRestClient {
    
    private final RestClient restClient;
    
    @Value("${app.rest-client.retry-max-attempts:3}")
    private int retryMaxAttempts;
    
    @Value("${app.rest-client.retry-initial-delay-ms:100}")
    private long retryInitialDelayMs;
    
    /**
     * Execute GET request with retry mechanism
     */
    public Map<String, Object> getWithRetry(String url) {
        return getWithRetry(url, retryMaxAttempts, retryInitialDelayMs);
    }
    
    /**
     * Execute GET request with retry mechanism
     */
    public Map<String, Object> getWithRetry(String url, int maxRetries, long initialDelayMs) {
        Exception lastException = null;
        
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> response = restClient.get()
                        .uri(url)
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .body(Map.class);
                
                if (response != null) {
                    return response;
                }
            } catch (Exception e) {
                lastException = e;
                log.warn("REST GET request failed on attempt {}: {}", attempt + 1, e.getMessage());
                
                // Don't sleep on the last attempt
                if (attempt < maxRetries) {
                    long delay = initialDelayMs * (1L << attempt); // Exponential backoff
                    try {
                        TimeUnit.MILLISECONDS.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Request interrupted", ie);
                    }
                }
            }
        }
        
        log.error("All {} attempts failed for GET request to {}", maxRetries + 1, url);
        throw new RuntimeException("Failed to get data after " + (maxRetries + 1) + " attempts", lastException);
    }
    
    /**
     * Execute PUT request with retry mechanism
     */
    public void putWithRetry(String url, Object body) {
        putWithRetry(url, body, retryMaxAttempts, retryInitialDelayMs);
    }
    
    /**
     * Execute PUT request with retry mechanism
     */
    public void putWithRetry(String url, Object body, int maxRetries, long initialDelayMs) {
        Exception lastException = null;
        
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                restClient.put()
                        .uri(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .toBodilessEntity();
                
                return;
            } catch (Exception e) {
                lastException = e;
                log.warn("REST PUT request failed on attempt {}: {}", attempt + 1, e.getMessage());
                
                // Don't sleep on the last attempt
                if (attempt < maxRetries) {
                    long delay = initialDelayMs * (1L << attempt); // Exponential backoff
                    try {
                        TimeUnit.MILLISECONDS.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Request interrupted", ie);
                    }
                }
            }
        }
        
        log.error("All {} attempts failed for PUT request to {}", maxRetries + 1, url);
        throw new RuntimeException("Failed to put data after " + (maxRetries + 1) + " attempts", lastException);
    }
}