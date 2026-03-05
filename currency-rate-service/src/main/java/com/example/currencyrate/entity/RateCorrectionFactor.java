package com.example.currencyrate.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "rate_correction_factors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RateCorrectionFactor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @Column(name = "source_code", nullable = false, length = 10)
    private String sourceCode;

    @Column(name = "destination_code", nullable = false, length = 10)
    private String destinationCode;

    @Column(nullable = false, precision = 10, scale = 6)
    private BigDecimal factor;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private boolean active;
}