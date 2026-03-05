package com.example.individualsapi.service;

import com.example.individualsapi.client.CurrencyRateServiceClient;
import com.example.individualsapi.client.TransactionServiceClient;
import com.example.dto.transaction.TransactionInitRequest;
import com.example.dto.transaction.TransactionInitResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {

    private final TransactionServiceClient transactionServiceClient;
    private final CurrencyRateServiceClient currencyRateServiceClient;

    public Mono<TransactionInitResponse> initTransfer(TransactionInitRequest request, String authToken) {

        // Fetch both wallets in parallel
        return Mono.zip(
                        transactionServiceClient.getWallet(request.getWalletUid(), authToken),
                        transactionServiceClient.getWallet(request.getTargetWalletUid(), authToken)
                )
                .flatMap(wallets -> {
                    String sourceCurrency = wallets.getT1().getCurrencyCode();
                    String targetCurrency = wallets.getT2().getCurrencyCode();

                    log.info("Transfer init: wallet={} ({}), targetWallet={} ({}), amount={}",
                            request.getWalletUid(), sourceCurrency,
                            request.getTargetWalletUid(), targetCurrency,
                            request.getAmount());

                    if (sourceCurrency.equalsIgnoreCase(targetCurrency)) {
                        // Same currency — no conversion needed, go straight to TransactionService
                        log.debug("Same currency ({}), skipping rate fetch", sourceCurrency);
                        return transactionServiceClient.initTransaction("transfer", request, authToken);
                    }

                    // Different currencies — fetch exchange rate first
                    log.info("Cross-currency transfer detected: {} -> {}, fetching exchange rate",
                            sourceCurrency, targetCurrency);

                    return currencyRateServiceClient.getRate(sourceCurrency, targetCurrency)
                            .flatMap(rate -> {
                                log.info("Applying exchange rate {} -> {}: {}",
                                        sourceCurrency, targetCurrency, rate.getRate());

                                TransactionInitRequest enrichedRequest = new TransactionInitRequest()
                                        .walletUid(request.getWalletUid())
                                        .targetWalletUid(request.getTargetWalletUid())
                                        .amount(request.getAmount())
                                        .paymentMethodId(request.getPaymentMethodId());
                                log.info("Exchange rate {} -> {}: {} (not passed to transaction-service, informational only)",
                                        sourceCurrency, targetCurrency, rate.getRate());

                                return transactionServiceClient.initTransaction("transfer", enrichedRequest, authToken);
                            });
                });
    }
}