package com.example.transaction.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.transaction")
public class AppProperties {


    // Fee percentage for deposit operations (0.01 = 1%)

    private BigDecimal depositFeePercent = BigDecimal.ZERO;

     // Fee percentage for withdrawal operations

    private BigDecimal withdrawalFeePercent = new BigDecimal("0.01");

    // Fee percentage for transfer operations

    private BigDecimal transferFeePercent = new BigDecimal("0.005");

     // Init request TTL in minutes

    private int initRequestTtlMinutes = 15;
}