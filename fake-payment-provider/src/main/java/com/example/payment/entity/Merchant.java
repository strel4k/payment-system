package com.example.payment.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "merchants")
@Getter
@Setter
@NoArgsConstructor
public class Merchant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "merchant_id", nullable = false, unique = true, length = 50)
    private String merchantId;

    @Column(name = "secret_key", nullable = false, length = 255)
    private String secretKey;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}