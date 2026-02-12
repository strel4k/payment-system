package com.example.transaction.mapper;

import com.example.transaction.dto.TransactionConfirmResponse;
import com.example.transaction.dto.TransactionInitResponse;
import com.example.transaction.dto.TransactionStatusResponse;
import com.example.transaction.entity.Transaction;
import com.example.transaction.service.InitRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.ZoneOffset;

@Component
public class TransactionMapper {

    public TransactionInitResponse toInitResponse(InitRequest initRequest, String currencyCode) {
        return TransactionInitResponse.builder()
                .requestUid(initRequest.getRequestUid())
                .walletUid(initRequest.getWalletUid())
                .amount(initRequest.getAmount())
                .fee(initRequest.getFee())
                .totalAmount(initRequest.getTotalAmount())
                .currencyCode(currencyCode)
                .available(true)
                .expiresAt(initRequest.getExpiresAt().atOffset(ZoneOffset.UTC))
                .build();
    }

    public TransactionConfirmResponse toConfirmResponse(Transaction transaction) {
        return TransactionConfirmResponse.builder()
                .transactionUid(transaction.getUid())
                .status(TransactionConfirmResponse.StatusEnum.fromValue(transaction.getStatus().name()))
                .type(TransactionConfirmResponse.TypeEnum.fromValue(transaction.getType().name()))
                .amount(transaction.getAmount())
                .fee(transaction.getFee())
                .createdAt(transaction.getCreatedAt() != null
                        ? transaction.getCreatedAt().atOffset(ZoneOffset.UTC)
                        : null)
                .build();
    }

    public TransactionStatusResponse toStatusResponse(Transaction transaction, String currencyCode) {
        return TransactionStatusResponse.builder()
                .uid(transaction.getUid())
                .userUid(transaction.getUserUid())
                .walletUid(transaction.getWallet().getUid())
                .targetWalletUid(transaction.getTargetWallet() != null
                        ? transaction.getTargetWallet().getUid()
                        : null)
                .type(TransactionStatusResponse.TypeEnum.fromValue(transaction.getType().name()))
                .status(TransactionStatusResponse.StatusEnum.fromValue(transaction.getStatus().name()))
                .amount(transaction.getAmount())
                .fee(transaction.getFee())
                .currencyCode(currencyCode)
                .failureReason(transaction.getFailureReason())
                .createdAt(transaction.getCreatedAt() != null
                        ? transaction.getCreatedAt().atOffset(ZoneOffset.UTC)
                        : null)
                .modifiedAt(transaction.getModifiedAt() != null
                        ? transaction.getModifiedAt().atOffset(ZoneOffset.UTC)
                        : null)
                .build();
    }
}