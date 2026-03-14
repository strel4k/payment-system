package com.example.payment.repository;

import com.example.payment.entity.WebhookAudit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookAuditRepository extends JpaRepository<WebhookAudit, Long> {
}