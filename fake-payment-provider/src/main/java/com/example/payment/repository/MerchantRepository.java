package com.example.payment.repository;

import com.example.payment.entity.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MerchantRepository extends JpaRepository<Merchant, Integer> {

    Optional<Merchant> findByMerchantId(String merchantId);
}