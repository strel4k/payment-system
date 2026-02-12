package com.example.individualsapi.client.dto.transaction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionConfirmResponse {
    private UUID transactionUid;
    private String type;
    private String status;
    private BigDecimal amount;
    private BigDecimal fee;
    private LocalDateTime createdAt;
}