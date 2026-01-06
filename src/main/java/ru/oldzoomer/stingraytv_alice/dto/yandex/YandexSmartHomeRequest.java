package ru.oldzoomer.stingraytv_alice.dto.yandex;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class YandexSmartHomeRequest {
    private Payload payload;

    @Data
    public static class Payload {
        @JsonProperty("user_id")
        private String userId;

        private List<Device> devices;

        @Data
        public static class Device {
            private String id;

            private List<Map<String, Object>> capabilities;
        }
    }
}
