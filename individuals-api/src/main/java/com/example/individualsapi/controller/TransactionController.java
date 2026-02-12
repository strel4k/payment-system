package com.example.individualsapi.controller;

import com.example.individualsapi.client.TransactionServiceClient;
import com.example.individualsapi.client.dto.transaction.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping(path = "/v1/transactions", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionServiceClient transactionServiceClient;

    @PostMapping(value = "/{type}/init", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<TransactionInitResponse> initTransaction(
            @PathVariable String type,
            @RequestBody TransactionInitRequest request,
            ServerWebExchange exchange) {
        String authToken = getAuthToken(exchange);
        return transactionServiceClient.initTransaction(type, request, authToken);
    }

    @PostMapping(value = "/{type}/confirm", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<TransactionConfirmResponse> confirmTransaction(
            @PathVariable String type,
            @RequestBody TransactionConfirmRequest request,
            ServerWebExchange exchange) {
        String authToken = getAuthToken(exchange);
        return transactionServiceClient.confirmTransaction(type, request, authToken);
    }

    @GetMapping("/{transactionUid}/status")
    public Mono<TransactionStatusResponse> getTransactionStatus(
            @PathVariable UUID transactionUid,
            ServerWebExchange exchange) {
        String authToken = getAuthToken(exchange);
        return transactionServiceClient.getTransactionStatus(transactionUid, authToken);
    }

    private String getAuthToken(ServerWebExchange exchange) {
        return exchange.getRequest().getHeaders().getFirst("Authorization");
    }
}