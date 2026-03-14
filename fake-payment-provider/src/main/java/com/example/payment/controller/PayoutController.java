package com.example.payment.controller;

import com.example.payment.dto.Payout;
import com.example.payment.dto.PayoutRequest;
import com.example.payment.service.PayoutService;
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
@RequestMapping("/api/v1/payouts")
@RequiredArgsConstructor
public class PayoutController {

    private final PayoutService payoutService;

    @PostMapping
    public ResponseEntity<Payout> createPayout(@RequestBody PayoutRequest request) {
        log.info("POST /api/v1/payouts amount={} currency={}",
                request.getAmount(), request.getCurrency());
        Payout created = payoutService.createPayout(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Payout> getPayout(@PathVariable Long id) {
        log.info("GET /api/v1/payouts/{}", id);
        return ResponseEntity.ok(payoutService.getPayout(id));
    }

    @GetMapping
    public ResponseEntity<List<Payout>> listPayouts(
            @RequestParam(value = "start_date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(value = "end_date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        log.info("GET /api/v1/payouts start_date={} end_date={}", startDate, endDate);
        return ResponseEntity.ok(payoutService.listPayouts(startDate, endDate));
    }
}