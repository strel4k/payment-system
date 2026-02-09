package com.example.personservice.entity;

import jakarta.persistence.*;
import org.hibernate.envers.Audited;

import java.time.LocalDateTime;

@Audited
@Entity
@Table(schema = "person", name = "countries")
public class CountryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private LocalDateTime created;
    private LocalDateTime updated;

    @Column(length = 32)
    private String name;

    @Column(length = 2)
    private String alpha2;

    @Column(length = 3)
    private String alpha3;

    @Column(length = 32)
    private String status;

    @PrePersist
    void prePersist() {
        var now = LocalDateTime.now();
        if (created == null) created = now;
        if (updated == null) updated = now;
    }

    @PreUpdate
    void preUpdate() {
        updated = LocalDateTime.now();
    }

    // getters/setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public LocalDateTime getCreated() { return created; }
    public void setCreated(LocalDateTime created) { this.created = created; }

    public LocalDateTime getUpdated() { return updated; }
    public void setUpdated(LocalDateTime updated) { this.updated = updated; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAlpha2() { return alpha2; }
    public void setAlpha2(String alpha2) { this.alpha2 = alpha2; }

    public String getAlpha3() { return alpha3; }
    public void setAlpha3(String alpha3) { this.alpha3 = alpha3; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}