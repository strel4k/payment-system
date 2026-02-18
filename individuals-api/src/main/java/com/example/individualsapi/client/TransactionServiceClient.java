package com.example.individualsapi.client;

import com.example.dto.transaction.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionServiceClient {

    private final WebClient transactionServiceWebClient;
    private final TransactionServiceProperties props;

    // ==================== WALLETS ====================

    public Mono<WalletResponse> createWallet(CreateWalletRequest request, String authToken) {
        String url = props.getBaseUrl() + "/api/v1/wallets";
        log.info("Creating wallet: {}", request.getName());

        return transactionServiceWebClient.post()
                .uri(url)
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(WalletResponse.class)
                .doOnSuccess(r -> log.info("Wallet created: {}", r.getUid()))
                .doOnError(e -> log.error("Failed to create wallet", e));
    }

    public Mono<WalletResponse> getWallet(UUID walletUid, String authToken) {
        String url = props.getBaseUrl() + "/api/v1/wallets/" + walletUid;
        log.debug("Getting wallet: {}", walletUid);

        return transactionServiceWebClient.get()
                .uri(url)
                .header("Authorization", authToken)
                .retrieve()
                .bodyToMono(WalletResponse.class);
    }

    public Flux<WalletResponse> getUserWallets(String authToken) {
        String url = props.getBaseUrl() + "/api/v1/wallets";
        log.debug("Getting user wallets");

        return transactionServiceWebClient.get()
                .uri(url)
                .header("Authorization", authToken)
                .retrieve()
                .bodyToFlux(WalletResponse.class);
    }

    // ==================== TRANSACTIONS ====================

    public Mono<TransactionInitResponse> initTransaction(String type, TransactionInitRequest request, String authToken) {
        String url = props.getBaseUrl() + "/api/v1/transactions/" + type + "/init";
        log.info("Init {} transaction for wallet: {}", type, request.getWalletUid());

        return transactionServiceWebClient.post()
                .uri(url)
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(TransactionInitResponse.class)
                .doOnSuccess(r -> log.info("Transaction init successful, requestUid: {}", r.getRequestUid()))
                .doOnError(e -> log.error("Failed to init {} transaction", type, e));
    }

    public Mono<TransactionConfirmResponse> confirmTransaction(String type, TransactionConfirmRequest request, String authToken) {
        String url = props.getBaseUrl() + "/api/v1/transactions/" + type + "/confirm";
        log.info("Confirm {} transaction, requestUid: {}", type, request.getRequestUid());

        return transactionServiceWebClient.post()
                .uri(url)
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(TransactionConfirmResponse.class)
                .doOnSuccess(r -> log.info("Transaction confirmed: {}", r.getTransactionUid()))
                .doOnError(e -> log.error("Failed to confirm {} transaction", type, e));
    }

    public Mono<TransactionStatusResponse> getTransactionStatus(UUID transactionUid, String authToken) {
        String url = props.getBaseUrl() + "/api/v1/transactions/" + transactionUid + "/status";
        log.debug("Getting transaction status: {}", transactionUid);

        return transactionServiceWebClient.get()
                .uri(url)
                .header("Authorization", authToken)
                .retrieve()
                .bodyToMono(TransactionStatusResponse.class);
    }
}