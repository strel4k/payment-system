package com.example.payment.repository;

import com.example.payment.entity.Merchant;
import com.example.payment.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByIdAndMerchant(Long id, Merchant merchant);

    List<Transaction> findAllByMerchantAndCreatedAtBetween(
            Merchant merchant,
            LocalDateTime from,
            LocalDateTime to
    );
}