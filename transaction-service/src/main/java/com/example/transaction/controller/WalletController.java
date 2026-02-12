package com.example.transaction.controller;

import com.example.transaction.api.WalletsApi;
import com.example.transaction.dto.CreateWalletRequest;
import com.example.transaction.dto.WalletResponse;
import com.example.transaction.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;


@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class WalletController implements WalletsApi {

    private final WalletService walletService;

    @Override
    public ResponseEntity<WalletResponse> createWallet(CreateWalletRequest createWalletRequest) {
        log.info("POST /wallets - Creating wallet for user: {}", createWalletRequest.getUserUid());

        WalletResponse response = walletService.createWallet(createWalletRequest);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    public ResponseEntity<WalletResponse> getWallet(UUID walletUid) {
        log.info("GET /wallets/{} - Getting wallet", walletUid);

        WalletResponse response = walletService.getWallet(walletUid);

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<List<WalletResponse>> getWalletsByUser(UUID userUid) {
        log.info("GET /wallets/user/{} - Getting wallets for user", userUid);

        List<WalletResponse> response = walletService.getWalletsByUser(userUid);

        return ResponseEntity.ok(response);
    }
}