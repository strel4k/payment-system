package com.example.individualsapi.controller;

import com.example.individualsapi.client.TransactionServiceClient;
import com.example.individualsapi.client.dto.transaction.CreateWalletRequest;
import com.example.individualsapi.client.dto.transaction.WalletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping(path = "/v1/wallets", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class WalletController {

    private final TransactionServiceClient transactionServiceClient;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<WalletResponse> createWallet(
            @RequestBody CreateWalletRequest request,
            ServerWebExchange exchange) {
        String authToken = getAuthToken(exchange);
        return transactionServiceClient.createWallet(request, authToken);
    }

    @GetMapping("/{walletUid}")
    public Mono<WalletResponse> getWallet(
            @PathVariable UUID walletUid,
            ServerWebExchange exchange) {
        String authToken = getAuthToken(exchange);
        return transactionServiceClient.getWallet(walletUid, authToken);
    }

    @GetMapping
    public Flux<WalletResponse> getUserWallets(ServerWebExchange exchange) {
        String authToken = getAuthToken(exchange);
        return transactionServiceClient.getUserWallets(authToken);
    }

    private String getAuthToken(ServerWebExchange exchange) {
        return exchange.getRequest().getHeaders().getFirst("Authorization");
    }
}