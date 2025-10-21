package ru.oldzoomer.stingraytv_alice.dto.yandex;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class YandexSmartHomeRequest {
    @JsonProperty("request_id")
    private String requestId;

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
