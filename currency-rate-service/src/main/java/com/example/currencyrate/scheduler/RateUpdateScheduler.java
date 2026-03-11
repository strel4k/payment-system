package com.example.currencyrate.scheduler;

import com.example.currencyrate.service.ExchangeRateService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RateUpdateScheduler {

    private final ExchangeRateService exchangeRateService;
    private final Counter successCounter;
    private final Counter failureCounter;
    private final Timer updateTimer;

    public RateUpdateScheduler(ExchangeRateService exchangeRateService, MeterRegistry meterRegistry) {
        this.exchangeRateService = exchangeRateService;
        this.successCounter = Counter.builder("rate_update_success_total")
                .description("Total successful rate update executions")
                .register(meterRegistry);
        this.failureCounter = Counter.builder("rate_update_failure_total")
                .description("Total failed rate update executions")
                .register(meterRegistry);
        this.updateTimer = Timer.builder("rate_update_duration_seconds")
                .description("Time taken to complete a rate update cycle")
                .register(meterRegistry);
    }

    @Scheduled(fixedDelayString = "${exchange-rate.update-interval-ms:3600000}")
    @SchedulerLock(
            name = "updateRates",
            lockAtMostFor = "PT10M",
            lockAtLeastFor = "PT5M"
    )
    public void updateRates() {
        log.info("Rate update job started");

        updateTimer.record(() -> {
            try {
                exchangeRateService.updateRates();
                successCounter.increment();
                log.info("Rate update job completed successfully");
            } catch (Exception ex) {
                failureCounter.increment();
                log.error("Rate update job failed: {}", ex.getMessage(), ex);
                throw ex;
            }
        });
    }
}