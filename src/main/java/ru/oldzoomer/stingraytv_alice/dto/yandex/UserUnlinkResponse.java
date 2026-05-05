package ru.oldzoomer.stingraytv_alice.dto.yandex;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for Yandex Smart Home user unlink response
 */
public record UserUnlinkResponse(
    @JsonProperty("request_id")
    @NotNull
    String requestId
) {
}
