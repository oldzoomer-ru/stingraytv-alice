package ru.oldzoomer.stingraytv_alice.dto.yandex;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class YandexSmartHomeRequest {
    @Valid
    private Payload payload;

    @Data
    public static class Payload {
        @JsonProperty("user_id")
        private String userId;

        @NotNull
        private List<@Valid Device> devices;

        @Data
        public static class Device {
            @NotNull
            private String id;

            private List<Map<String, Object>> capabilities;
        }
    }
}
