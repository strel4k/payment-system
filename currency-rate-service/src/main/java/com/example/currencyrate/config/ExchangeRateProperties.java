package com.example.currencyrate.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "exchange-rate")
@Getter
@Setter
public class ExchangeRateProperties {

    private String apiKey = "demo";
    private String baseUrl = "https://v6.exchangerate-api.com";
    private String baseCurrency = "USD";
    private long updateIntervalMs = 3_600_000L;
    private int rateTtlHours = 2;
}