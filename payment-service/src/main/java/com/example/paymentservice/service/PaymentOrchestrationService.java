package com.example.paymentservice.service;

import com.example.paymentservice.dto.PaymentRequest;
import com.example.paymentservice.dto.PaymentResponse;
import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.entity.PaymentMethod;
import com.example.paymentservice.entity.PaymentOutbox;
import com.example.paymentservice.exception.PaymentMethodNotFoundException;
import com.example.paymentservice.repository.PaymentMethodRepository;
import com.example.paymentservice.repository.PaymentOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentOrchestrationService {

    private final PaymentMethodRepository paymentMethodRepository;
    private final PaymentOutboxRepository paymentOutboxRepository;
    private final PaymentPersistenceService paymentPersistenceService;

    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        log.info("Initiating payment: internalTransactionUid={} methodId={} amount={} currency={}",
                request.getInternalTransactionUid(),
                request.getMethodId(),
                request.getAmount(),
                request.getCurrency());

        PaymentMethod paymentMethod = paymentMethodRepository
                .findById(request.getMethodId())
                .filter(m -> Boolean.TRUE.equals(m.getIsActive()))
                .orElseThrow(() -> new PaymentMethodNotFoundException(request.getMethodId()));

        Payment payment = paymentPersistenceService.createPending(
                paymentMethod,
                request.getInternalTransactionUid() != null
                        ? request.getInternalTransactionUid().toString()
                        : null,
                BigDecimal.valueOf(request.getAmount()),
                request.getCurrency()
        );

        PaymentOutbox outboxEntry = new PaymentOutbox();
        outboxEntry.setPayment(payment);
        outboxEntry.setMethodType(paymentMethod.getProviderMethodType());
        outboxEntry.setAmount(payment.getAmount());
        outboxEntry.setCurrency(payment.getCurrency());
        paymentOutboxRepository.save(outboxEntry);

        log.info("Payment queued for processing: paymentId={} outboxId={}",
                payment.getId(), outboxEntry.getId());

        PaymentResponse response = new PaymentResponse();
        response.setProviderTransactionId(null);
        response.setStatus(PaymentResponse.StatusEnum.PENDING);
        return response;
    }
}