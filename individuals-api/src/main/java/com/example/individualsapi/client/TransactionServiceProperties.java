package com.example.individualsapi.client;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "transaction-service")
public class TransactionServiceProperties {
    private String baseUrl = "http://localhost:8083";
}