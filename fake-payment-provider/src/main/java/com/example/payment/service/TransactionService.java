package com.example.payment.service;

import com.example.payment.dto.Transaction;
import com.example.payment.dto.TransactionRequest;
import com.example.payment.entity.Merchant;
import com.example.payment.exception.EntityNotFoundException;
import com.example.payment.exception.ValidationException;
import com.example.payment.mapper.TransactionMapper;
import com.example.payment.repository.TransactionRepository;
import com.example.payment.security.MerchantPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private static final Set<String> SUPPORTED_METHODS = Set.of("CARD", "BANK_TRANSFER", "CRYPTO");

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;

    @Transactional
    public Transaction createTransaction(TransactionRequest request) {
        Merchant merchant = currentMerchant();

        validateTransactionRequest(request);

        com.example.payment.entity.Transaction entity = new com.example.payment.entity.Transaction();
        entity.setMerchant(merchant);
        entity.setAmount(BigDecimal.valueOf(request.getAmount()));
        entity.setCurrency(request.getCurrency().toUpperCase());
        entity.setMethod(request.getMethod().toUpperCase());
        entity.setDescription(request.getDescription());
        entity.setNotificationUrl(request.getNotificationUrl());

        com.example.payment.entity.Transaction saved = transactionRepository.save(entity);
        log.info("Transaction created: id={}, merchant={}, amount={} {}",
                saved.getId(), merchant.getMerchantId(), saved.getAmount(), saved.getCurrency());

        return transactionMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public Transaction getTransaction(Long id) {
        Merchant merchant = currentMerchant();

        com.example.payment.entity.Transaction entity = transactionRepository
                .findByIdAndMerchant(id, merchant)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Transaction not found: " + id));

        return transactionMapper.toDto(entity);
    }

    @Transactional(readOnly = true)
    public List<Transaction> listTransactions(LocalDateTime startDate, LocalDateTime endDate) {
        Merchant merchant = currentMerchant();

        if (startDate.isAfter(endDate)) {
            throw new ValidationException("start_date must be before end_date");
        }

        return transactionRepository
                .findAllByMerchantAndCreatedAtBetween(merchant, startDate, endDate)
                .stream()
                .map(transactionMapper::toDto)
                .collect(Collectors.toList());
    }

    private void validateTransactionRequest(TransactionRequest request) {
        if (request.getAmount() == null || request.getAmount() <= 0) {
            throw new ValidationException("amount must be greater than 0");
        }
        if (request.getCurrency() == null || request.getCurrency().length() != 3) {
            throw new ValidationException("currency must be a 3-letter ISO 4217 code");
        }
        if (request.getMethod() == null || !SUPPORTED_METHODS.contains(request.getMethod().toUpperCase())) {
            throw new ValidationException(
                    "Unsupported payment method: " + request.getMethod() + ". Supported: " + SUPPORTED_METHODS);
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