package com.example.payment.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "webhooks")
@Getter
@Setter
@NoArgsConstructor
public class WebhookAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "payload", columnDefinition = "text")
    private String payload;

    @Column(name = "received_at", updatable = false)
    private LocalDateTime receivedAt;

    @Column(name = "notification_url", length = 2048)
    private String notificationUrl;

    @PrePersist
    protected void onCreate() {
        receivedAt = LocalDateTime.now();
    }
}