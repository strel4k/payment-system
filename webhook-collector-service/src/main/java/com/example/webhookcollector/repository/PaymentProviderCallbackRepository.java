package com.example.webhookcollector.repository;

import com.example.webhookcollector.entity.PaymentProviderCallback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PaymentProviderCallbackRepository
        extends JpaRepository<PaymentProviderCallback, UUID> {
}