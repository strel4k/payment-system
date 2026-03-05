package com.example.individualsapi.client.dto.currencyrate;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RateResponse {
    private String fromCurrency;
    private String toCurrency;
    private BigDecimal rate;
    private LocalDateTime rateBeginTime;
    private LocalDateTime rateEndTime;
    private String providerCode;
}