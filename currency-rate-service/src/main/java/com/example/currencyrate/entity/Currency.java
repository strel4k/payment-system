package com.example.currencyrate.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "currencies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Currency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "iso_code", nullable = false, unique = true)
    private String isoCode;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "symbol")
    private String symbol;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}