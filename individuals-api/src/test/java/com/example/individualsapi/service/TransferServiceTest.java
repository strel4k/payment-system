package com.example.individualsapi.service;

import com.example.dto.transaction.TransactionInitRequest;
import com.example.dto.transaction.TransactionInitResponse;
import com.example.dto.transaction.WalletResponse;
import com.example.individualsapi.client.CurrencyRateServiceClient;
import com.example.individualsapi.client.TransactionServiceClient;
import com.example.individualsapi.client.dto.currencyrate.RateResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private TransactionServiceClient transactionServiceClient;

    @Mock
    private CurrencyRateServiceClient currencyRateServiceClient;

    @InjectMocks
    private TransferService transferService;

    private static final UUID WALLET_UID  = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID TARGET_UID  = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000002");
    private static final String AUTH      = "Bearer test-token";

    private WalletResponse walletStub(String currency) {
        WalletResponse w = new WalletResponse();
        w.setUid(WALLET_UID);
        w.setCurrencyCode(currency);
        w.setStatus(WalletResponse.StatusEnum.ACTIVE);
        w.setBalance(BigDecimal.valueOf(1000));
        return w;
    }

    private WalletResponse targetWalletStub(String currency) {
        WalletResponse w = new WalletResponse();
        w.setUid(TARGET_UID);
        w.setCurrencyCode(currency);
        w.setStatus(WalletResponse.StatusEnum.ACTIVE);
        w.setBalance(BigDecimal.ZERO);
        return w;
    }

    private TransactionInitRequest requestStub() {
        return new TransactionInitRequest()
                .walletUid(WALLET_UID)
                .targetWalletUid(TARGET_UID)
                .amount(BigDecimal.valueOf(50));
    }

    private TransactionInitResponse initResponseStub() {
        TransactionInitResponse r = new TransactionInitResponse();
        r.setRequestUid(UUID.randomUUID());
        r.setAmount(BigDecimal.valueOf(50));
        r.setFee(BigDecimal.ZERO);
        r.setTotalAmount(BigDecimal.valueOf(50));
        return r;
    }

    private RateResponse rateStub(String from, String to, double rate) {
        RateResponse r = new RateResponse();
        r.setFromCurrency(from);
        r.setToCurrency(to);
        r.setRate(BigDecimal.valueOf(rate));
        return r;
    }

    // ── same currency ───────

    @Test
    @DisplayName("Same currency → no rate fetch, delegates to transactionServiceClient")
    void initTransfer_sameCurrency_skipsCurrencyRateFetch() {
        when(transactionServiceClient.getWallet(eq(WALLET_UID), anyString()))
                .thenReturn(Mono.just(walletStub("USD")));
        when(transactionServiceClient.getWallet(eq(TARGET_UID), anyString()))
                .thenReturn(Mono.just(targetWalletStub("USD")));
        when(transactionServiceClient.initTransaction(eq("transfer"), any(), anyString()))
                .thenReturn(Mono.just(initResponseStub()));

        StepVerifier.create(transferService.initTransfer(requestStub(), AUTH))
                .expectNextMatches(r -> r.getAmount().compareTo(BigDecimal.valueOf(50)) == 0)
                .verifyComplete();

        verify(currencyRateServiceClient, never()).getRate(anyString(), anyString());
        verify(transactionServiceClient).initTransaction(eq("transfer"), any(), eq(AUTH));
    }

    // ── cross-currency ──────

    @Test
    @DisplayName("Cross-currency USD→EUR → fetches rate and delegates to transactionServiceClient")
    void initTransfer_crossCurrency_fetchesRateAndDelegates() {
        when(transactionServiceClient.getWallet(eq(WALLET_UID), anyString()))
                .thenReturn(Mono.just(walletStub("USD")));
        when(transactionServiceClient.getWallet(eq(TARGET_UID), anyString()))
                .thenReturn(Mono.just(targetWalletStub("EUR")));
        when(currencyRateServiceClient.getRate(eq("USD"), eq("EUR")))
                .thenReturn(Mono.just(rateStub("USD", "EUR", 0.92)));
        when(transactionServiceClient.initTransaction(eq("transfer"), any(), anyString()))
                .thenReturn(Mono.just(initResponseStub()));

        StepVerifier.create(transferService.initTransfer(requestStub(), AUTH))
                .expectNextMatches(r -> r.getAmount() != null)
                .verifyComplete();

        verify(currencyRateServiceClient).getRate("USD", "EUR");
        verify(transactionServiceClient).initTransaction(eq("transfer"), any(), eq(AUTH));
    }

    // ── source wallet not found ──────

    @Test
    @DisplayName("Source wallet fetch fails → error propagates")
    void initTransfer_sourceWalletNotFound_propagatesError() {
        when(transactionServiceClient.getWallet(eq(WALLET_UID), anyString()))
                .thenReturn(Mono.error(new RuntimeException("wallet not found")));
        when(transactionServiceClient.getWallet(eq(TARGET_UID), anyString()))
                .thenReturn(Mono.just(targetWalletStub("EUR")));

        StepVerifier.create(transferService.initTransfer(requestStub(), AUTH))
                .expectErrorMatches(e -> e.getMessage().contains("wallet not found"))
                .verify();

        verify(transactionServiceClient, never()).initTransaction(any(), any(), any());
    }

    // ── rate service unavailable ───────

    @Test
    @DisplayName("Currency rate service fails → error propagates")
    void initTransfer_rateServiceFails_propagatesError() {
        when(transactionServiceClient.getWallet(eq(WALLET_UID), anyString()))
                .thenReturn(Mono.just(walletStub("USD")));
        when(transactionServiceClient.getWallet(eq(TARGET_UID), anyString()))
                .thenReturn(Mono.just(targetWalletStub("EUR")));
        when(currencyRateServiceClient.getRate(eq("USD"), eq("EUR")))
                .thenReturn(Mono.error(new RuntimeException("rate service unavailable")));

        StepVerifier.create(transferService.initTransfer(requestStub(), AUTH))
                .expectErrorMatches(e -> e.getMessage().contains("rate service unavailable"))
                .verify();

        verify(transactionServiceClient, never()).initTransaction(any(), any(), any());
    }
}