package com.example.transaction.repository;

import com.example.transaction.entity.WalletType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WalletTypeRepository extends JpaRepository<WalletType, UUID> {

    Optional<WalletType> findByName(String name);

    Optional<WalletType> findByCurrencyCode(String currencyCode);

    boolean existsByName(String name);
}