package com.example.paymentservice.controller;

import com.example.paymentservice.dto.PaymentMethodResponse;
import com.example.paymentservice.dto.PaymentRequest;
import com.example.paymentservice.dto.PaymentResponse;
import com.example.paymentservice.service.PaymentMethodService;
import com.example.paymentservice.service.PaymentOrchestrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentMethodService paymentMethodService;
    private final PaymentOrchestrationService paymentOrchestrationService;

    @GetMapping("/payment-methods/{currencyCode}/{countryCode}")
    public ResponseEntity<List<PaymentMethodResponse>> getPaymentMethods(
            @PathVariable String currencyCode,
            @PathVariable String countryCode) {

        List<PaymentMethodResponse> result = paymentMethodService.getPaymentMethods(
                currencyCode, countryCode
        );
        return ResponseEntity.ok(result);
    }

    @PostMapping("/payments")
    public ResponseEntity<PaymentResponse> processPayment(@RequestBody PaymentRequest request) {
        log.info("POST /payments internalTransactionUid={} methodId={}",
                request.getInternalTransactionUid(), request.getMethodId());
        PaymentResponse response = paymentOrchestrationService.processPayment(request);
        return ResponseEntity.ok(response);
    }
}