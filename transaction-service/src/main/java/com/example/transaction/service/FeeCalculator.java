package com.example.transaction.service;

import com.example.transaction.config.AppProperties;
import com.example.transaction.entity.enums.PaymentType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
@RequiredArgsConstructor
public class FeeCalculator {

    private final AppProperties appProperties;

    public BigDecimal calculateFee(BigDecimal amount, PaymentType type) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal feePercent = getFeePercent(type);
        return amount.multiply(feePercent).setScale(4, RoundingMode.HALF_UP);
    }

    public BigDecimal getFeePercent(PaymentType type) {
        return switch (type) {
            case DEPOSIT -> appProperties.getDepositFeePercent();
            case WITHDRAWAL -> appProperties.getWithdrawalFeePercent();
            case TRANSFER -> appProperties.getTransferFeePercent();
        };
    }

    public BigDecimal calculateTotalAmount(BigDecimal amount, PaymentType type) {
        BigDecimal fee = calculateFee(amount, type);
        return amount.add(fee);
    }
}