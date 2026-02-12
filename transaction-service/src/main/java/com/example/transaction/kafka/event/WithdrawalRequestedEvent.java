package com.example.transaction.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class WithdrawalRequestedEvent extends BaseEvent {

    private UUID walletUid;

    private BigDecimal amount;

    private BigDecimal fee;

    private BigDecimal totalAmount;

    private String currencyCode;

    private Long paymentMethodId;
}