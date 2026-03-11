package com.example.currencyrate.controller.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CurrencyRateResponse(
        String fromCurrency,
        String toCurrency,
        BigDecimal rate,
        LocalDateTime rateBeginTime,
        LocalDateTime rateEndTime,
        String providerCode
) {}