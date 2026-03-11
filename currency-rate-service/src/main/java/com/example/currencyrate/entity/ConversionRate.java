package com.example.currencyrate.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "conversion_rates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversionRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_code", nullable = false)
    private String sourceCode;

    @Column(name = "destination_code", nullable = false)
    private String destinationCode;

    @Column(name = "rate", nullable = false, precision = 20, scale = 8)
    private BigDecimal rate;

    @Column(name = "rate_begin_time", nullable = false)
    private LocalDateTime rateBeginTime;

    @Column(name = "rate_end_time", nullable = false)
    private LocalDateTime rateEndTime;

    @Column(name = "provider_code", nullable = false)
    private String providerCode;
}