package com.example.currencyrate.it;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.example.currencyrate.entity.ConversionRate;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CurrencyRateIT extends AbstractIntegrationTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void configureWireMock(DynamicPropertyRegistry registry) {
        registry.add("exchange-rate.base-url", wireMock::baseUrl);
        registry.add("exchange-rate.api-key", () -> "test-key");
    }

    @BeforeEach
    void stubExternalRateApi() {
        wireMock.stubFor(WireMock.get(urlPathMatching("/v6/.*/latest/.*"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "result": "success",
                                    "base_code": "USD",
                                    "conversion_rates": {
                                        "USD": 1.0,
                                        "EUR": 0.92,
                                        "RUB": 88.5
                                    }
                                }
                                """)));
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
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].providerCode").value("EXR"));
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

    @Test
    @DisplayName("GET /rates identity rate returns 1.0 for same currency")
    void getRate_sameCurrency_returnsIdentity() throws Exception {
        mockMvc.perform(get("/api/v1/rates")
                        .param("from", "USD")
                        .param("to", "USD")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rate").value(1));
    }

    // ==================== /api/v1/health ====================

    @Test
    @DisplayName("GET /health returns UP")
    void health_returnsUp() throws Exception {
        mockMvc.perform(get("/api/v1/health").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}