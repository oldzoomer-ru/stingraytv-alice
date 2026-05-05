package ru.oldzoomer.stingraytv_alice.dto.yandex;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

public record YandexSmartHomeRequest(
    @Valid
    Payload payload
) {
    public record Payload(
        @JsonProperty("user_id")
        String userId,

        @NotNull
        @Valid
        List<@Valid Device> devices
    ) {
        public record Device(
            @NotNull
            String id,

            List<Map<String, Object>> capabilities
        ) {
        }
    }
}
