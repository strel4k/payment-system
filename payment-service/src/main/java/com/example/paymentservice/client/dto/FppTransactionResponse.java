package com.example.paymentservice.client.dto;

import java.time.OffsetDateTime;

public record FppTransactionResponse(
        Long id,
        String merchantId,
        Double amount,
        String currency,
        String method,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String description,
        String externalId,
        String notificationUrl
) {}