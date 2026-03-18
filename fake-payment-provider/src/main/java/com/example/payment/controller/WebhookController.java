package com.example.payment.controller;

import com.example.payment.dto.StatusUpdate;
import com.example.payment.service.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService webhookService;

    @PostMapping("/transaction")
    public ResponseEntity<Void> handleTransactionWebhook(@RequestBody StatusUpdate payload) {
        log.info("POST /webhook/transaction id={} status={}", payload.getId(), payload.getStatus());
        webhookService.processTransactionWebhook(payload);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/payout")
    public ResponseEntity<Void> handlePayoutWebhook(@RequestBody StatusUpdate payload) {
        log.info("POST /webhook/payout id={} status={}", payload.getId(), payload.getStatus());
        webhookService.processPayoutWebhook(payload);
        return ResponseEntity.ok().build();
    }
}