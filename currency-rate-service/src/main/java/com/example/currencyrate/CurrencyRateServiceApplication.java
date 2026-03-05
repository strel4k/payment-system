package com.example.currencyrate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class CurrencyRateServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CurrencyRateServiceApplication.class, args);
    }
}
