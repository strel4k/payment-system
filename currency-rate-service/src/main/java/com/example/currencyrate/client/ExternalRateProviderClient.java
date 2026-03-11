package com.example.currencyrate.client;

import com.example.currencyrate.config.ExchangeRateProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExternalRateProviderClient {

    private final WebClient exchangeRateWebClient;
    private final ExchangeRateProperties props;

    public Map<String, BigDecimal> fetchBaseRates() {
        String baseCurrency = props.getBaseCurrency();
        log.info("Fetching base rates from external API, base={}", baseCurrency);

        try {
            ExchangeRateApiResponse response = exchangeRateWebClient.get()
                    .uri("/v6/{apiKey}/latest/{base}", props.getApiKey(), baseCurrency)
                    .retrieve()
                    .bodyToMono(ExchangeRateApiResponse.class)
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                            .filter(ex -> !(ex instanceof WebClientResponseException.Unauthorized))
                            .doBeforeRetry(rs -> log.warn("Retrying external rate fetch, attempt {}",
                                    rs.totalRetries() + 1)))
                    .timeout(Duration.ofSeconds(30))
                    .onErrorResume(ex -> {
                        log.error("Failed to fetch rates from external API after retries: {}", ex.getMessage());
                        return Mono.empty();
                    })
                    .block();

            if (response == null || response.getConversionRates() == null) {
                log.warn("External API returned empty response");
                return Collections.emptyMap();
            }

            log.info("Fetched {} rates from external API", response.getConversionRates().size());
            return response.getConversionRates();

        } catch (Exception ex) {
            log.error("Unexpected error fetching rates from external API", ex);
            return Collections.emptyMap();
        }
    }
}