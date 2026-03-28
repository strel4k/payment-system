package com.example.paymentservice.service;

import com.example.paymentservice.client.FakeProviderClient;
import com.example.paymentservice.client.dto.FppTransactionResponse;
import com.example.paymentservice.entity.PaymentOutbox;
import com.example.paymentservice.exception.PaymentProviderException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentOutboxProcessor {

    private final PaymentOutboxPersistenceService outboxPersistenceService;
    private final FakeProviderClient fakeProviderClient;

    @Scheduled(fixedDelayString = "${payment.outbox.poll-interval-ms:5000}")
    public void processOutbox() {
        List<PaymentOutbox> pending = outboxPersistenceService.findPending();

        if (pending.isEmpty()) {
            return;
        }

        log.debug("Processing {} outbox entries", pending.size());

        for (PaymentOutbox entry : pending) {
            outboxPersistenceService.markProcessing(entry);
            callFppAndUpdateStatus(entry);
        }
    }

    private void callFppAndUpdateStatus(PaymentOutbox entry) {
        try {
            FppTransactionResponse fppResponse = fakeProviderClient.createTransaction(
                    entry.getAmount(),
                    entry.getCurrency(),
                    entry.getMethodType()
            );
            outboxPersistenceService.markSuccess(entry.getId(), fppResponse.id().toString());
            log.info("Outbox entry={} processed successfully, externalId={}",
                    entry.getId(), fppResponse.id());

        } catch (PaymentProviderException e) {
            log.warn("Outbox entry={} FPP error: {}, attempts={}/{}",
                    entry.getId(), e.getMessage(), entry.getAttempts(), entry.getMaxAttempts());
            outboxPersistenceService.markFailure(
                    entry.getId(), entry.getAttempts(), entry.getMaxAttempts(), e.getMessage());

        } catch (Exception e) {
            log.error("Outbox entry={} unexpected error: {}", entry.getId(), e.getMessage());
            outboxPersistenceService.markFailure(
                    entry.getId(), entry.getAttempts(), entry.getMaxAttempts(), e.getMessage());
        }
    }
}