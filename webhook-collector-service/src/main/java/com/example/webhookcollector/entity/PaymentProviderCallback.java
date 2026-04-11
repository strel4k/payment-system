package com.example.webhookcollector.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "payment_provider_callbacks")
@Getter
@Setter
@NoArgsConstructor
public class PaymentProviderCallback extends BaseEntity {

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "provider_transaction_uid")
    private UUID providerTransactionUid;

    @Column(name = "type")
    private String type;

    @Column(name = "provider")
    private String provider;
}