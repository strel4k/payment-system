package com.example.currencyrate.service;

import com.example.currencyrate.client.ExternalRateProviderClient;
import com.example.currencyrate.config.ExchangeRateProperties;
import com.example.currencyrate.entity.ConversionRate;
import com.example.currencyrate.entity.Currency;
import com.example.currencyrate.entity.RateCorrectionFactor;
import com.example.currencyrate.entity.RateProvider;
import com.example.currencyrate.exception.InvalidCurrencyException;
import com.example.currencyrate.exception.RateNotFoundException;
import com.example.currencyrate.repository.ConversionRateRepository;
import com.example.currencyrate.repository.CurrencyRepository;
import com.example.currencyrate.repository.RateCorrectionFactorRepository;
import com.example.currencyrate.repository.RateProviderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private static final MathContext MC = new MathContext(18, RoundingMode.HALF_UP);
    private static final int RATE_TTL_HOURS = 2;

    private final ExternalRateProviderClient externalClient;
    private final CurrencyRepository currencyRepository;
    private final RateProviderRepository rateProviderRepository;
    private final ConversionRateRepository conversionRateRepository;
    private final RateCorrectionFactorRepository correctionFactorRepository;
    private final ExchangeRateProperties props;

    // ==================== Public API ====================

    @Transactional(readOnly = true)
    public ConversionRate getRate(String sourceCode, String destinationCode, LocalDateTime timestamp) {
        log.debug("Getting rate: {} -> {} at {}", sourceCode, destinationCode, timestamp);

        validateCurrencyCode(sourceCode);
        validateCurrencyCode(destinationCode);

        if (sourceCode.equalsIgnoreCase(destinationCode)) {
            return identityRate(sourceCode, timestamp != null ? timestamp : LocalDateTime.now());
        }

        if (timestamp != null) {
            return conversionRateRepository
                    .findRateAtTimestamp(sourceCode, destinationCode, timestamp)
                    .orElseThrow(() -> new RateNotFoundException(sourceCode, destinationCode, timestamp.toString()));
        }

        return conversionRateRepository
                .findLatestRate(sourceCode, destinationCode)
                .orElseThrow(() -> new RateNotFoundException(sourceCode, destinationCode));
    }

    @Transactional
    public void updateRates() {
        log.info("Starting rate update...");

        List<Currency> currencies = currencyRepository.findAllByActiveTrue();
        if (currencies.isEmpty()) {
            log.warn("No active currencies found, skipping update");
            return;
        }

        RateProvider provider = rateProviderRepository
                .findAllByActiveTrueOrderByPriorityAsc()
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No active rate provider configured"));

        Map<String, BigDecimal> baseRates = externalClient.fetchBaseRates();
        if (baseRates.isEmpty()) {
            throw new IllegalStateException("External API returned no rates");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endTime = now.plusHours(RATE_TTL_HOURS);
        int savedCount = 0;

        for (Currency source : currencies) {
            for (Currency destination : currencies) {
                if (source.getCode().equals(destination.getCode())) continue;

                BigDecimal usdToSource = baseRates.get(source.getCode());
                BigDecimal usdToDestination = baseRates.get(destination.getCode());

                if (usdToSource == null || usdToDestination == null) {
                    log.warn("Missing base rate for pair {} -> {}, skipping",
                            source.getCode(), destination.getCode());
                    continue;
                }

                // cross-rate: rate(A->B) = USD->B / USD->A
                BigDecimal crossRate = usdToDestination.divide(usdToSource, MC);

                // Apply correction factor if configured for this pair
                BigDecimal finalRate = applyCorrectionFactor(
                        source.getCode(), destination.getCode(), crossRate);

                // Expire current active rate
                conversionRateRepository.expireActiveRates(
                        source.getCode(), destination.getCode(), now);

                // Save new rate with correction applied
                conversionRateRepository.save(ConversionRate.builder()
                        .sourceCode(source.getCode())
                        .destinationCode(destination.getCode())
                        .rate(finalRate)
                        .rateBeginTime(now)
                        .rateEndTime(endTime)
                        .providerCode(provider.getProviderCode())
                        .build());

                savedCount++;
            }
        }

        log.info("Rate update complete: {} pairs saved, provider={}", savedCount, provider.getProviderCode());
    }

    @Transactional(readOnly = true)
    public List<Currency> getActiveCurrencies() {
        return currencyRepository.findAllByActiveTrue();
    }

    @Transactional(readOnly = true)
    public List<RateProvider> getActiveProviders() {
        return rateProviderRepository.findAllByActiveTrueOrderByPriorityAsc();
    }

    // ==================== Internals ====================

    private BigDecimal applyCorrectionFactor(String sourceCode, String destinationCode,
                                             BigDecimal rawRate) {
        return correctionFactorRepository
                .findBySourceCodeAndDestinationCodeAndActiveTrue(sourceCode, destinationCode)
                .map(RateCorrectionFactor::getFactor)
                .map(factor -> {
                    BigDecimal corrected = rawRate.multiply(factor, MC);
                    log.debug("Applied correction factor {} for pair {} -> {}: {} -> {}",
                            factor, sourceCode, destinationCode, rawRate, corrected);
                    return corrected;
                })
                .orElse(rawRate);
    }

    private void validateCurrencyCode(String code) {
        if (!currencyRepository.existsByCode(code.toUpperCase())) {
            throw new InvalidCurrencyException(code);
        }
    }

    private ConversionRate identityRate(String code, LocalDateTime at) {
        return ConversionRate.builder()
                .sourceCode(code)
                .destinationCode(code)
                .rate(BigDecimal.ONE)
                .rateBeginTime(at)
                .rateEndTime(at.plusHours(RATE_TTL_HOURS))
                .providerCode("IDENTITY")
                .build();
    }
}