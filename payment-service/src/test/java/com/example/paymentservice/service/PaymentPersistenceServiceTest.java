package com.example.paymentservice.service;

import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.entity.PaymentMethod;
import com.example.paymentservice.entity.PaymentStatus;
import com.example.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentPersistenceServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentPersistenceService paymentPersistenceService;

    private PaymentMethod paymentMethod;

    @BeforeEach
    void setUp() {
        paymentMethod = new PaymentMethod();
        paymentMethod.setId(1);
        paymentMethod.setName("Bank Card");
        paymentMethod.setProviderMethodType("CARD");
    }

    @Test
    @DisplayName("createPending — сохраняет Payment со статусом PENDING")
    void createPending_savesPaymentWithPendingStatus() {
        Payment saved = new Payment();
        saved.setId(1);
        saved.setStatus(PaymentStatus.PENDING);
        saved.setInternalTransactionUid("test-uid");
        saved.setAmount(BigDecimal.valueOf(100.0));
        saved.setCurrency("USD");

        when(paymentRepository.save(any())).thenReturn(saved);

        Payment result = paymentPersistenceService.createPending(
                paymentMethod, "test-uid", BigDecimal.valueOf(100.0), "USD");

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());

        Payment captured = captor.getValue();
        assertThat(captured.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(captured.getInternalTransactionUid()).isEqualTo("test-uid");
        assertThat(captured.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(100.0));
        assertThat(captured.getCurrency()).isEqualTo("USD");
        assertThat(captured.getPaymentMethod()).isEqualTo(paymentMethod);

        assertThat(result.getId()).isEqualTo(1);
    }

    @Test
    @DisplayName("markCompleted — обновляет статус COMPLETED и внешний ID")
    void markCompleted_updatesStatusAndExternalId() {
        Payment payment = new Payment();
        payment.setId(1);
        payment.setStatus(PaymentStatus.PENDING);

        when(paymentRepository.save(any())).thenReturn(payment);

        paymentPersistenceService.markCompleted(payment, "ext-99");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(payment.getExternalTransactionId()).isEqualTo("ext-99");
        verify(paymentRepository).save(payment);
    }

    @Test
    @DisplayName("markFailed — обновляет статус FAILED, externalTransactionId не трогает")
    void markFailed_updatesStatusToFailed() {
        Payment payment = new Payment();
        payment.setId(1);
        payment.setStatus(PaymentStatus.PENDING);

        when(paymentRepository.save(any())).thenReturn(payment);

        paymentPersistenceService.markFailed(payment);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getExternalTransactionId()).isNull();
        verify(paymentRepository).save(payment);
    }
}