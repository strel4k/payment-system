package com.example.payment.controller;

import com.example.payment.dto.Transaction;
import com.example.payment.dto.TransactionRequest;
import com.example.payment.exception.ValidationException;
import com.example.payment.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<Transaction> createTransaction(
            @RequestBody TransactionRequest request) {
        log.info("POST /api/v1/transactions amount={} currency={} method={}",
                request.getAmount(), request.getCurrency(), request.getMethod());
        Transaction created = transactionService.createTransaction(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Transaction> getTransaction(@PathVariable Long id) {
        log.info("GET /api/v1/transactions/{}", id);
        return ResponseEntity.ok(transactionService.getTransaction(id));
    }

    @GetMapping
    public ResponseEntity<List<Transaction>> listTransactions(
            @RequestParam("start_date")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam("end_date")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        if (startDate == null || endDate == null) {
            throw new ValidationException("start_date and end_date are required");
        }
        log.info("GET /api/v1/transactions?start_date={}&end_date={}", startDate, endDate);
        return ResponseEntity.ok(transactionService.listTransactions(startDate, endDate));
    }
}