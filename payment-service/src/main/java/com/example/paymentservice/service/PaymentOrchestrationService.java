package com.example.paymentservice.service;

import com.example.paymentservice.client.FakeProviderClient;
import com.example.paymentservice.client.dto.FppTransactionResponse;
import com.example.paymentservice.dto.PaymentRequest;
import com.example.paymentservice.dto.PaymentResponse;
import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.entity.PaymentMethod;
import com.example.paymentservice.exception.PaymentMethodNotFoundException;
import com.example.paymentservice.exception.PaymentProviderException;
import com.example.paymentservice.repository.PaymentMethodRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentOrchestrationService {

    private final PaymentMethodRepository paymentMethodRepository;
    private final PaymentPersistenceService paymentPersistenceService;
    private final FakeProviderClient fakeProviderClient;

    public PaymentResponse processPayment(PaymentRequest request) {
        log.info("Processing payment: internalTransactionUid={} methodId={} amount={} currency={}",
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

        try {
            FppTransactionResponse fppResponse = fakeProviderClient.createTransaction(
                    payment.getAmount(),
                    payment.getCurrency(),
                    paymentMethod.getProviderMethodType()
            );

            paymentPersistenceService.markCompleted(payment, fppResponse.id().toString());

            log.info("Payment processed successfully: id={} providerTransactionId={}",
                    payment.getId(), fppResponse.id());

            PaymentResponse response = new PaymentResponse();
            response.setProviderTransactionId(fppResponse.id().toString());
            response.setStatus(PaymentResponse.StatusEnum.COMPLETED);
            return response;

        } catch (PaymentProviderException e) {
            paymentPersistenceService.markFailed(payment);

            log.error("Payment failed: id={} internalTransactionUid={} reason={}",
                    payment.getId(), payment.getInternalTransactionUid(), e.getMessage());

            throw e;
        }
    }
}