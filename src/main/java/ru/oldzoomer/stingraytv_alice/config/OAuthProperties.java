package ru.oldzoomer.stingraytv_alice.config;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "oauth")
public class OAuthProperties {
    @NotEmpty
    private List<Client> clients;

    @Setter
    @Getter
    public static class Client {
        private String clientId;
        private List<String> redirectUris;
    }
}