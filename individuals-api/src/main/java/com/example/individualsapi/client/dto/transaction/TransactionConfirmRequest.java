package com.example.individualsapi.client.dto.transaction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionConfirmRequest {
    private UUID requestUid;
    private UUID walletUid;
    private UUID targetWalletUid;
    private BigDecimal amount;
    private Long paymentMethodId;
}