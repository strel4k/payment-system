package com.example.webhookcollector.repository;

import com.example.webhookcollector.entity.UnknownCallback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UnknownCallbackRepository
        extends JpaRepository<UnknownCallback, UUID> {
}