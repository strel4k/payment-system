package com.example.paymentservice.service;

import com.example.paymentservice.dto.PaymentRequest;
import com.example.paymentservice.dto.PaymentResponse;
import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.entity.PaymentMethod;
import com.example.paymentservice.entity.PaymentOutbox;
import com.example.paymentservice.entity.PaymentStatus;
import com.example.paymentservice.exception.PaymentMethodNotFoundException;
import com.example.paymentservice.repository.PaymentMethodRepository;
import com.example.paymentservice.repository.PaymentOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    private PaymentOutboxRepository paymentOutboxRepository;

    @Mock
    private PaymentPersistenceService paymentPersistenceService;

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
    @DisplayName("processPayment — создаёт Payment(PENDING) и запись outbox, возвращает PENDING")
    void processPayment_createsPaymentAndOutboxEntry_returnsPending() {
        when(paymentMethodRepository.findById(1)).thenReturn(Optional.of(paymentMethod));
        when(paymentPersistenceService.createPending(any(), any(), any(), any()))
                .thenReturn(pendingPayment);
        when(paymentOutboxRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PaymentResponse result = paymentOrchestrationService.processPayment(request);

        assertThat(result.getStatus()).isEqualTo(PaymentResponse.StatusEnum.PENDING);
        assertThat(result.getProviderTransactionId()).isNull();

        verify(paymentPersistenceService).createPending(
                eq(paymentMethod),
                eq("550e8400-e29b-41d4-a716-446655440000"),
                eq(BigDecimal.valueOf(100.0)),
                eq("USD")
        );

        ArgumentCaptor<PaymentOutbox> outboxCaptor = ArgumentCaptor.forClass(PaymentOutbox.class);
        verify(paymentOutboxRepository).save(outboxCaptor.capture());
        PaymentOutbox savedOutbox = outboxCaptor.getValue();
        assertThat(savedOutbox.getPayment()).isEqualTo(pendingPayment);
        assertThat(savedOutbox.getMethodType()).isEqualTo("CARD");
        assertThat(savedOutbox.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(100.0));
        assertThat(savedOutbox.getCurrency()).isEqualTo("USD");
    }

    @Test
    @DisplayName("processPayment — FPP НЕ вызывается напрямую (outbox pattern)")
    void processPayment_doesNotCallFppDirectly() {
        when(paymentMethodRepository.findById(1)).thenReturn(Optional.of(paymentMethod));
        when(paymentPersistenceService.createPending(any(), any(), any(), any()))
                .thenReturn(pendingPayment);
        when(paymentOutboxRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        paymentOrchestrationService.processPayment(request);

        verify(paymentPersistenceService, never()).markCompleted(any(), any());
        verify(paymentPersistenceService, never()).markFailed(any());
    }

    @Test
    @DisplayName("processPayment — methodId не найден → PaymentMethodNotFoundException, outbox не создаётся")
    void processPayment_methodNotFound_throws_noOutboxCreated() {
        when(paymentMethodRepository.findById(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentOrchestrationService.processPayment(request))
                .isInstanceOf(PaymentMethodNotFoundException.class);

        verify(paymentPersistenceService, never()).createPending(any(), any(), any(), any());
        verify(paymentOutboxRepository, never()).save(any());
    }

    @Test
    @DisplayName("processPayment — метод неактивен → PaymentMethodNotFoundException, outbox не создаётся")
    void processPayment_inactiveMethod_throws_noOutboxCreated() {
        paymentMethod.setIsActive(false);
        when(paymentMethodRepository.findById(1)).thenReturn(Optional.of(paymentMethod));

        assertThatThrownBy(() -> paymentOrchestrationService.processPayment(request))
                .isInstanceOf(PaymentMethodNotFoundException.class);

        verify(paymentOutboxRepository, never()).save(any());
    }
}