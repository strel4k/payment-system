package com.example.transaction.service;

import com.example.transaction.entity.enums.PaymentType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class InitRequest {

    private UUID requestUid;
    private UUID userUid;
    private UUID walletUid;
    private UUID targetWalletUid;
    private PaymentType type;
    private BigDecimal amount;
    private BigDecimal fee;
    private BigDecimal totalAmount;
    private Long paymentMethodId;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isTransfer() {
        return type == PaymentType.TRANSFER;
    }
}