package com.example.paymentservice.mapper;

import com.example.paymentservice.dto.PaymentMethodResponse;
import com.example.paymentservice.dto.RequiredField;
import com.example.paymentservice.entity.PaymentMethod;
import com.example.paymentservice.entity.PaymentMethodRequiredField;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
public class PaymentMethodMapper {

    public PaymentMethodResponse toDto(PaymentMethod entity) {
        PaymentMethodResponse dto = new PaymentMethodResponse();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setProviderMethodType(entity.getProviderMethodType());
        dto.setImageUrl(entity.getLogo());

        List<RequiredField> fields = entity.getRequiredFields().stream()
                .filter(f -> Boolean.TRUE.equals(f.getIsActive()))
                .map(this::toRequiredFieldDto)
                .toList();
        dto.setRequiredFields(fields);

        return dto;
    }

    private RequiredField toRequiredFieldDto(PaymentMethodRequiredField entity) {
        RequiredField dto = new RequiredField();

        if (entity.getUid() != null) {
            dto.setUid(entity.getUid());
        }
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setPlaceholder(entity.getPlaceholder());
        dto.setRepresentationName(entity.getRepresentationName());
        dto.setDataType(entity.getDataType());
        dto.setValidationType(entity.getValidationType());
        dto.setValidationRule(entity.getValidationRule());
        dto.setDefaultValue(entity.getDefaultValue());
        dto.setLanguage(entity.getLanguage());
        dto.setIsActive(entity.getIsActive());

        if (entity.getValuesOptions() != null && !entity.getValuesOptions().isBlank()) {
            dto.setValuesOptions(Arrays.asList(entity.getValuesOptions().split(",")));
        } else {
            dto.setValuesOptions(Collections.emptyList());
        }

        return dto;
    }
}