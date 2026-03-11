package com.example.currencyrate.controller.dto;

public record RateProviderResponse(
        String providerCode,
        String providerName,
        int priority,
        boolean active
) {}