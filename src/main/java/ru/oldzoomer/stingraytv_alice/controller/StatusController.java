package ru.oldzoomer.stingraytv_alice.controller;

import org.jspecify.annotations.NonNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1.0")
public class StatusController {
    /**
     * Health check endpoint
     */
    @RequestMapping(value = "", method = RequestMethod.HEAD)
    public ResponseEntity<@NonNull String> healthCheck() {
        return ResponseEntity.ok("OK");
    }
}
