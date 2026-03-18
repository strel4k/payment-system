package com.example.payment.repository;

import com.example.payment.entity.Merchant;
import com.example.payment.entity.Payout;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PayoutRepository extends JpaRepository<Payout, Long> {

    Optional<Payout> findByIdAndMerchant(Long id, Merchant merchant);

    List<Payout> findAllByMerchantAndCreatedAtBetween(
            Merchant merchant,
            LocalDateTime from,
            LocalDateTime to
    );

    List<Payout> findAllByMerchant(Merchant merchant);
}