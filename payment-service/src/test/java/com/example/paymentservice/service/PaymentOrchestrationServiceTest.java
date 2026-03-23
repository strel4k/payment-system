package com.example.paymentservice.service;

import com.example.paymentservice.client.FakeProviderClient;
import com.example.paymentservice.client.dto.FppTransactionResponse;
import com.example.paymentservice.dto.PaymentRequest;
import com.example.paymentservice.dto.PaymentResponse;
import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.entity.PaymentMethod;
import com.example.paymentservice.entity.PaymentStatus;
import com.example.paymentservice.exception.PaymentMethodNotFoundException;
import com.example.paymentservice.exception.PaymentProviderException;
import com.example.paymentservice.repository.PaymentMethodRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentOrchestrationServiceTest {

    @Mock
    private PaymentMethodRepository paymentMethodRepository;

    @Mock
    private PaymentPersistenceService paymentPersistenceService;

    @Mock
    private FakeProviderClient fakeProviderClient;

    @InjectMocks
    private PaymentOrchestrationService paymentOrchestrationService;

    private PaymentMethod paymentMethod;
    private PaymentRequest request;
    private Payment pendingPayment;

    @BeforeEach
    void setUp() {
        paymentMethod = new PaymentMethod();
        paymentMethod.setId(1);
        paymentMethod.setName("Bank Card");
        paymentMethod.setProviderMethodType("CARD");
        paymentMethod.setIsActive(true);

        request = new PaymentRequest();
        request.setMethodId(1);
        request.setInternalTransactionUid(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
        request.setAmount(100.0);
        request.setCurrency("USD");

        pendingPayment = new Payment();
        pendingPayment.setId(1);
        pendingPayment.setPaymentMethod(paymentMethod);
        pendingPayment.setAmount(BigDecimal.valueOf(100.0));
        pendingPayment.setCurrency("USD");
        pendingPayment.setStatus(PaymentStatus.PENDING);
        pendingPayment.setInternalTransactionUid("550e8400-e29b-41d4-a716-446655440000");
    }

    @Test
    @DisplayName("processPayment — happy path: FPP вернул успех → статус COMPLETED")
    void processPayment_success_returnsCompleted() {
        FppTransactionResponse fppResponse = new FppTransactionResponse(
                42L, "merchant-1", 100.0, "USD", "CARD",
                "PENDING", null, null, null, null, null
        );

        when(paymentMethodRepository.findById(1)).thenReturn(Optional.of(paymentMethod));
        when(paymentPersistenceService.createPending(any(), any(), any(), any()))
                .thenReturn(pendingPayment);
        when(fakeProviderClient.createTransaction(any(), any(), any())).thenReturn(fppResponse);

        PaymentResponse result = paymentOrchestrationService.processPayment(request);

        assertThat(result).isNotNull();
        assertThat(result.getProviderTransactionId()).isEqualTo("42");
        assertThat(result.getStatus()).isEqualTo(PaymentResponse.StatusEnum.COMPLETED);

        verify(paymentPersistenceService).createPending(eq(paymentMethod),
                eq("550e8400-e29b-41d4-a716-446655440000"),
                eq(BigDecimal.valueOf(100.0)), eq("USD"));
        verify(paymentPersistenceService).markCompleted(pendingPayment, "42");
        verify(paymentPersistenceService, never()).markFailed(any());
    }

    @Test
    @DisplayName("processPayment — FPP вернул ошибку → статус FAILED, бросает исключение")
    void processPayment_fppError_marksFailedAndThrows() {
        when(paymentMethodRepository.findById(1)).thenReturn(Optional.of(paymentMethod));
        when(paymentPersistenceService.createPending(any(), any(), any(), any()))
                .thenReturn(pendingPayment);
        when(fakeProviderClient.createTransaction(any(), any(), any()))
                .thenThrow(new PaymentProviderException("Provider error", HttpStatus.BAD_GATEWAY));

        assertThatThrownBy(() -> paymentOrchestrationService.processPayment(request))
                .isInstanceOf(PaymentProviderException.class)
                .hasMessageContaining("Provider error");

        verify(paymentPersistenceService).markFailed(pendingPayment);
        verify(paymentPersistenceService, never()).markCompleted(any(), any());
    }

    @Test
    @DisplayName("processPayment — methodId не найден → PaymentMethodNotFoundException")
    void processPayment_methodNotFound_throws() {
        when(paymentMethodRepository.findById(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentOrchestrationService.processPayment(request))
                .isInstanceOf(PaymentMethodNotFoundException.class);

        verify(paymentPersistenceService, never()).createPending(any(), any(), any(), any());
        verify(fakeProviderClient, never()).createTransaction(any(), any(), any());
    }

    @Test
    @DisplayName("processPayment — метод неактивен → PaymentMethodNotFoundException")
    void processPayment_inactiveMethod_throws() {
        paymentMethod.setIsActive(false);
        when(paymentMethodRepository.findById(1)).thenReturn(Optional.of(paymentMethod));

        assertThatThrownBy(() -> paymentOrchestrationService.processPayment(request))
                .isInstanceOf(PaymentMethodNotFoundException.class);

        verify(fakeProviderClient, never()).createTransaction(any(), any(), any());
    }
}