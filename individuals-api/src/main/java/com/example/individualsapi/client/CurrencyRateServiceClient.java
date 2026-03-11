package com.example.individualsapi.client;

import com.example.individualsapi.client.dto.currencyrate.RateResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class CurrencyRateServiceClient {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final CurrencyRateServiceFeignClient feignClient;

    public Mono<RateResponse> getRate(String fromCurrency, String toCurrency) {
        log.info("Fetching exchange rate via Feign: {} -> {}", fromCurrency, toCurrency);

        return Mono.fromCallable(() -> feignClient.getRate(fromCurrency, toCurrency))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(r -> log.info("Got rate {} -> {}: {}", fromCurrency, toCurrency, r.getRate()))
                .doOnError(e -> log.error("Failed to get rate {} -> {}: {}",
                        fromCurrency, toCurrency, e.getMessage()));
    }

    public Mono<RateResponse> getRateAt(String fromCurrency, String toCurrency, LocalDateTime timestamp) {
        log.info("Fetching historical rate via Feign: {} -> {} at {}", fromCurrency, toCurrency, timestamp);
        String ts = timestamp.format(ISO_FORMATTER);

        return Mono.fromCallable(() -> feignClient.getRateAt(fromCurrency, toCurrency, ts))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(r -> log.info("Got historical rate {} -> {}: {}", fromCurrency, toCurrency, r.getRate()))
                .doOnError(e -> log.error("Failed to get historical rate {} -> {}: {}",
                        fromCurrency, toCurrency, e.getMessage()));
    }
}