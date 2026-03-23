package com.example.paymentservice.mapper;

import com.example.paymentservice.dto.PaymentMethodResponse;
import com.example.paymentservice.entity.PaymentMethod;
import com.example.paymentservice.entity.PaymentMethodRequiredField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentMethodMapperTest {

    private PaymentMethodMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new PaymentMethodMapper();
    }

    @Test
    @DisplayName("toDto — маппит все поля PaymentMethod корректно")
    void toDto_mapsAllFields() {
        PaymentMethod entity = new PaymentMethod();
        entity.setId(1);
        entity.setName("Bank Card");
        entity.setProviderMethodType("CARD");
        entity.setLogo("https://example.com/card.png");

        PaymentMethodResponse dto = mapper.toDto(entity);

        assertThat(dto.getId()).isEqualTo(1);
        assertThat(dto.getName()).isEqualTo("Bank Card");
        assertThat(dto.getProviderMethodType()).isEqualTo("CARD");
        assertThat(dto.getImageUrl()).isEqualTo("https://example.com/card.png");
        assertThat(dto.getRequiredFields()).isEmpty();
    }

    @Test
    @DisplayName("toDto — маппит requiredFields, фильтрует неактивные")
    void toDto_filtersInactiveRequiredFields() {
        PaymentMethodRequiredField activeField = buildField("card_number", true);
        PaymentMethodRequiredField inactiveField = buildField("old_field", false);

        PaymentMethod entity = new PaymentMethod();
        entity.setId(1);
        entity.setName("Bank Card");
        entity.setProviderMethodType("CARD");
        entity.setRequiredFields(List.of(activeField, inactiveField));

        PaymentMethodResponse dto = mapper.toDto(entity);

        assertThat(dto.getRequiredFields()).hasSize(1);
        assertThat(dto.getRequiredFields().get(0).getName()).isEqualTo("card_number");
    }

    @Test
    @DisplayName("toDto — valuesOptions парсится из CSV в список")
    void toDto_parsesValuesOptionsFromCsv() {
        PaymentMethodRequiredField field = buildField("payment_type", true);
        field.setValuesOptions("CARD,BANK_TRANSFER,CRYPTO");

        PaymentMethod entity = new PaymentMethod();
        entity.setId(1);
        entity.setRequiredFields(List.of(field));

        PaymentMethodResponse dto = mapper.toDto(entity);

        assertThat(dto.getRequiredFields().get(0).getValuesOptions())
                .containsExactly("CARD", "BANK_TRANSFER", "CRYPTO");
    }

    @Test
    @DisplayName("toDto — пустой valuesOptions возвращает пустой список")
    void toDto_nullValuesOptions_returnsEmptyList() {
        PaymentMethodRequiredField field = buildField("card_number", true);
        field.setValuesOptions(null);

        PaymentMethod entity = new PaymentMethod();
        entity.setId(1);
        entity.setRequiredFields(List.of(field));

        PaymentMethodResponse dto = mapper.toDto(entity);

        assertThat(dto.getRequiredFields().get(0).getValuesOptions()).isEmpty();
    }

    @Test
    @DisplayName("toDto — uid маппится как UUID объект")
    void toDto_mapsUidAsUuid() {
        UUID uid = UUID.randomUUID();
        PaymentMethodRequiredField field = buildField("card_number", true);
        field.setUid(uid);

        PaymentMethod entity = new PaymentMethod();
        entity.setId(1);
        entity.setRequiredFields(List.of(field));

        PaymentMethodResponse dto = mapper.toDto(entity);

        assertThat(dto.getRequiredFields().get(0).getUid()).isEqualTo(uid);
    }

    // ==================== helpers ====================

    private PaymentMethodRequiredField buildField(String name, boolean active) {
        PaymentMethodRequiredField field = new PaymentMethodRequiredField();
        field.setUid(UUID.randomUUID());
        field.setName(name);
        field.setDataType("STRING");
        field.setPaymentType("DEPOSIT");
        field.setIsActive(active);
        return field;
    }
}