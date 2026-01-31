package ru.oldzoomer.stingraytv_alice.dto.yandex;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class YandexSmartHomeResponse {
    @JsonProperty("request_id")
    private String requestId;

    @NotNull
    private String status;

    @JsonProperty("error_code")
    private String errorCode;

    @JsonProperty("error_message")
    private String errorMessage;

    @Valid
    private Payload payload;

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Payload {
        @JsonProperty("user_id")
        private String userId;

        private List<@Valid Device> devices;

        @Data
        @Builder
        public static class Device {
            @NotNull
            private String id;
            private String name;
            private String description;
            private String room;
            private String type;
            private List<@Valid Capability> capabilities;
            private List<@Valid Property> properties;
            @JsonProperty("status_info")
            private @Valid StatusInfo statusInfo;
            @JsonProperty("device_info")
            private @Valid DeviceInfo deviceInfo;

            @Data
            @Builder
            public static class Capability {
                @NotNull
                private String type;
                private boolean retrievable;
                private Map<String, Object> parameters;
                private Map<String, Object> state;
            }

            @Data
            @Builder
            public static class Property {
                @NotNull
                private String type;
                private boolean retrievable;
                private Map<String, Object> parameters;
                private Map<String, Object> state;
            }

            @Data
            @Builder
            public static class StatusInfo {
                private boolean reportable;
            }

            @Data
            @Builder
            public static class DeviceInfo {
                private String manufacturer;
                private String model;
                @JsonProperty("hw_version")
                private String hwVersion;
                @JsonProperty("sw_version")
                private String swVersion;
            }
        }
    }
}