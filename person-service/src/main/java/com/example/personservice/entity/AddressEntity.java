package com.example.personservice.entity;

import jakarta.persistence.*;
import org.hibernate.envers.Audited;

import java.time.LocalDateTime;
import java.util.UUID;

@Audited
@Entity
@Table(schema = "person", name = "addresses")
public class AddressEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    private LocalDateTime created;
    private LocalDateTime updated;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_id")
    private CountryEntity country;

    @Column(length = 128)
    private String address;

    @Column(name = "zip_code", length = 32)
    private String zipCode;

    private LocalDateTime archived;

    @Column(length = 32)
    private String city;

    @Column(length = 32)
    private String state;

    @PrePersist
    void prePersist() {
        var now = LocalDateTime.now();
        if (id == null) id = UUID.randomUUID();
        if (created == null) created = now;
        if (updated == null) updated = now;
        if (archived == null) archived = LocalDateTime.of(9999, 12, 31, 0, 0); // активный адрес
    }

    @PreUpdate
    void preUpdate() {
        updated = LocalDateTime.now();
    }

    // getters/setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public LocalDateTime getCreated() { return created; }
    public void setCreated(LocalDateTime created) { this.created = created; }

    public LocalDateTime getUpdated() { return updated; }
    public void setUpdated(LocalDateTime updated) { this.updated = updated; }

    public CountryEntity getCountry() { return country; }
    public void setCountry(CountryEntity country) { this.country = country; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getZipCode() { return zipCode; }
    public void setZipCode(String zipCode) { this.zipCode = zipCode; }

    public LocalDateTime getArchived() { return archived; }
    public void setArchived(LocalDateTime archived) { this.archived = archived; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
}