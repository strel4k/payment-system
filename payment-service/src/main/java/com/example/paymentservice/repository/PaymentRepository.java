package com.example.paymentservice.repository;

import com.example.paymentservice.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    Optional<Payment> findByInternalTransactionUid(String internalTransactionUid);

    Optional<Payment> findByExternalTransactionId(String externalTransactionId);
}