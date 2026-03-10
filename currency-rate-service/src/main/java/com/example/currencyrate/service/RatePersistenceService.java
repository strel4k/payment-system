package com.example.currencyrate.service;

import com.example.currencyrate.entity.ConversionRate;
import com.example.currencyrate.repository.ConversionRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class RatePersistenceService {

    private final ConversionRateRepository conversionRateRepository;

    @Transactional
    public void savePair(String sourceCode,
                         String destinationCode,
                         BigDecimal rate,
                         LocalDateTime beginTime,
                         LocalDateTime endTime,
                         String providerCode) {
        conversionRateRepository.expireActiveRates(sourceCode, destinationCode, beginTime);
        conversionRateRepository.save(ConversionRate.builder()
                .sourceCode(sourceCode)
                .destinationCode(destinationCode)
                .rate(rate)
                .rateBeginTime(beginTime)
                .rateEndTime(endTime)
                .providerCode(providerCode)
                .build());
        log.debug("Saved rate {} -> {}: {}", sourceCode, destinationCode, rate);
    }
}