package com.example.webhookcollector.controller.dto;

import java.util.UUID;

public record WebhookPayload(
        UUID providerTransactionUid,
        String type,
        String provider,
        String status
) {}