package com.example.personservice.entity;

import jakarta.persistence.*;
import org.hibernate.envers.Audited;

import java.time.LocalDateTime;
import java.util.UUID;

@Audited
@Entity
@Table(schema = "person", name = "users")
public class UserEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "secret_key", length = 32)
    private String secretKey;

    @Column(length = 1024)
    private String email;

    private LocalDateTime created;
    private LocalDateTime updated;

    @Column(name = "first_name", length = 32)
    private String firstName;

    @Column(name = "last_name", length = 32)
    private String lastName;

    private Boolean filled;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "address_id")
    private AddressEntity address;

    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private IndividualEntity individual;

    @PrePersist
    void prePersist() {
        var now = LocalDateTime.now();
        if (id == null) id = UUID.randomUUID();
        if (created == null) created = now;
        if (updated == null) updated = now;
        if (filled == null) filled = Boolean.FALSE;
    }

    @PreUpdate
    void preUpdate() {
        updated = LocalDateTime.now();
    }

    // getters/setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public LocalDateTime getCreated() { return created; }
    public void setCreated(LocalDateTime created) { this.created = created; }

    public LocalDateTime getUpdated() { return updated; }
    public void setUpdated(LocalDateTime updated) { this.updated = updated; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public Boolean getFilled() { return filled; }
    public void setFilled(Boolean filled) { this.filled = filled; }

    public AddressEntity getAddress() { return address; }
    public void setAddress(AddressEntity address) { this.address = address; }

    public IndividualEntity getIndividual() { return individual; }
    public void setIndividual(IndividualEntity individual) { this.individual = individual; }
}