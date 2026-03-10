package com.example.currencyrate.controller;

import com.example.currencyrate.controller.dto.CurrencyRateResponse;
import com.example.currencyrate.controller.dto.CurrencyResponse;
import com.example.currencyrate.controller.dto.RateProviderResponse;
import com.example.currencyrate.controller.mapper.CurrencyRateMapper;
import com.example.currencyrate.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CurrencyRateController {

    private final ExchangeRateService exchangeRateService;
    private final CurrencyRateMapper mapper;

    @GetMapping("/rates")
    public ResponseEntity<CurrencyRateResponse> getRate(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime timestamp
    ) {
        log.debug("GET /rates from={} to={} timestamp={}", from, to, timestamp);
        return ResponseEntity.ok(
                mapper.toResponse(
                        exchangeRateService.getRate(from.toUpperCase(), to.toUpperCase(), timestamp)
                )
        );
    }

    @GetMapping("/currencies")
    public ResponseEntity<List<CurrencyResponse>> getCurrencies() {
        return ResponseEntity.ok(
                exchangeRateService.getActiveCurrencies().stream()
                        .map(mapper::toResponse)
                        .toList()
        );
    }

    @GetMapping("/rate-providers")
    public ResponseEntity<List<RateProviderResponse>> getRateProviders() {
        return ResponseEntity.ok(
                exchangeRateService.getActiveProviders().stream()
                        .map(mapper::toResponse)
                        .toList()
        );
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}