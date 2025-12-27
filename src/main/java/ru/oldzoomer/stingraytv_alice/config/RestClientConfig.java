package ru.oldzoomer.stingraytv_alice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class RestClientConfig {

    @Value("${app.rest-client.connect-timeout:3000}")
    private int connectTimeout;

    @Value("${app.rest-client.read-timeout:3000}")
    private int readTimeout;

    @Bean
    RestClient restClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);
        return RestClient.builder()
                .requestFactory(requestFactory)
                .defaultHeader("User-Agent", "StingrayTV-Alice/1.0")
                .defaultStatusHandler(response -> response.getStatusCode().isError())
                .build();
    }
}
