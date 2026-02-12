package com.example.transaction.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "wallet_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletType extends BaseEntity {

    @Column(name = "name", nullable = false, length = 32)
    private String name;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "status", nullable = false, length = 18)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @Column(name = "user_type", length = 15)
    private String userType;

    @Column(name = "creator")
    private String creator;

    @Column(name = "modifier")
    private String modifier;
}