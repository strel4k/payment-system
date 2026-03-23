package com.example.paymentservice.client.dto;

public record FppTransactionRequest(
        double amount,
        String currency,
        String method,
        String description,
        String notificationUrl
) {}