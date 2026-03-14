package com.example.payment.service;

import com.example.payment.dto.StatusUpdate;
import com.example.payment.entity.OperationStatus;
import com.example.payment.entity.Payout;
import com.example.payment.entity.Transaction;
import com.example.payment.entity.WebhookAudit;
import com.example.payment.exception.EntityNotFoundException;
import com.example.payment.exception.ValidationException;
import com.example.payment.repository.PayoutRepository;
import com.example.payment.repository.TransactionRepository;
import com.example.payment.repository.WebhookAuditRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WebhookServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private PayoutRepository payoutRepository;

    @Mock
    private WebhookAuditRepository webhookAuditRepository;

    @Mock
    private WebhookDeliveryService webhookDeliveryService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private WebhookService webhookService;

    private Transaction pendingTransaction;
    private Payout pendingPayout;

    @BeforeEach
    void setUp() throws Exception {
        pendingTransaction = new Transaction();
        pendingTransaction.setId(1L);
        pendingTransaction.setStatus(OperationStatus.PENDING);

        pendingPayout = new Payout();
        pendingPayout.setId(1L);
        pendingPayout.setStatus(OperationStatus.PENDING);

        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
    }

    @Test
    @DisplayName("processTransactionWebhook — PENDING транзакция обновляется до SUCCESS")
    void processTransactionWebhook_pending_updatedToSuccess() {
        StatusUpdate payload = buildStatusUpdate(1L, StatusUpdate.StatusEnum.SUCCESS);

        when(transactionRepository.findById(1L)).thenReturn(Optional.of(pendingTransaction));

        webhookService.processTransactionWebhook(payload);

        verify(transactionRepository).save(pendingTransaction);
        verify(webhookAuditRepository).save(any(WebhookAudit.class));
        verify(webhookDeliveryService).deliver(any(), eq("TRANSACTION"), eq(1L), eq("SUCCESS"));
        assert pendingTransaction.getStatus() == OperationStatus.SUCCESS;
    }

    @Test
    @DisplayName("processTransactionWebhook — PENDING транзакция обновляется до FAILED")
    void processTransactionWebhook_pending_updatedToFailed() {
        StatusUpdate payload = buildStatusUpdate(1L, StatusUpdate.StatusEnum.FAILED);

        when(transactionRepository.findById(1L)).thenReturn(Optional.of(pendingTransaction));

        webhookService.processTransactionWebhook(payload);

        verify(transactionRepository).save(pendingTransaction);
        verify(webhookDeliveryService).deliver(any(), eq("TRANSACTION"), eq(1L), eq("FAILED"));
        assert pendingTransaction.getStatus() == OperationStatus.FAILED;
    }

    @Test
    @DisplayName("processTransactionWebhook — идемпотентность: уже SUCCESS — только audit, delivery не вызывается")
    void processTransactionWebhook_alreadySuccess_onlyAuditSaved() {
        pendingTransaction.setStatus(OperationStatus.SUCCESS);
        StatusUpdate payload = buildStatusUpdate(1L, StatusUpdate.StatusEnum.SUCCESS);

        when(transactionRepository.findById(1L)).thenReturn(Optional.of(pendingTransaction));

        webhookService.processTransactionWebhook(payload);

        verify(webhookAuditRepository).save(any(WebhookAudit.class));
        verify(transactionRepository, never()).save(any());
        verify(webhookDeliveryService, never()).deliver(any(), any(), any(), any());
    }

    @Test
    @DisplayName("processTransactionWebhook — транзакция не найдена бросает EntityNotFoundException")
    void processTransactionWebhook_notFound_throwsEntityNotFoundException() {
        StatusUpdate payload = buildStatusUpdate(99L, StatusUpdate.StatusEnum.SUCCESS);

        when(transactionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> webhookService.processTransactionWebhook(payload))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Transaction not found: 99");

        verify(webhookAuditRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
        verify(webhookDeliveryService, never()).deliver(any(), any(), any(), any());
    }

    @Test
    @DisplayName("processPayoutWebhook — PENDING выплата обновляется до SUCCESS")
    void processPayoutWebhook_pending_updatedToSuccess() {
        StatusUpdate payload = buildStatusUpdate(1L, StatusUpdate.StatusEnum.SUCCESS);

        when(payoutRepository.findById(1L)).thenReturn(Optional.of(pendingPayout));

        webhookService.processPayoutWebhook(payload);

        verify(payoutRepository).save(pendingPayout);
        verify(webhookAuditRepository).save(any(WebhookAudit.class));
        verify(webhookDeliveryService).deliver(any(), eq("PAYOUT"), eq(1L), eq("SUCCESS"));
        assert pendingPayout.getStatus() == OperationStatus.SUCCESS;
    }

    @Test
    @DisplayName("processPayoutWebhook — идемпотентность: уже FAILED — только audit, delivery не вызывается")
    void processPayoutWebhook_alreadyFailed_onlyAuditSaved() {
        pendingPayout.setStatus(OperationStatus.FAILED);
        StatusUpdate payload = buildStatusUpdate(1L, StatusUpdate.StatusEnum.FAILED);

        when(payoutRepository.findById(1L)).thenReturn(Optional.of(pendingPayout));

        webhookService.processPayoutWebhook(payload);

        verify(webhookAuditRepository).save(any(WebhookAudit.class));
        verify(payoutRepository, never()).save(any());
        verify(webhookDeliveryService, never()).deliver(any(), any(), any(), any());
    }

    @Test
    @DisplayName("processPayoutWebhook — выплата не найдена бросает EntityNotFoundException")
    void processPayoutWebhook_notFound_throwsEntityNotFoundException() {
        StatusUpdate payload = buildStatusUpdate(99L, StatusUpdate.StatusEnum.SUCCESS);

        when(payoutRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> webhookService.processPayoutWebhook(payload))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Payout not found: 99");

        verify(webhookDeliveryService, never()).deliver(any(), any(), any(), any());
    }

    private StatusUpdate buildStatusUpdate(Long id, StatusUpdate.StatusEnum status) {
        StatusUpdate payload = new StatusUpdate();
        payload.setId(id);
        payload.setStatus(status);
        return payload;
    }
}