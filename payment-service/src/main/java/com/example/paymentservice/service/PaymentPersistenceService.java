package com.example.paymentservice.service;

import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.entity.PaymentMethod;
import com.example.paymentservice.entity.PaymentStatus;
import com.example.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentPersistenceService {

    private final PaymentRepository paymentRepository;

    @Transactional
    public Payment createPending(PaymentMethod paymentMethod,
                                 String internalTransactionUid,
                                 BigDecimal amount,
                                 String currency) {
        Payment payment = new Payment();
        payment.setPaymentMethod(paymentMethod);
        payment.setInternalTransactionUid(internalTransactionUid);
        payment.setAmount(amount);
        payment.setCurrency(currency);
        payment.setStatus(PaymentStatus.PENDING);

        Payment saved = paymentRepository.save(payment);
        log.info("Payment created: id={} internalTransactionUid={} status=PENDING",
                saved.getId(), internalTransactionUid);
        return saved;
    }

    @Transactional
    public void markCompleted(Payment payment, String externalTransactionId) {
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setExternalTransactionId(externalTransactionId);
        paymentRepository.save(payment);
        log.info("Payment completed: id={} externalTransactionId={}",
                payment.getId(), externalTransactionId);
    }

    @Transactional
    public void markFailed(Payment payment) {
        payment.setStatus(PaymentStatus.FAILED);
        paymentRepository.save(payment);
        log.info("Payment failed: id={} internalTransactionUid={}",
                payment.getId(), payment.getInternalTransactionUid());
    }
}