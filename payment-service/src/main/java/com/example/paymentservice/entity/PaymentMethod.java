package com.example.paymentservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "payment_methods")
@Getter
@Setter
@NoArgsConstructor
public class PaymentMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id")
    private PaymentProvider provider;

    @Column(name = "type", nullable = false, length = 32)
    private String type;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @Column(name = "name", nullable = false, length = 64)
    private String name;

    @Column(name = "is_active")
    private Boolean isActive = Boolean.TRUE;

    @Column(name = "provider_unique_id", nullable = false, length = 128)
    private String providerUniqueId;

    @Column(name = "provider_method_type", nullable = false, length = 32)
    private String providerMethodType;

    @Column(name = "logo", columnDefinition = "TEXT")
    private String logo;

    @Column(name = "profile_type", nullable = false, length = 24)
    private String profileType = "INDIVIDUAL";

    @OneToMany(mappedBy = "paymentMethod", fetch = FetchType.LAZY)
    private List<PaymentMethodDefinition> definitions = new ArrayList<>();

    @OneToMany(mappedBy = "paymentMethod", fetch = FetchType.LAZY)
    private List<PaymentMethodRequiredField> requiredFields = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        modifiedAt = LocalDateTime.now();
    }
}