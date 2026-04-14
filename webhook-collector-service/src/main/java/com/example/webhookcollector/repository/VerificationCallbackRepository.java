package com.example.webhookcollector.repository;

import com.example.webhookcollector.entity.VerificationCallback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface VerificationCallbackRepository
        extends JpaRepository<VerificationCallback, UUID> {
}