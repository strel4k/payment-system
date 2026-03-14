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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final TransactionRepository transactionRepository;
    private final PayoutRepository payoutRepository;
    private final WebhookAuditRepository webhookAuditRepository;
    private final WebhookDeliveryService webhookDeliveryService;
    private final ObjectMapper objectMapper;

    @Transactional
    public void processTransactionWebhook(StatusUpdate payload) {
        Transaction transaction = transactionRepository.findById(payload.getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Transaction not found: " + payload.getId()));

        OperationStatus newStatus = parseStatus(payload.getStatus().getValue());

        saveAudit("TRANSACTION", payload);

        if (transaction.getStatus() != OperationStatus.PENDING) {
            log.warn("Transaction {} already in terminal status {}, webhook ignored (audit saved)",
                    transaction.getId(), transaction.getStatus());
            return;
        }

        transaction.setStatus(newStatus);
        transactionRepository.save(transaction);
        log.info("Transaction {} status updated to {}", transaction.getId(), newStatus);

        webhookDeliveryService.deliver(
                transaction.getNotificationUrl(),
                "TRANSACTION",
                transaction.getId(),
                newStatus.name()
        );
    }

    @Transactional
    public void processPayoutWebhook(StatusUpdate payload) {
        Payout payout = payoutRepository.findById(payload.getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Payout not found: " + payload.getId()));

        OperationStatus newStatus = parseStatus(payload.getStatus().getValue());

        saveAudit("PAYOUT", payload);

        if (payout.getStatus() != OperationStatus.PENDING) {
            log.warn("Payout {} already in terminal status {}, webhook ignored (audit saved)",
                    payout.getId(), payout.getStatus());
            return;
        }

        payout.setStatus(newStatus);
        payoutRepository.save(payout);
        log.info("Payout {} status updated to {}", payout.getId(), newStatus);

        webhookDeliveryService.deliver(
                payout.getNotificationUrl(),
                "PAYOUT",
                payout.getId(),
                newStatus.name()
        );
    }

    private OperationStatus parseStatus(String status) {
        try {
            OperationStatus parsed = OperationStatus.valueOf(status);
            if (parsed == OperationStatus.PENDING) {
                throw new ValidationException("Webhook status cannot be PENDING");
            }
            return parsed;
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Unknown status: " + status);
        }
    }

    private void saveAudit(String eventType, StatusUpdate payload) {
        WebhookAudit audit = new WebhookAudit();
        audit.setEventType(eventType);
        audit.setEntityId(payload.getId());
        audit.setPayload(toJson(payload));
        webhookAuditRepository.save(audit);
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize webhook payload to JSON", e);
            return "{}";
        }
    }
}