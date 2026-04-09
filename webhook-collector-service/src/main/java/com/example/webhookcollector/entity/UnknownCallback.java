package com.example.webhookcollector.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "unknown_callbacks")
@Getter
@Setter
@NoArgsConstructor
public class UnknownCallback extends BaseEntity {

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;
}