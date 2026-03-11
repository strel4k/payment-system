package com.example.currencyrate.it;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.example.currencyrate.repository.ConversionRateRepository;
import com.example.currencyrate.repository.CurrencyRepository;
import com.example.currencyrate.repository.RateProviderRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
abstract class AbstractIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("currency")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.enabled", () -> "false");
    }

    @Autowired protected MockMvc mockMvc;
    @Autowired protected CurrencyRepository currencyRepository;
    @Autowired protected RateProviderRepository rateProviderRepository;
    @Autowired protected ConversionRateRepository conversionRateRepository;

    @BeforeEach
    void seedReferenceData() {
        conversionRateRepository.deleteAll();

        if (!rateProviderRepository.existsById("EXR")) {
            rateProviderRepository.save(new com.example.currencyrate.entity.RateProvider(
                    "EXR", "ExchangeRate-API", 1, true));
        }
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
}