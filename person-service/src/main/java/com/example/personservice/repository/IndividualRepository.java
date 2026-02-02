package com.example.personservice.repository;

import com.example.personservice.entity.IndividualEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IndividualRepository extends JpaRepository<IndividualEntity, UUID> {
    Optional<IndividualEntity> findByUserId(UUID userId);
}
