package com.example.paymentservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_method_required_fields")
@Getter
@Setter
@NoArgsConstructor
public class PaymentMethodRequiredField {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "uid", updatable = false, nullable = false)
    private UUID uid;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_method_id")
    private PaymentMethod paymentMethod;

    @Column(name = "payment_type", nullable = false, length = 64)
    private String paymentType;

    @Column(name = "country_alpha3_code", length = 3)
    private String countryAlpha3Code;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "data_type", nullable = false, length = 128)
    private String dataType;

    @Column(name = "validation_type", length = 128)
    private String validationType;

    @Column(name = "validation_rule", length = 256)
    private String validationRule;

    @Column(name = "default_value", length = 128)
    private String defaultValue;

    @Column(name = "values_options", columnDefinition = "TEXT")
    private String valuesOptions;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "placeholder", length = 255)
    private String placeholder;

    @Column(name = "representation_name", length = 255)
    private String representationName;

    @Column(name = "language", length = 2)
    private String language;

    @Column(name = "is_active")
    private Boolean isActive = Boolean.TRUE;

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