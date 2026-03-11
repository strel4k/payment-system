package com.example.individualsapi.controller;

import com.example.dto.transaction.TransactionConfirmRequest;
import com.example.dto.transaction.TransactionConfirmResponse;
import com.example.dto.transaction.TransactionInitRequest;
import com.example.dto.transaction.TransactionInitResponse;
import com.example.dto.transaction.TransactionStatusResponse;
import com.example.individualsapi.client.TransactionServiceClient;
import com.example.individualsapi.service.TransferService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    @Mock
    private TransactionServiceClient transactionServiceClient;

    @Mock
    private TransferService transferService;

    @InjectMocks
    private TransactionController transactionController;

    private static final UUID WALLET_UID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID REQUEST_UID = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");
    private static final UUID TX_UID = UUID.fromString("cccccccc-0000-0000-0000-000000000003");

    private MockServerWebExchange exchangeWithToken() {
        return MockServerWebExchange.from(
                MockServerHttpRequest.get("/")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-token")
                        .build()
        );
    }

    private TransactionInitResponse initStub() {
        TransactionInitResponse r = new TransactionInitResponse();
        r.setRequestUid(REQUEST_UID);
        r.setAmount(BigDecimal.valueOf(100));
        r.setFee(BigDecimal.ZERO);
        r.setTotalAmount(BigDecimal.valueOf(100));
        return r;
    }

    private TransactionConfirmResponse confirmStub() {
        TransactionConfirmResponse r = new TransactionConfirmResponse();
        r.setTransactionUid(TX_UID);
        r.setStatus(TransactionConfirmResponse.StatusEnum.PENDING);
        r.setType(TransactionConfirmResponse.TypeEnum.DEPOSIT);
        r.setAmount(BigDecimal.valueOf(100));
        r.setFee(BigDecimal.ZERO);
        return r;
    }

    private TransactionStatusResponse statusStub() {
        TransactionStatusResponse r = new TransactionStatusResponse();
        r.setUid(TX_UID);
        r.setStatus(TransactionStatusResponse.StatusEnum.COMPLETED);
        r.setType(TransactionStatusResponse.TypeEnum.DEPOSIT);
        r.setAmount(BigDecimal.valueOf(100));
        return r;
    }

    // ── initTransaction ──────

    @Test
    @DisplayName("initTransaction deposit → delegates to transactionServiceClient")
    void initTransaction_deposit_delegatesToClient() {
        when(transactionServiceClient.initTransaction(eq("deposit"), any(), anyString()))
                .thenReturn(Mono.just(initStub()));

        TransactionInitRequest req = new TransactionInitRequest()
                .walletUid(WALLET_UID)
                .amount(BigDecimal.valueOf(100));

        StepVerifier.create(transactionController.initTransaction("deposit", req, exchangeWithToken()))
                .expectNextMatches(r -> r.getRequestUid().equals(REQUEST_UID))
                .verifyComplete();

        verify(transactionServiceClient).initTransaction(eq("deposit"), eq(req), anyString());
        verify(transferService, never()).initTransfer(any(), any());
    }

    @Test
    @DisplayName("initTransaction transfer → delegates to transferService")
    void initTransaction_transfer_delegatesToTransferService() {
        when(transferService.initTransfer(any(), anyString()))
                .thenReturn(Mono.just(initStub()));

        TransactionInitRequest req = new TransactionInitRequest()
                .walletUid(WALLET_UID)
                .targetWalletUid(UUID.randomUUID())
                .amount(BigDecimal.valueOf(50));

        StepVerifier.create(transactionController.initTransaction("transfer", req, exchangeWithToken()))
                .expectNextMatches(r -> r.getRequestUid().equals(REQUEST_UID))
                .verifyComplete();

        verify(transferService).initTransfer(eq(req), anyString());
        verify(transactionServiceClient, never()).initTransaction(any(), any(), any());
    }

    @Test
    @DisplayName("initTransaction TRANSFER (uppercase) → delegates to transferService")
    void initTransaction_transferUppercase_delegatesToTransferService() {
        when(transferService.initTransfer(any(), anyString()))
                .thenReturn(Mono.just(initStub()));

        TransactionInitRequest req = new TransactionInitRequest()
                .walletUid(WALLET_UID)
                .targetWalletUid(UUID.randomUUID())
                .amount(BigDecimal.valueOf(50));

        StepVerifier.create(transactionController.initTransaction("TRANSFER", req, exchangeWithToken()))
                .expectNextMatches(r -> r.getRequestUid() != null)
                .verifyComplete();

        verify(transferService).initTransfer(any(), anyString());
    }

    // ── confirmTransaction ──────

    @Test
    @DisplayName("confirmTransaction → delegates to transactionServiceClient")
    void confirmTransaction_delegatesToClient() {
        when(transactionServiceClient.confirmTransaction(eq("deposit"), any(), anyString()))
                .thenReturn(Mono.just(confirmStub()));

        TransactionConfirmRequest req = new TransactionConfirmRequest()
                .requestUid(REQUEST_UID)
                .walletUid(WALLET_UID)
                .amount(BigDecimal.valueOf(100));

        StepVerifier.create(transactionController.confirmTransaction("deposit", req, exchangeWithToken()))
                .expectNextMatches(r -> r.getTransactionUid().equals(TX_UID)
                        && r.getStatus() == TransactionConfirmResponse.StatusEnum.PENDING)
                .verifyComplete();

        verify(transactionServiceClient).confirmTransaction(eq("deposit"), eq(req), anyString());
    }

    // ── getTransactionStatus ──────

    @Test
    @DisplayName("getTransactionStatus → delegates to transactionServiceClient")
    void getTransactionStatus_delegatesToClient() {
        when(transactionServiceClient.getTransactionStatus(eq(TX_UID), anyString()))
                .thenReturn(Mono.just(statusStub()));

        StepVerifier.create(transactionController.getTransactionStatus(TX_UID, exchangeWithToken()))
                .expectNextMatches(r -> r.getUid().equals(TX_UID)
                        && r.getStatus() == TransactionStatusResponse.StatusEnum.COMPLETED)
                .verifyComplete();

        verify(transactionServiceClient).getTransactionStatus(eq(TX_UID), anyString());
    }
}