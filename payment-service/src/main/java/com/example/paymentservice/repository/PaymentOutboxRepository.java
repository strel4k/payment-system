package com.example.paymentservice.repository;

import com.example.paymentservice.entity.PaymentOutbox;
import com.example.paymentservice.entity.PaymentOutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PaymentOutboxRepository extends JpaRepository<PaymentOutbox, Integer> {

    @Query("SELECT po FROM PaymentOutbox po JOIN FETCH po.payment WHERE po.status = :status ORDER BY po.createdAt ASC")
    List<PaymentOutbox> findByStatusWithPayment(@Param("status") PaymentOutboxStatus status);
}