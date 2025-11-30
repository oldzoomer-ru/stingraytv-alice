package ru.oldzoomer.stingraytv_alice.dto.yandex;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class YandexSmartHomeResponse {
    @JsonProperty("request_id")
    private String requestId;

    private String status;

    @JsonProperty("error_code")
    private String errorCode;

    @JsonProperty("error_message")
    private String errorMessage;

    private Payload payload;

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Payload {
        @JsonProperty("user_id")
        private String userId;

        private List<Device> devices;

        @Data
        @Builder
        public static class Device {
            private String id;
            private String name;
            private String description;
            private String room;
            private String type;
            private List<Capability> capabilities;
            private List<Property> properties;

            @Data
            @Builder
            public static class Capability {
                private String type;
                private boolean retrievable;
                private Map<String, Object> parameters;
                private Map<String, Object> state;
            }

            @Data
            @Builder
            public static class Property {
                private String type;
                private boolean retrievable;
                private Map<String, Object> parameters;
                private Map<String, Object> state;
            }
        }
    }
}