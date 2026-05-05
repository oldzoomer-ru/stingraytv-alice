package ru.oldzoomer.stingraytv_alice.dto.yandex;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record YandexSmartHomeResponse(
    @JsonProperty("request_id")
    String requestId,

    @NotNull
    String status,

    @JsonProperty("error_code")
    String errorCode,

    @JsonProperty("error_message")
    String errorMessage,

    @Valid
    Payload payload
) {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Payload(
        @JsonProperty("user_id")
        String userId,

        @Valid
        List<@Valid Device> devices
    ) {
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record Device(
            @NotNull
            String id,
            String name,
            String description,
            String room,
            String type,
            @Valid
            List<@Valid Capability> capabilities,
            @Valid
            List<@Valid Property> properties,
            @JsonProperty("status_info")
            @Valid
            StatusInfo statusInfo,
            @JsonProperty("device_info")
            @Valid
            DeviceInfo deviceInfo
        ) {
            @JsonInclude(JsonInclude.Include.NON_NULL)
            public record Capability(
                @NotNull
                String type,
                boolean retrievable,
                Map<String, Object> parameters,
                Map<String, Object> state
            ) {
            }

            @JsonInclude(JsonInclude.Include.NON_NULL)
            public record Property(
                @NotNull
                String type,
                boolean retrievable,
                Map<String, Object> parameters,
                Map<String, Object> state
            ) {
            }

            @JsonInclude(JsonInclude.Include.NON_NULL)
            public record StatusInfo(
                    boolean reportable
            ) {
            }

            @JsonInclude(JsonInclude.Include.NON_NULL)
            public record DeviceInfo(
                    String manufacturer,
                    String model,
                @JsonProperty("hw_version")
                    String hwVersion,
                @JsonProperty("sw_version")
                    String swVersion
            ) {
            }
        }
    }
}
