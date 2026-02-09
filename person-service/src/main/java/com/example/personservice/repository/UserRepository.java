package com.example.personservice.repository;

import com.example.personservice.entity.UserEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    @EntityGraph(attributePaths = {"address", "address.country", "individual"})
    Optional<UserEntity> findByEmailIgnoreCase(String email);

    @Override
    @EntityGraph(attributePaths = {"address", "address.country", "individual"})
    Optional<UserEntity> findById(UUID uuid);
}