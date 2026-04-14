package com.example.webhookcollector.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "verification_callbacks")
@Getter
@Setter
@NoArgsConstructor
public class VerificationCallback {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "uid", updatable = false, nullable = false)
    private UUID uid;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "transaction_uid")
    private UUID transactionUid;

    @Column(name = "profile_uid", nullable = false)
    private UUID profileUid;

    @Column(name = "status")
    private String status;

    @Column(name = "type")
    private String type;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        modifiedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        modifiedAt = LocalDateTime.now();
    }
}