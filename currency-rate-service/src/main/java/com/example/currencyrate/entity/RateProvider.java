package com.example.currencyrate.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "rate_providers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RateProvider {

    @Id
    @Column(name = "provider_code", nullable = false)
    private String providerCode;

    @Column(name = "provider_name", nullable = false)
    private String providerName;

    @Column(name = "priority", nullable = false)
    private int priority = 1;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}