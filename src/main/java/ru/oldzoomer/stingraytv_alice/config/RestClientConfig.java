package ru.oldzoomer.stingraytv_alice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Slf4j
@Configuration
public class RestClientConfig {

    @Value("${app.rest-client.connect-timeout:5000}")
    private int connectTimeout;

    @Value("${app.rest-client.read-timeout:10000}")
    private int readTimeout;

    @Bean
    public RestClient restClient() {
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
