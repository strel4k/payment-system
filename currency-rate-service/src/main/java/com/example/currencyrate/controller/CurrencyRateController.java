package com.example.currencyrate.controller;

import com.example.currencyrate.entity.ConversionRate;
import com.example.currencyrate.entity.Currency;
import com.example.currencyrate.entity.RateProvider;
import com.example.currencyrate.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CurrencyRateController {

    private final ExchangeRateService exchangeRateService;

    @GetMapping("/rates")
    public ResponseEntity<Map<String, Object>> getRate(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime timestamp
    ) {
        log.debug("GET /rates from={} to={} timestamp={}", from, to, timestamp);

        ConversionRate rate = exchangeRateService.getRate(
                from.toUpperCase(), to.toUpperCase(), timestamp);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("fromCurrency", rate.getSourceCode());
        response.put("toCurrency", rate.getDestinationCode());
        response.put("rate", rate.getRate());
        response.put("rateBeginTime", rate.getRateBeginTime());
        response.put("rateEndTime", rate.getRateEndTime());
        response.put("providerCode", rate.getProviderCode());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/currencies")
    public ResponseEntity<List<Map<String, Object>>> getCurrencies() {
        List<Currency> currencies = exchangeRateService.getActiveCurrencies();

        List<Map<String, Object>> response = currencies.stream()
                .map(c -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("code", c.getCode());
                    m.put("isoCode", c.getIsoCode());
                    m.put("description", c.getDescription());
                    m.put("symbol", c.getSymbol());
                    m.put("active", c.isActive());
                    return m;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/rate-providers")
    public ResponseEntity<List<Map<String, Object>>> getRateProviders() {
        List<RateProvider> providers = exchangeRateService.getActiveProviders();

        List<Map<String, Object>> response = providers.stream()
                .map(p -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("providerCode", p.getProviderCode());
                    m.put("providerName", p.getProviderName());
                    m.put("priority", p.getPriority());
                    m.put("active", p.isActive());
                    return m;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}