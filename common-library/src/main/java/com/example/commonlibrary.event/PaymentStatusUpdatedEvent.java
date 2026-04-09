package com.example.commonlibrary.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatusUpdatedEvent {

    private UUID eventId;
    private LocalDateTime timestamp;
    private UUID providerTransactionUid;
    private String type;
    private String provider;
    private String status;
}