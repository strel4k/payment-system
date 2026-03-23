package com.example.paymentservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "payment_method_definitions")
@Getter
@Setter
@NoArgsConstructor
public class PaymentMethodDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_method_id")
    private PaymentMethod paymentMethod;

    @Column(name = "currency_code", length = 3)
    private String currencyCode;

    @Column(name = "country_alpha3_code", length = 3)
    private String countryAlpha3Code;

    @Column(name = "is_all_currencies")
    private Boolean isAllCurrencies = Boolean.FALSE;

    @Column(name = "is_all_countries")
    private Boolean isAllCountries = Boolean.FALSE;

    @Column(name = "is_priority")
    private Boolean isPriority = Boolean.FALSE;

    @Column(name = "is_active")
    private Boolean isActive = Boolean.TRUE;
}