package com.example.currencyrate.controller.dto;

public record CurrencyResponse(
        String code,
        String isoCode,
        String description,
        String symbol,
        boolean active
) {}