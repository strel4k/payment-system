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
public class WalletResponse {
    private UUID uid;
    private UUID userUid;
    private UUID walletTypeUid;
    private String walletTypeName;
    private String currencyCode;
    private String name;
    private String status;
    private BigDecimal balance;
    private LocalDateTime createdAt;
}