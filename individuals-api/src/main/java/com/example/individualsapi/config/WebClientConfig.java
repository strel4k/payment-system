package com.example.individualsapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    private ExchangeStrategies defaultStrategies() {
        return ExchangeStrategies.builder()
                .codecs(cfg -> cfg.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
    }

    @Bean
    public WebClient keycloakWebClient() {
        return WebClient.builder()
                .exchangeStrategies(defaultStrategies())
                .build();
    }

    @Bean
    public WebClient personServiceWebClient() {
        return WebClient.builder()
                .exchangeStrategies(defaultStrategies())
                .build();
    }

    @Bean
    public WebClient transactionServiceWebClient() {
        return WebClient.builder()
                .exchangeStrategies(defaultStrategies())
                .build();
    }
}