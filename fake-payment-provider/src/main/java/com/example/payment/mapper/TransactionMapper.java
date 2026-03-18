package com.example.payment.mapper;

import com.example.payment.dto.Transaction;
import com.example.payment.entity.OperationStatus;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;

@Component
public class TransactionMapper {

    public Transaction toDto(com.example.payment.entity.Transaction entity) {
        Transaction dto = new Transaction();
        dto.setId(entity.getId());
        dto.setMerchantId(entity.getMerchant().getMerchantId());
        dto.setAmount(entity.getAmount().doubleValue());
        dto.setCurrency(entity.getCurrency());
        dto.setMethod(entity.getMethod());
        dto.setStatus(Transaction.StatusEnum.valueOf(entity.getStatus().name()));
        dto.setDescription(entity.getDescription());
        dto.setExternalId(entity.getExternalId());
        dto.setNotificationUrl(entity.getNotificationUrl());

        if (entity.getCreatedAt() != null) {
            dto.setCreatedAt(entity.getCreatedAt().atOffset(ZoneOffset.UTC));
        }
        if (entity.getUpdatedAt() != null) {
            dto.setUpdatedAt(entity.getUpdatedAt().atOffset(ZoneOffset.UTC));
        }
        return dto;
    }
}