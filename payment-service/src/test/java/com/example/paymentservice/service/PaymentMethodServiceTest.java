package com.example.paymentservice.service;

import com.example.paymentservice.dto.PaymentMethodResponse;
import com.example.paymentservice.entity.PaymentMethod;
import com.example.paymentservice.mapper.PaymentMethodMapper;
import com.example.paymentservice.repository.PaymentMethodRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentMethodServiceTest {

    @Mock
    private PaymentMethodRepository paymentMethodRepository;

    @Mock
    private PaymentMethodMapper paymentMethodMapper;

    @InjectMocks
    private PaymentMethodService paymentMethodService;

    @Test
    @DisplayName("getPaymentMethods — возвращает методы оплаты для USD/USA")
    void getPaymentMethods_returnsFilteredMethods() {
        PaymentMethod method = new PaymentMethod();
        method.setId(1);
        method.setName("Bank Card");
        method.setProviderMethodType("CARD");
        method.setIsActive(true);

        PaymentMethodResponse dto = new PaymentMethodResponse();
        dto.setId(1);
        dto.setName("Bank Card");

        when(paymentMethodRepository.findActiveByCurrencyAndCountry("USD", "USA"))
                .thenReturn(List.of(method));
        when(paymentMethodMapper.toDto(method)).thenReturn(dto);

        List<PaymentMethodResponse> result = paymentMethodService.getPaymentMethods("USD", "USA");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Bank Card");
        verify(paymentMethodRepository, times(1)).findActiveByCurrencyAndCountry("USD", "USA");
    }

    @Test
    @DisplayName("getPaymentMethods — возвращает пустой список если методов нет")
    void getPaymentMethods_noMethods_returnsEmptyList() {
        when(paymentMethodRepository.findActiveByCurrencyAndCountry("EUR", "DEU"))
                .thenReturn(Collections.emptyList());

        List<PaymentMethodResponse> result = paymentMethodService.getPaymentMethods("EUR", "DEU");

        assertThat(result).isEmpty();
        verify(paymentMethodMapper, never()).toDto(any());
    }
}