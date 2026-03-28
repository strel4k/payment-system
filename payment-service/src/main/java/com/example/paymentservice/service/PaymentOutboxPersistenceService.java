package com.example.paymentservice.service;

import com.example.paymentservice.entity.PaymentOutbox;
import com.example.paymentservice.entity.PaymentOutboxStatus;
import com.example.paymentservice.repository.PaymentOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentOutboxPersistenceService {

    private final PaymentOutboxRepository outboxRepository;
    private final PaymentPersistenceService paymentPersistenceService;

    @Transactional(readOnly = true)
    public List<PaymentOutbox> findPending() {
        return outboxRepository.findByStatusWithPayment(PaymentOutboxStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public List<PaymentOutbox> findProcessing() {
        return outboxRepository.findByStatusWithPayment(PaymentOutboxStatus.PROCESSING);
    }

    @Transactional
    public void markProcessing(PaymentOutbox entry) {
        entry.setStatus(PaymentOutboxStatus.PROCESSING);
        entry.setAttempts(entry.getAttempts() + 1);
        outboxRepository.save(entry);
    }

    @Transactional
    public void markSuccess(Integer entryId, String externalTransactionId) {
        PaymentOutbox fresh = findProcessingById(entryId);
        if (fresh == null) return;

        paymentPersistenceService.markCompleted(fresh.getPayment(), externalTransactionId);
        fresh.setStatus(PaymentOutboxStatus.COMPLETED);
        fresh.setProcessedAt(LocalDateTime.now());
        outboxRepository.save(fresh);
    }

    @Transactional
    public void markFailure(Integer entryId, int attempts, int maxAttempts, String error) {
        PaymentOutbox fresh = findProcessingById(entryId);
        if (fresh == null) return;

        fresh.setLastError(error);

        if (attempts >= maxAttempts) {
            log.error("Outbox entry={} exhausted all {} attempts, marking FAILED", entryId, maxAttempts);
            paymentPersistenceService.markFailed(fresh.getPayment());
            fresh.setStatus(PaymentOutboxStatus.FAILED);
            fresh.setProcessedAt(LocalDateTime.now());
        } else {
            fresh.setStatus(PaymentOutboxStatus.PENDING);
        }

        outboxRepository.save(fresh);
    }

    private PaymentOutbox findProcessingById(Integer id) {
        return outboxRepository.findByStatusWithPayment(PaymentOutboxStatus.PROCESSING)
                .stream()
                .filter(o -> o.getId().equals(id))
                .findFirst()
                .orElse(null);
    }
}