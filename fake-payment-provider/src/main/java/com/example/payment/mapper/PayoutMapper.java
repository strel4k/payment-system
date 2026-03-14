package com.example.payment.mapper;

import com.example.payment.dto.Payout;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;

@Component
public class PayoutMapper {

    public Payout toDto(com.example.payment.entity.Payout entity) {
        Payout dto = new Payout();
        dto.setId(entity.getId());
        dto.setMerchantId(entity.getMerchant().getMerchantId());
        dto.setAmount(entity.getAmount().doubleValue());
        dto.setCurrency(entity.getCurrency());
        dto.setStatus(Payout.StatusEnum.valueOf(entity.getStatus().name()));
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