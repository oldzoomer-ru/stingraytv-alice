package ru.oldzoomer.stingraytv_alice.dto.yandex;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * DTO for Yandex Smart Home user unlink response
 * Response format required by Yandex Smart Home API
 */
@Data
@Builder
public class UserUnlinkResponse {

    @JsonProperty("request_id")
    private String requestId;
}