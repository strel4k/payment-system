package com.example.transaction.controller;

import com.example.transaction.api.TransactionsApi;
import com.example.transaction.dto.TransactionConfirmRequest;
import com.example.transaction.dto.TransactionConfirmResponse;
import com.example.transaction.dto.TransactionInitRequest;
import com.example.transaction.dto.TransactionInitResponse;
import com.example.transaction.dto.TransactionPageResponse;
import com.example.transaction.dto.TransactionStatusResponse;
import com.example.transaction.exception.InvalidTransactionException;
import com.example.transaction.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;


@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TransactionController implements TransactionsApi {

    private final TransactionService transactionService;

    @Override
    public ResponseEntity<TransactionInitResponse> initTransaction(
            String type,
            TransactionInitRequest transactionInitRequest) {

        log.info("POST /transactions/{}/init - wallet: {}", type, transactionInitRequest.getWalletUid());

        TransactionInitResponse response = switch (type.toLowerCase()) {
            case "deposit" -> transactionService.initDeposit(transactionInitRequest);
            case "withdrawal" -> transactionService.initWithdrawal(transactionInitRequest);
            case "transfer" -> transactionService.initTransfer(transactionInitRequest);
            default -> throw new InvalidTransactionException("Invalid transaction type: " + type);
        };

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<TransactionConfirmResponse> confirmTransaction(
            String type,
            TransactionConfirmRequest transactionConfirmRequest) {

        log.info("POST /transactions/{}/confirm - requestUid: {}",
                type, transactionConfirmRequest.getRequestUid());

        TransactionConfirmResponse response = switch (type.toLowerCase()) {
            case "deposit" -> transactionService.confirmDeposit(transactionConfirmRequest);
            case "withdrawal" -> transactionService.confirmWithdrawal(transactionConfirmRequest);
            case "transfer" -> transactionService.confirmTransfer(transactionConfirmRequest);
            default -> throw new InvalidTransactionException("Invalid transaction type: " + type);
        };

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    public ResponseEntity<TransactionStatusResponse> getTransactionStatus(UUID transactionUid) {
        log.info("GET /transactions/{}/status", transactionUid);

        TransactionStatusResponse response = transactionService.getTransactionStatus(transactionUid);

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<TransactionPageResponse> searchTransactions(
            UUID userUid,
            UUID walletUid,
            String type,
            String status,
            OffsetDateTime dateFrom,
            OffsetDateTime dateTo,
            Integer page,
            Integer size) {

        log.info("GET /transactions - userUid: {}, walletUid: {}, type: {}, status: {}",
                userUid, walletUid, type, status);

        LocalDateTime localDateFrom = dateFrom != null ? dateFrom.toLocalDateTime() : null;
        LocalDateTime localDateTo = dateTo != null ? dateTo.toLocalDateTime() : null;

        TransactionPageResponse response = transactionService.searchTransactions(
                userUid, walletUid, type, status, localDateFrom, localDateTo, page, size);

        return ResponseEntity.ok(response);
    }
}