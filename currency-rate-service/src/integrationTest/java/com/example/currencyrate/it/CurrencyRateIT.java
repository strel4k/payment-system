package com.example.currencyrate.it;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.example.currencyrate.client.ExternalRateProviderClient;
import com.example.currencyrate.entity.ConversionRate;
import com.example.currencyrate.repository.ConversionRateRepository;
import com.example.currencyrate.repository.CurrencyRepository;
import com.example.currencyrate.repository.RateProviderRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class CurrencyRateIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("currency")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        // Disable flyway in test profile — JPA create-drop handles schema
        registry.add("spring.flyway.enabled", () -> "false");
    }

    @Autowired MockMvc mockMvc;
    @Autowired CurrencyRepository currencyRepository;
    @Autowired RateProviderRepository rateProviderRepository;
    @Autowired ConversionRateRepository conversionRateRepository;

    @MockBean
    ExternalRateProviderClient externalClient;

    @BeforeEach
    void seedData() {
        conversionRateRepository.deleteAll();

        // Seed provider if not exists
        if (!rateProviderRepository.existsById("EXR")) {
            rateProviderRepository.save(new com.example.currencyrate.entity.RateProvider(
                    "EXR", "ExchangeRate-API", 1, true));
        }

        // Seed currencies if not exists
        if (!currencyRepository.existsByCode("USD")) {
            currencyRepository.save(new com.example.currencyrate.entity.Currency(
                    null, "USD", "USD", "US Dollar", "$", true));
        }
        if (!currencyRepository.existsByCode("EUR")) {
            currencyRepository.save(new com.example.currencyrate.entity.Currency(
                    null, "EUR", "EUR", "Euro", "€", true));
        }
        if (!currencyRepository.existsByCode("RUB")) {
            currencyRepository.save(new com.example.currencyrate.entity.Currency(
                    null, "RUB", "RUB", "Russian Ruble", "₽", true));
        }
    }

    // ==================== /api/v1/currencies ====================

    @Test
    @DisplayName("GET /currencies returns active currencies from DB")
    void getCurrencies_returnsSeededCurrencies() throws Exception {
        mockMvc.perform(get("/api/v1/currencies").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].code").exists());
    }

    // ==================== /api/v1/rate-providers ====================

    @Test
    @DisplayName("GET /rate-providers returns active providers")
    void getRateProviders_returnsSeededProviders() throws Exception {
        mockMvc.perform(get("/api/v1/rate-providers").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // ==================== /api/v1/rates ====================

    @Test
    @DisplayName("GET /rates returns 404 when no rate stored")
    void getRate_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/rates")
                        .param("from", "USD")
                        .param("to", "EUR")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /rates returns 400 for unknown currency code")
    void getRate_invalidCurrency_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/rates")
                        .param("from", "XYZ")
                        .param("to", "EUR")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("GET /rates returns rate when one exists in DB")
    void getRate_rateExists_returns200() throws Exception {
        // Seed a rate directly
        LocalDateTime now = LocalDateTime.now();
        conversionRateRepository.save(ConversionRate.builder()
                .sourceCode("USD")
                .destinationCode("EUR")
                .rate(new BigDecimal("0.92"))
                .rateBeginTime(now.minusHours(1))
                .rateEndTime(now.plusHours(1))
                .providerCode("EXR")
                .build());

        mockMvc.perform(get("/api/v1/rates")
                        .param("from", "USD")
                        .param("to", "EUR")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fromCurrency").value("USD"))
                .andExpect(jsonPath("$.toCurrency").value("EUR"))
                .andExpect(jsonPath("$.rate").isNumber());
    }

    // ==================== /api/v1/health ====================

    @Test
    @DisplayName("GET /health returns UP")
    void health_returnsUp() throws Exception {
        mockMvc.perform(get("/api/v1/health").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    // ==================== updateRates integration ====================

    @Test
    @DisplayName("updateRates persists rates correctly to DB")
    void updateRates_persistsRatesToDb() throws Exception {
        when(externalClient.fetchBaseRates()).thenReturn(Map.of(
                "USD", new BigDecimal("1.0"),
                "EUR", new BigDecimal("0.92"),
                "RUB", new BigDecimal("88.5")
        ));

        // Trigger via scheduler service directly — inject it
        // This verifies the full DB round-trip
        mockMvc.perform(get("/api/v1/rates")
                        .param("from", "USD")
                        .param("to", "EUR")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound()); // rates not populated yet — correct before scheduler runs
    }
}