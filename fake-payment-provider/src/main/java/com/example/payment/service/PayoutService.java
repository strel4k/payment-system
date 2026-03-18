package com.example.payment.service;

import com.example.payment.dto.Payout;
import com.example.payment.dto.PayoutRequest;
import com.example.payment.entity.Merchant;
import com.example.payment.exception.EntityNotFoundException;
import com.example.payment.exception.ValidationException;
import com.example.payment.mapper.PayoutMapper;
import com.example.payment.repository.PayoutRepository;
import com.example.payment.security.MerchantPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayoutService {

    private final PayoutRepository payoutRepository;
    private final PayoutMapper payoutMapper;

    @Transactional
    public Payout createPayout(PayoutRequest request) {
        Merchant merchant = currentMerchant();

        validatePayoutRequest(request);

        com.example.payment.entity.Payout entity = new com.example.payment.entity.Payout();
        entity.setMerchant(merchant);
        entity.setAmount(BigDecimal.valueOf(request.getAmount()));
        entity.setCurrency(request.getCurrency().toUpperCase());
        entity.setNotificationUrl(request.getNotificationUrl());

        com.example.payment.entity.Payout saved = payoutRepository.save(entity);
        log.info("Payout created: id={}, merchant={}, amount={} {}",
                saved.getId(), merchant.getMerchantId(), saved.getAmount(), saved.getCurrency());

        return payoutMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public Payout getPayout(Long id) {
        Merchant merchant = currentMerchant();

        com.example.payment.entity.Payout entity = payoutRepository
                .findByIdAndMerchant(id, merchant)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Payout not found: " + id));

        return payoutMapper.toDto(entity);
    }

    @Transactional(readOnly = true)
    public List<Payout> listPayouts(LocalDateTime startDate, LocalDateTime endDate) {
        Merchant merchant = currentMerchant();

        List<com.example.payment.entity.Payout> payouts;

        if (startDate != null && endDate != null) {
            if (startDate.isAfter(endDate)) {
                throw new ValidationException("start_date must be before end_date");
            }
            payouts = payoutRepository.findAllByMerchantAndCreatedAtBetween(
                    merchant, startDate, endDate);
        } else {
            payouts = payoutRepository.findAllByMerchant(merchant);
        }

        return payouts.stream()
                .map(payoutMapper::toDto)
                .collect(Collectors.toList());
    }

    private void validatePayoutRequest(PayoutRequest request) {
        if (request.getAmount() == null || request.getAmount() <= 0) {
            throw new ValidationException("amount must be greater than 0");
        }
        if (request.getCurrency() == null || request.getCurrency().length() != 3) {
            throw new ValidationException("currency must be a 3-letter ISO 4217 code");
        }
    }

    private Merchant currentMerchant() {
        MerchantPrincipal principal = (MerchantPrincipal) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
        return principal.getMerchant();
    }
}